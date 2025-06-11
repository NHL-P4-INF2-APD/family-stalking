package com.familystalking.app.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.familystalking.app.domain.repository.FamilyRepository
import com.familystalking.app.domain.repository.PendingRequest
import com.familystalking.app.presentation.family.FamilyMember
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@Serializable
internal data class UserName(
    val name: String
)

@Serializable
internal data class FriendshipRequest(
    val id: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    val status: String,
    val timestamp: String
)

@Serializable
internal data class Friend(
    @SerialName("user_id") val userId: String,
    @SerialName("friend_id") val friendId: String
)

// Data class for Supabase mapping (pas aan naar jouw kolommen)
@Serializable
data class FamilyMemberSupabase(
    @SerialName("user_id") val userId: String,
    val name: String,
    val status: String? = null
)

class FamilyRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : FamilyRepository {

    override suspend fun getFamilyMembers(): List<FamilyMember> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: return emptyList()

            // Step 1: Get the IDs of all friends from the 'friends' table
            val friendIds = supabaseClient.from("friends").select {
                filter { eq("user_id", currentUserId) }
            }.decodeList<Friend>().map { it.friendId }


            if (friendIds.isEmpty()) return emptyList()

            // Step 2: Get user details for the retrieved friend IDs from the 'users' table
            val friends = supabaseClient.from("users").select {
                filter { isIn("user_id", friendIds) }
            }.decodeList<FamilyMemberSupabase>()

