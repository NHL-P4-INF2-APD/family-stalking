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
            val response = supabaseClient.from("family_members").select().decodeList<FamilyMemberSupabase>()
            response.map { FamilyMember(it.name, it.status ?: "") }
        } catch (e: Exception) {
            e.printStackTrace()
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
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("User not authenticated"))

            if (currentUserId == receiverId) {
                return Result.failure(Exception("You cannot add yourself as a friend."))
            }

            // Find all existing requests (in either direction) between the two users
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

            // 1. Check if they are already friends
            if (existingRequests.any { it.status == "accepted" }) {
                return Result.failure(Exception("You are already friends with this user."))
            }

            // 2. Delete any old, pending requests to avoid duplicates
            val pendingRequestIds = existingRequests
                .filter { it.status == "pending" }
                .map { it.id }

            if (pendingRequestIds.isNotEmpty()) {
                supabaseClient.from("friendship_requests").delete {
                    filter {
                        isIn("id", pendingRequestIds)
                    }
                }
            }
            
            // 3. Insert the new friend request
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
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FamilyRepository", "sendFriendshipRequest failed", e)
            Result.failure(e)
        }
    }
    
    override suspend fun acceptFriendshipRequest(requestId: String): Result<Unit> {
        return try {
            supabaseClient.from("friendship_requests")
                .update({ set("status", "accepted") }) {
                    filter { eq("id", requestId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
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
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        suspend fun fetchAndSend() {
            try {
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
                trySend(requests)
            } catch (e: Exception) {
                Log.e("FamilyRepository", "Error fetching pending requests", e)
                trySend(emptyList())
            }
        }

        fetchAndSend()

        val channel = supabaseClient.channel("friendship-requests")
        val changeJob = launch {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "friendship_requests"
                filter = "receiver_id=eq.$currentUserId"
            }.collect {
                fetchAndSend()
            }
        }

        launch {
            channel.subscribe()
        }

        awaitClose {
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