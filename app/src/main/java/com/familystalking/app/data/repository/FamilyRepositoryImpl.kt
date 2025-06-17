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
            Log.d("FamilyRepositoryImpl", "[getFamilyMembers] Current user ID: $currentUserId")

            val myFriendships = supabaseClient.postgrest["friends"].select {
                filter { eq("user_id", currentUserId) }
            }.decodeList<Friend>()

            if (myFriendships.isEmpty()) {
                Log.d("FamilyRepositoryImpl", "[getFamilyMembers] No friendships found for user: $currentUserId")
                return emptyList()
            }

            val friendIds = myFriendships.map { it.friendId }
            Log.d("FamilyRepositoryImpl", "[getFamilyMembers] Friend IDs to lookup: $friendIds")

            val friendsProfileData = supabaseClient.postgrest["profiles"].select(
                Columns.list("id, name, username, status")
            ) {
                filter { isIn("id", friendIds) }
            }.decodeList<ProfileDataForRepository>()

            Log.d("FamilyRepositoryImpl", "[getFamilyMembers] Found ${friendsProfileData.size} profile records for friends")

            val result = friendsProfileData.map { profile ->
                FamilyMember(
                    id = profile.id,
                    // UPDATED: Prioritize username, then name, then a default
                    name = profile.username?.takeIf { it.isNotBlank() } ?: profile.name?.takeIf { it.isNotBlank() } ?: "Unknown User",
                    status = profile.status ?: "Offline"
                )
            }

            Log.d("FamilyRepositoryImpl", "[getFamilyMembers] Mapped ${result.size} family members to display")
            result
        } catch (e: Exception) {
            Log.e("FamilyRepositoryImpl", "[getFamilyMembers] EXCEPTION: ${e.message}", e)
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

            // UPDATED: Prioritize username, then name, then email-based default
            val finalName = profileData?.username?.takeIf { it.isNotBlank() }
                ?: profileData?.name?.takeIf { it.isNotBlank() }
                ?: defaultNameFromEmail(userEmail)
            val finalStatus = profileData?.status?.takeIf { it.isNotBlank() } ?: defaultStatus

            return FamilyMember(id = userId, name = finalName, status = finalStatus)
        } catch (e: Exception) {
            Log.e("FamilyRepositoryImpl", "[getCurrentUser] Outer catch block exception: ${e.message}", e)
            val userEmail = supabaseClient.auth.currentUserOrNull()?.email
            return FamilyMember(id = null, name = defaultNameFromEmail(userEmail), status = defaultStatus)
        }
    }

    override suspend fun getCurrentUserId(): String? {
        return try {
            supabaseClient.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Log.e("FamilyRepositoryImpl", "[getCurrentUserId] Error fetching user ID", e)
            null
        }
    }

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

            // UPDATED: Prioritize username for the snapshot
            val senderNameSnapshot = senderProfile?.username?.takeIf { it.isNotBlank() }
                ?: senderProfile?.name?.takeIf { it.isNotBlank() }
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
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: return Result.failure(IllegalStateException("User not authenticated to accept request."))

        return try {
            val fullRequest = supabaseClient.postgrest["friendship_requests"]
                .select { filter { eq("id", request.id); eq("receiver_id", currentUserId); eq("status", "pending") } }
                .decodeSingleOrNull<FriendshipRequest>() ?: return Result.failure(NoSuchElementException("Friendship request not found or already processed."))

            supabaseClient.postgrest["friendship_requests"]
                .update( { set("status", "accepted") } ) { filter { eq("id", fullRequest.id) } }

            supabaseClient.postgrest.rpc(
                function = "create_friendship",
                parameters = mapOf("user1_id" to fullRequest.senderId, "user2_id" to fullRequest.receiverId)
            )
            Log.i("FamilyRepositoryImpl", "[acceptFriendshipRequest] Called create_friendship RPC for ${fullRequest.senderId} and ${fullRequest.receiverId}.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FamilyRepositoryImpl", "[acceptFriendshipRequest] Failed for request ID: ${request.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun declineFriendshipRequest(request: PendingRequest): Result<Unit> {
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: return Result.failure(IllegalStateException("User not authenticated to decline request."))

        return try {
            supabaseClient.postgrest["friendship_requests"]
                .delete { filter { eq("id", request.id); eq("receiver_id", currentUserId); eq("status", "pending") } }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FamilyRepositoryImpl", "[declineFriendshipRequest] Failed for request ID: ${request.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun getPendingRequests(userId: String): List<PendingRequest> {
        try {
            val requestsData = supabaseClient.postgrest["friendship_requests"]
                .select { filter { eq("receiver_id", userId); eq("status", "pending") } }
                .decodeList<FriendshipRequest>()

            return requestsData.mapNotNull { request ->
                PendingRequest(
                    id = request.id,
                    senderId = request.senderId,
                    senderName = request.senderNameSnapshot?.takeIf { it.isNotBlank() } ?: "Unknown Sender",
                    timestamp = request.timestampValue
                )
            }
        } catch (e: Exception) {
            Log.e("FamilyRepositoryImpl", "[getPendingRequests] Error for $userId: ${e.message}", e)
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
                        // UPDATED: Prioritize username for consistency in search results
                        name = profile.username?.takeIf { it.isNotBlank() } ?: profile.name?.takeIf { it.isNotBlank() } ?: "User",
                        status = profile.status ?: "Offline"
                    )
                }
        } catch (e: Exception) {
            Log.e("FamilyRepositoryImpl", "searchUsers failed", e)
            emptyList()
        }
    }
}