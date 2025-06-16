package com.familystalking.app.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.familystalking.app.domain.repository.FamilyRepository
import com.familystalking.app.domain.repository.PendingRequest
import com.familystalking.app.presentation.family.FamilyMember
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@Serializable
internal data class FriendshipRequest(
    val id: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    val status: String,
    @SerialName("timestamp") val timestampValue: String,
    @SerialName("sender_name_snapshot") val senderNameSnapshot: String? = null
)

@Serializable
internal data class Friend(
    @SerialName("user_id") val userId: String,
    @SerialName("friend_id") val friendId: String
)

@Serializable
data class ProfileDataForRepository(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String?,
    @SerialName("username") val username: String?,
    @SerialName("status") val status: String? = null
)

class FamilyRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : FamilyRepository {

    override suspend fun getFamilyMembers(): List<FamilyMember> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: return emptyList()
            val friendEntries = supabaseClient.postgrest["friends"].select {
                filter { eq("user_id", currentUserId) }
            }.decodeList<Friend>()

            val friendIds = friendEntries.map { it.friendId }
            if (friendIds.isEmpty()) return emptyList()

            val friendsProfileData = supabaseClient.postgrest["profiles"].select(
                Columns.list("id, name, username, status")
            ) {
                filter { isIn("id", friendIds) }
            }.decodeList<ProfileDataForRepository>()

