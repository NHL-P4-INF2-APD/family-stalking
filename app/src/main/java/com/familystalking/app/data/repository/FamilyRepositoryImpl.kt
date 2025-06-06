package com.familystalking.app.data.repository

import com.familystalking.app.domain.repository.FamilyRepository
import com.familystalking.app.presentation.family.FamilyMember
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.gotrue.auth
import javax.inject.Inject
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import kotlinx.coroutines.flow.emptyFlow

data class FamilyMemberSupabase(
    @SerialName("name") val name: String,
    @SerialName("status") val status: String? = null
)

@Serializable
data class FriendshipRequest(
    @SerialName("id") val id: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("status") val status: String,
    @SerialName("timestamp") val timestamp: String
)

@Serializable
data class FriendEdge(
    @SerialName("user_id") val userId: String,
    @SerialName("friend_id") val friendId: String
)

class FamilyRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : FamilyRepository {
    override suspend fun getFamilyMembers(): List<FamilyMember> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: return emptyList()

            // Step 1: Get the IDs of all friends
            val friendIdMaps = supabaseClient.from("friends").select {
                filter { eq("user_id", currentUserId) }
            }.decodeList<Map<String, String>>()
            val friendIds = friendIdMaps.mapNotNull { it["friend_id"] }

            if (friendIds.isEmpty()) return emptyList()

            // Step 2: Get user details for the retrieved friend IDs
            val friends = supabaseClient.from("users").select {
                filter {
                    isIn("user_id", friendIds)
                }
            }.decodeList<FamilyMemberSupabase>()

            friends.map { FamilyMember(it.name, it.status ?: "No status") }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getCurrentUser(): FamilyMember {
        try {
            val authUser = supabaseClient.auth.currentUserOrNull()
            val userId = authUser?.id
            val userEmail = authUser?.email
            val nameFromEmail = userEmail?.substringBefore('@') ?: "Unknown"

            if (userId == null) {
                return FamilyMember(nameFromEmail, "")
            }

            val response = supabaseClient.from("users").select { filter { eq("user_id", userId) } }
            val users = response.decodeList<FamilyMemberSupabase>()
            val user = users.firstOrNull()

            val finalName = if (user != null && user.name.isNotBlank() && user.name != "Unknown") {
                user.name
            } else {
                nameFromEmail
            }
            val finalStatus = user?.status ?: ""

            return FamilyMember(finalName, finalStatus)
        } catch (e: Exception) {
            e.printStackTrace()
            val userEmail = supabaseClient.auth.currentUserOrNull()?.email
            val nameFromEmail = userEmail?.substringBefore('@') ?: "Unknown"
            return FamilyMember(nameFromEmail, "")
        }
    }

    override suspend fun sendFriendshipRequest(receiverId: String): Result<Unit> {
        Log.d("FamilyRepo", "Attempting to send friend request to $receiverId")
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("User not authenticated"))
            Log.d("FamilyRepo", "Current user ID: $currentUserId")

            if (currentUserId == receiverId) {
                Log.w("FamilyRepo", "User tried to add themselves.")
                return Result.failure(Exception("You cannot add yourself as a friend."))
            }

            // Check if a friendship or pending request already exists
            val existingRequest = supabaseClient.from("friendship_requests").select {
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

            if (existingRequest.any { it.status == "accepted" }) {
                Log.w("FamilyRepo", "Friendship already exists.")
                return Result.failure(Exception("You are already friends with this user."))
            }
            if (existingRequest.any { it.status == "pending" }) {
                Log.w("FamilyRepo", "Pending request already exists.")
                return Result.failure(Exception("A friend request has already been sent."))
            }

            Log.d("FamilyRepo", "No existing request found. Proceeding to insert new request.")
            supabaseClient.from("friendship_requests").insert(
                FriendshipRequest(
                    id = UUID.randomUUID().toString(),
                    senderId = currentUserId,
                    receiverId = receiverId,
                    status = "pending",
                    timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(System.currentTimeMillis()))
                )
            )
            Log.d("FamilyRepo", "Friend request insert successful for receiver $receiverId")
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e("FamilyRepo", "Failed to send friend request", t)
            Result.failure(Exception("An unexpected error occurred: ${t.message}"))
        }
    }

    override suspend fun acceptFriendshipRequest(requestId: String): Result<Unit> {
        return try {
            val request = supabaseClient.from("friendship_requests").select {
                filter { eq("id", requestId) }
            }.decodeSingle<FriendshipRequest>()

            // Update request status
            supabaseClient.from("friendship_requests")
                .update({ set("status", "accepted") }) {
                    filter { eq("id", requestId) }
                }

            // Create two-way friendship
            supabaseClient.from("friends").insert(listOf(
                FriendEdge(userId = request.senderId, friendId = request.receiverId),
                FriendEdge(userId = request.receiverId, friendId = request.senderId)
            ))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectFriendshipRequest(requestId: String): Result<Unit> {
        return try {
            supabaseClient.from("friendship_requests")
                .update({ set("status", "rejected") }) {
                    filter { eq("id", requestId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun listenToFriendshipRequestChanges(): Flow<PostgresAction> {
        return try {
            val channel = supabaseClient.channel("friendship_requests_realtime")
            // The subscription must be launched in a coroutine scope.
            CoroutineScope(Dispatchers.IO).launch {
                channel.subscribe()
            }
            // Return the flow that listens for any changes in the table.
            channel.postgresChangeFlow(schema = "public") {
                table = "friendship_requests"
            }
        } catch (e: Exception) {
            Log.e("FamilyRepo", "Error listening to friendship request changes", e)
            emptyFlow()
        }
    }

    override suspend fun getPendingRequestsForUser(userId: String): List<FriendshipRequest> {
        return try {
            supabaseClient.from("friendship_requests")
                .select {
                    filter {
                        eq("receiver_id", userId)
                        eq("status", "pending")
                    }
                }
                .decodeList()
        } catch (e: Exception) {
            Log.e("FamilyRepo", "Could not get pending requests for user $userId", e)
            emptyList()
        }
    }

    override suspend fun getCurrentUserId(): String? {
        return supabaseClient.auth.currentUserOrNull()?.id
    }

    override suspend fun getUserById(userId: String): FamilyMember? {
        return try {
            supabaseClient.from("users").select {
                filter { eq("user_id", userId) }
            }.decodeSingleOrNull<FamilyMemberSupabase>()?.let {
                // Here we can only return the name from the database, can't derive from email.
                FamilyMember(it.name, it.status ?: "No status")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
} 