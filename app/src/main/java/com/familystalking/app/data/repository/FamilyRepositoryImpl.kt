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
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
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
    val timestamp: String,
    val users: UserName? = null
)

@Serializable
internal data class Friend(
    @SerialName("user_id") val userId: String,
    @SerialName("friend_id") val friendId: String
)

internal fun FriendshipRequest.toDomain(): PendingRequest {
    return PendingRequest(
        id = this.id,
        senderId = this.senderId,
        senderName = this.users?.name ?: "Unknown User",
        timestamp = this.timestamp
    )
}

class FamilyRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : FamilyRepository {

    override suspend fun getFamilyMembers(): List<FamilyMember> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: return emptyList()

            // Step 1: Get the IDs of all friends from the 'friends' table
            val friendIds = supabaseClient.from("friends").select(Columns.list("friend_id")) {
                filter { eq("user_id", currentUserId) }
            }.decodeList<Map<String, String>>()
            .mapNotNull { it["friend_id"] }

            if (friendIds.isEmpty()) return emptyList()

            // Step 2: Get user details for the retrieved friend IDs from the 'users' table
            val friends = supabaseClient.from("users").select {
                filter { isIn("user_id", friendIds) }
            }.decodeList<FamilyMemberSupabase>()

            friends.map { FamilyMember(it.name, it.status ?: "") }
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
            // Fallback name from email
            val nameFromEmail = userEmail?.substringBefore('@')?.replaceFirstChar { it.uppercase() } ?: "Unknown"

            if (userId == null) {
                return FamilyMember(nameFromEmail, "")
            }
            
            val response = supabaseClient.from("users").select { filter { eq("user_id", userId) } }.decodeList<FamilyMemberSupabase>()
            val user = response.firstOrNull()

            // Prefer database name, but use email fallback if not available
            val finalName = if (user != null && user.name.isNotBlank()) user.name else nameFromEmail
            val finalStatus = user?.status ?: ""

            FamilyMember(finalName, finalStatus)
        } catch (e: Exception) {
            e.printStackTrace()
            // Ensure fallback works even on error
            val userEmail = supabaseClient.auth.currentUserOrNull()?.email
            val nameFromEmail = userEmail?.substringBefore('@')?.replaceFirstChar { it.uppercase() } ?: "Unknown"
            FamilyMember(nameFromEmail, "")
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
                    timestamp = Instant.now().toString(),
                    users = null
                )
            )
            Log.d("FriendRequestFlow", "SENDER: Successfully inserted new friend request into database.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FriendRequestFlow", "SENDER: sendFriendshipRequest failed with exception.", e)
            Result.failure(e)
        }
    }
    
    override suspend fun acceptFriendshipRequest(requestId: String): Result<Unit> {
        return try {
            val request = supabaseClient.from("friendship_requests").select {
                filter { eq("id", requestId) }
            }.decodeSingle<FriendshipRequest>()

            // Update request status to 'accepted'
            supabaseClient.from("friendship_requests")
                .update({ set("status", "accepted") }) {
                    filter { eq("id", requestId) }
                }
            
            // Create the two-way friendship in the 'friends' table
            val friendship1 = Friend(userId = request.senderId, friendId = request.receiverId)
            val friendship2 = Friend(userId = request.receiverId, friendId = request.senderId)
            
            supabaseClient.from("friends").insert(listOf(friendship1, friendship2))

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FamilyRepository", "acceptFriendshipRequest failed", e)
            Result.failure(e)
        }
    }

    override suspend fun rejectFriendshipRequest(requestId: String): Result<Unit> {
        return try {
            supabaseClient.from("friendship_requests")
                .delete { filter { eq("id", requestId) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPendingFriendshipRequests(): Flow<List<PendingRequest>> = callbackFlow {
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
        if (currentUserId == null) {
            Log.w("FriendRequestFlow", "RECEIVER: Cannot listen for requests, user is null.")
            trySend(emptyList()); close(); return@callbackFlow
        }
        
        Log.d("FriendRequestFlow", "RECEIVER: Starting to listen for friend requests for user $currentUserId.")

        suspend fun fetchAndSend() {
            try {
                Log.d("FriendRequestFlow", "RECEIVER: Fetching pending requests...")
                val requests = supabaseClient.from("friendship_requests")
                    .select(
                        columns = Columns.list("*, users:sender_id(name)")
                    ) {
                        filter {
                            eq("receiver_id", currentUserId)
                            eq("status", "pending")
                        }
                    }.decodeList<FriendshipRequest>()
                    .map { it.toDomain() }
                Log.d("FriendRequestFlow", "RECEIVER: Found ${requests.size} pending requests. Sending to UI.")
                trySend(requests)
            } catch (e: Exception) {
                Log.e("FriendRequestFlow", "RECEIVER: Error fetching pending requests.", e)
                trySend(emptyList())
            }
        }

        // Fetch the initial data as soon as we start listening.
        fetchAndSend()

        val channel = supabaseClient.channel("friendship-requests")
        val changeJob = launch {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "friendship_requests"
                filter = "receiver_id=eq.$currentUserId"
            }.collect {
                Log.d("FriendRequestFlow", "RECEIVER: Detected a database change. Refetching requests.")
                fetchAndSend()
            }
        }

        launch {
            Log.d("FriendRequestFlow", "RECEIVER: Subscribing to realtime channel...")
            channel.subscribe()
        }

        awaitClose {
            Log.d("FriendRequestFlow", "RECEIVER: Closing listener for user $currentUserId.")
            changeJob.cancel()
            launch { channel.unsubscribe() }
        }
    }
}

// Data class for Supabase mapping (pas aan naar jouw kolommen)
data class FamilyMemberSupabase(
    val name: String,
    val status: String? = null
) 