            friendsProfileData.map { profile ->
                FamilyMember(
                    id = profile.id,
                    name = profile.name?.takeIf { it.isNotBlank() } ?: profile.username?.takeIf {it.isNotBlank()} ?: "Unknown User",
                    status = profile.status ?: "Offline"
                )
            }
        } catch (e: Exception) {
            Log.e("FamilyRepositoryImpl", "[getFamilyMembers] Failed", e)
            emptyList()
        }
    }

    override suspend fun getCurrentUser(): FamilyMember {
        val defaultNameFromEmail: (String?) -> String = { email ->
            email?.substringBefore('@')?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "Unknown"
        }
        val defaultStatus = "Offline"
        try {
            val authUser = supabaseClient.auth.currentUserOrNull()
            val userId = authUser?.id
            val userEmail = authUser?.email
            if (userId == null) {
                return FamilyMember(id = null, name = defaultNameFromEmail(userEmail), status = defaultStatus)
            }

            var profileData: ProfileDataForRepository? = null
            try {
                profileData = supabaseClient.postgrest["profiles"]
                    .select(Columns.list("id, name, username, status")) {
                        filter { eq("id", userId) }
                    }
                    .decodeSingleOrNull<ProfileDataForRepository>()
            } catch(re: RestException) {
                Log.e("FamilyRepositoryImpl", "[getCurrentUser] RestException fetching profile for $userId: ${re.message}", re)
            } catch (e: Exception) {
                Log.e("FamilyRepositoryImpl", "[getCurrentUser] Generic Exception fetching profile for $userId: ${e.message}", e)
            }

            val finalName = profileData?.name?.takeIf { it.isNotBlank() }
                ?: profileData?.username?.takeIf { it.isNotBlank() }
                ?: defaultNameFromEmail(userEmail)
            val finalStatus = profileData?.status?.takeIf { it.isNotBlank() } ?: defaultStatus

            return FamilyMember(id = userId, name = finalName, status = finalStatus)
        } catch (e: Exception) {
            Log.e("FamilyRepositoryImpl", "[getCurrentUser] Outer catch block exception: ${e.message}", e)
            val userEmail = supabaseClient.auth.currentUserOrNull()?.email
            return FamilyMember(id = null, name = defaultNameFromEmail(userEmail), status = defaultStatus)
        }
    }

    // --- ADDED THIS MISSING IMPLEMENTATION ---
    override suspend fun getCurrentUserId(): String? {
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id
            Log.d("FamilyRepositoryImpl", "[getCurrentUserId] User ID from auth: $userId")
            userId
        } catch (e: Exception) {
            Log.e("FamilyRepositoryImpl", "[getCurrentUserId] Error fetching user ID", e)
            null
        }
    }
    // --- END OF ADDED IMPLEMENTATION ---

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun sendFriendshipRequest(receiverId: String): Result<Unit> {
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
            ?: return Result.failure(Exception("SENDER: User not authenticated"))

        return try {
            if (currentUserId == receiverId) {
                return Result.failure(IllegalArgumentException("You cannot add yourself as a friend."))
            }

            val existingRequests = supabaseClient.postgrest["friendship_requests"].select {
                filter {
                    or {
                        and { eq("sender_id", currentUserId); eq("receiver_id", receiverId) }
                        and { eq("sender_id", receiverId); eq("receiver_id", currentUserId) }
                    }
                }
            }.decodeList<FriendshipRequest>()

            if (existingRequests.any { it.status == "accepted" }) {
                return Result.failure(IllegalStateException("You are already friends with this user."))
            }
            if (existingRequests.any { it.status == "pending" && it.senderId == currentUserId && it.receiverId == receiverId }) {
                return Result.failure(IllegalStateException("FRIEND_REQUEST_ALREADY_PENDING_SENT"))
            }
            if (existingRequests.any { it.status == "pending" && it.senderId == receiverId && it.receiverId == currentUserId }) {
                return Result.failure(IllegalStateException("FRIEND_REQUEST_ALREADY_PENDING_RECEIVED"))
            }

            val senderProfile = supabaseClient.postgrest["profiles"]
                .select(Columns.list("id, name, username")) {
                    filter { eq("id", currentUserId) }
                }
                .decodeSingleOrNull<ProfileDataForRepository>()

            val senderNameSnapshot = senderProfile?.name?.takeIf { it.isNotBlank() }
                ?: senderProfile?.username?.takeIf { it.isNotBlank() }
                ?: "A User"

            supabaseClient.postgrest["friendship_requests"].insert(
                FriendshipRequest(
                    id = UUID.randomUUID().toString(),
                    senderId = currentUserId,
                    receiverId = receiverId,
                    status = "pending",
                    timestampValue = Instant.now().toString(),
                    senderNameSnapshot = senderNameSnapshot
                )
            )
            Log.i("FamilyRepositoryImpl", "SENDER: Friendship request sent successfully from $currentUserId to $receiverId with sender name: $senderNameSnapshot")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FamilyRepositoryImpl", "SENDER: sendFriendshipRequest failed", e)
            if (e.message?.contains("duplicate key value violates unique constraint") == true) {
                return Result.failure(Exception("FRIEND_REQUEST_ALREADY_PENDING_SENT"))
            }
            Result.failure(e)
        }
    }

    override suspend fun acceptFriendshipRequest(request: PendingRequest): Result<Unit> {
        Log.d("FamilyRepositoryImpl", "[acceptFriendshipRequest] Attempting for request ID: ${request.id}, Sender: ${request.senderId}")

        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
        if (currentUserId == null) {
            Log.e("FamilyRepositoryImpl", "[acceptFriendshipRequest] Current user is not authenticated.")
            return Result.failure(IllegalStateException("User not authenticated to accept request."))
        }

        return try {
            val fullRequest = supabaseClient.postgrest["friendship_requests"]
                .select {
                    filter {
                        eq("id", request.id)
                        eq("receiver_id", currentUserId)
                        eq("status", "pending")
                    }
                }
                .decodeSingleOrNull<FriendshipRequest>()

            if (fullRequest == null) {
                Log.w("FamilyRepositoryImpl", "[acceptFriendshipRequest] Request ID ${request.id} not found, not pending, or not for this user ($currentUserId).")
                return Result.failure(NoSuchElementException("Friendship request not found or already processed."))
            }

            Log.d("FamilyRepositoryImpl", "[acceptFriendshipRequest] Found request to accept: $fullRequest")

            supabaseClient.postgrest["friendship_requests"]
                .update( { set("status", "accepted") } ) {
                    filter { eq("id", fullRequest.id) }
                }

            Log.d("FamilyRepositoryImpl", "[acceptFriendshipRequest] Request status updated to 'accepted' for ID: ${fullRequest.id}")

            supabaseClient.postgrest.rpc(
                function = "create_friendship",
                parameters = mapOf(
                    "user1_id" to fullRequest.senderId,
                    "user2_id" to fullRequest.receiverId
                )
            )
            Log.i("FamilyRepositoryImpl", "[acceptFriendshipRequest] Called create_friendship RPC for ${fullRequest.senderId} and ${fullRequest.receiverId}.")
            Result.success(Unit)
        } catch (e: RestException) {
            Log.e("FamilyRepositoryImpl", "[acceptFriendshipRequest] RestException for request ID: ${request.id}. Message: ${e.message}, Error: ${e.error}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("FamilyRepositoryImpl", "[acceptFriendshipRequest] Generic failed for request ID: ${request.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun declineFriendshipRequest(request: PendingRequest): Result<Unit> {
        Log.d("FamilyRepositoryImpl", "[declineFriendshipRequest] Attempting for request ID: ${request.id}")
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
        if (currentUserId == null) {
            Log.e("FamilyRepositoryImpl", "[declineFriendshipRequest] Current user is not authenticated.")
            return Result.failure(IllegalStateException("User not authenticated to decline request."))
        }

        return try {
            supabaseClient.postgrest["friendship_requests"]
                .delete {
                    filter {
                        eq("id", request.id)
                        eq("receiver_id", currentUserId)
                        eq("status", "pending")
                    }
                }
            Log.i("FamilyRepositoryImpl", "[declineFriendshipRequest] Delete request sent for ID: ${request.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FamilyRepositoryImpl", "[declineFriendshipRequest] Failed for request ID: ${request.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun getPendingRequests(userId: String): List<PendingRequest> {
        Log.d("FamilyRepositoryImpl", "[getPendingRequests] Attempting to fetch for receiver_id: $userId")
        try {
            val requestsData = supabaseClient.postgrest["friendship_requests"]
                .select {
                    filter {
                        eq("receiver_id", userId)
                        eq("status", "pending")
                    }
                }.decodeList<FriendshipRequest>()

            Log.d("FamilyRepositoryImpl", "[getPendingRequests] Raw requestsData count for receiver_id $userId (status 'pending'): ${requestsData.size}")

            return requestsData.mapNotNull { request ->
                val senderNameToDisplay = request.senderNameSnapshot?.takeIf { it.isNotBlank() } ?: "Unknown Sender"
                PendingRequest(
                    id = request.id,
                    senderId = request.senderId,
                    senderName = senderNameToDisplay,
                    timestamp = request.timestampValue
                )
            }
        } catch (e: RestException) {
            Log.e("FamilyRepositoryImpl", "[getPendingRequests] RestException for $userId: ${e.message} Error: ${e.error}", e)
            return emptyList()
        }
        catch (e: Exception) {
            Log.e("FamilyRepositoryImpl", "[getPendingRequests] Generic error for $userId: ${e.message}", e)
            return emptyList()
        }
    }

    override suspend fun searchUsers(query: String): List<FamilyMember> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: return emptyList()
            supabaseClient.postgrest["profiles"]
                .select(Columns.list("id, name, username, status")) {
                    filter {
                        or {
                            ilike("name", "%$query%")
                            ilike("username", "%$query%")
                        }
                        neq("id", currentUserId)
                    }
                    limit(10)
                }
                .decodeList<ProfileDataForRepository>()
                .map { profile ->
                    FamilyMember(
                        id = profile.id,
                        name = profile.name?.takeIf { it.isNotBlank() } ?: profile.username?.takeIf { it.isNotBlank() } ?: "User",
                        status = profile.status ?: "Offline"
                    )
                }
        } catch (e: Exception) {
            Log.e("FamilyRepositoryImpl", "searchUsers failed", e)
            emptyList()
        }
    }
}