            friends.map { FamilyMember(it.name, it.status ?: "", it.userId) }
        } catch (e: Exception) {
            Log.e("FamilyRepository", "getFamilyMembers failed", e)
            emptyList()
        }
    }

    override suspend fun getCurrentUser(): FamilyMember {
        return try {
            val authUser = supabaseClient.auth.currentUserOrNull()
            val userId = authUser?.id
            val userEmail = authUser?.email
            val nameFromEmail = userEmail?.substringBefore('@')?.replaceFirstChar { it.uppercase() } ?: "Unknown"

            if (userId == null) {
                return FamilyMember(id = null, name = nameFromEmail, status = "")
            }
            
            val response = supabaseClient.from("users").select { filter { eq("user_id", userId) } }.decodeList<FamilyMemberSupabase>()
            val user = response.firstOrNull()

            val finalName = if (user != null && user.name.isNotBlank()) user.name else nameFromEmail
            val finalStatus = user?.status ?: ""

            FamilyMember(id = userId, name = finalName, status = finalStatus)
        } catch (e: Exception) {
            e.printStackTrace()
            val userEmail = supabaseClient.auth.currentUserOrNull()?.email
            val nameFromEmail = userEmail?.substringBefore('@')?.replaceFirstChar { it.uppercase() } ?: "Unknown"
            FamilyMember(id = null, name = nameFromEmail, status = "")
        }
    }

    override suspend fun getCurrentUserId(): String? {
        return supabaseClient.auth.currentUserOrNull()?.id
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun sendFriendshipRequest(receiverId: String): Result<Unit> {
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
            ?: return Result.failure(Exception("SENDER: User not authenticated"))
        Log.d("FriendRequestFlow", "SENDER: Attempting to send request from $currentUserId to $receiverId")

        return try {
            if (currentUserId == receiverId) {
                return Result.failure(Exception("You cannot add yourself as a friend."))
            }

            val existingRequests = supabaseClient.from("friendship_requests").select {
                filter {
                    or {
                        and {
                            eq("sender_id", currentUserId)
                            eq("receiver_id", receiverId)
                        }
                        and {
                            eq("sender_id", receiverId)
                            eq("receiver_id", currentUserId)
                        }
                    }
                }
            }.decodeList<FriendshipRequest>()
            Log.d("FriendRequestFlow", "SENDER: Found ${existingRequests.size} existing records between users.")

            if (existingRequests.any { it.status == "accepted" }) {
                Log.w("FriendRequestFlow", "SENDER: Blocked. Users are already friends.")
                return Result.failure(Exception("You are already friends with this user."))
            }

            if (existingRequests.any { it.status == "pending" }) {
                Log.i("FriendRequestFlow", "SENDER: Silent success. Request already pending.")
                return Result.success(Unit)
            }

            // Insert the new friend request
            supabaseClient.from("friendship_requests").insert(
                FriendshipRequest(
                    id = UUID.randomUUID().toString(),
                    senderId = currentUserId,
                    receiverId = receiverId,
                    status = "pending",
                    timestamp = Instant.now().toString()
                )
            )
            Log.d("FriendRequestFlow", "SENDER: Successfully inserted new friend request into database.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FriendRequestFlow", "SENDER: sendFriendshipRequest failed with exception.", e)
            Result.failure(e)
        }
    }


    override suspend fun acceptFriendshipRequest(request: PendingRequest): Result<Unit> {
        Log.d("FriendRequestFlow", "RECEIVER: Attempting to accept request ID: ${request.id}")
        return try {
            val fullRequest = supabaseClient.from("friendship_requests").select {
                filter { eq("id", request.id) }
            }.decodeSingle<FriendshipRequest>()

            // Update request status to 'accepted'
            supabaseClient.from("friendship_requests")
                .update({ set("status", "accepted") }) { filter { eq("id", request.id) } }
            
            Log.d("FriendRequestFlow", "RECEIVER: Request status updated to 'accepted'.")

            // Create the two-way friendship
            val friendship1 = Friend(userId = fullRequest.senderId, friendId = fullRequest.receiverId)
            val friendship2 = Friend(userId = fullRequest.receiverId, friendId = fullRequest.senderId)
            
            supabaseClient.from("friends").insert(friendship1)
            supabaseClient.from("friends").insert(friendship2)

            Log.i("FriendRequestFlow", "RECEIVER: Friendship records created successfully.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FriendRequestFlow", "RECEIVER: acceptFriendshipRequest failed.", e)
            Result.failure(e)
        }
    }

    override suspend fun declineFriendshipRequest(request: PendingRequest): Result<Unit> {
        return try {
            supabaseClient.from("friendship_requests")
                .delete { filter { eq("id", request.id) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPendingRequests(userId: String): List<PendingRequest> {
        try {
            Log.d("FriendRequestFlow", "RECEIVER: Manually fetching pending requests for $userId...")
            val requestsWithoutNames = supabaseClient.from("friendship_requests")
                .select {
                    filter {
                        eq("receiver_id", userId)
                        eq("status", "pending")
                    }
                }.decodeList<FriendshipRequest>()

            val requestsWithNames = requestsWithoutNames.mapNotNull { request ->
                val senderName = try {
                    supabaseClient.from("users")
                        .select { filter { eq("user_id", request.senderId) } }
                        .decodeSingleOrNull<FamilyMemberSupabase>()?.name
                } catch (e: Exception) {
                    Log.e("FamilyRepo", "Could not fetch sender name for ${request.senderId}", e)
                    null
                }

                if (senderName == null) {
                    Log.w("FamilyRepo", "Skipping request from non-existent user: ${request.senderId}")
                    null
                } else {
                    PendingRequest(
                        id = request.id,
                        senderId = request.senderId,
                        senderName = senderName,
                        timestamp = request.timestamp
                    )
                }
            }
            Log.d("FriendRequestFlow", "RECEIVER: Found ${requestsWithNames.size} pending requests. Returning to ViewModel.")
            return requestsWithNames
        } catch (e: Exception) {
            Log.e("FriendRequestFlow", "RECEIVER: Error manually fetching pending requests.", e)
            return emptyList()
        }
    }

    override suspend fun searchUsers(query: String): List<FamilyMember> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: return emptyList()
            supabaseClient.from("users")
                .select {
                    filter {
                        ilike("name", "%$query%")
                        neq("user_id", currentUserId)
                    }
                }
                .decodeList<FamilyMemberSupabase>()
                .map { FamilyMember(it.name, it.status ?: "", it.userId) }
        } catch (e: Exception) {
            Log.e("FamilyRepository", "searchUsers failed", e)
            emptyList()
        }
    }
}