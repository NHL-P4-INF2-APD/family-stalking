package com.familystalking.app.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.familystalking.app.domain.repository.FamilyRepository
import com.familystalking.app.domain.repository.PendingRequest
import com.familystalking.app.presentation.family.FamilyMember // Verify this import path
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@Serializable
internal data class DbFriendshipRequest(
    val id: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    val status: String,
    @SerialName("timestamp") val timestampValue: String,
    @SerialName("sender_name_snapshot") val senderNameSnapshot: String? = null
)

@Serializable
internal data class DbFriend(
    @SerialName("user_id") val userId: String,
    @SerialName("friend_id") val friendId: String
)

@Serializable
internal data class DbProfile(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String?,
    @SerialName("username") val username: String?,
    @SerialName("status") val status: String? = null
)

class FamilyRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : FamilyRepository {

    override suspend fun getFamilyMembers(): List<FamilyMember> {
        Log.i("FamilyRepoImpl", "[getFamilyMembers] Called")
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
            if (currentUserId == null) {
                Log.w("FamilyRepoImpl", "[getFamilyMembers] Current user ID is null")
                return emptyList()
            }
            Log.d("FamilyRepoImpl", "[getFamilyMembers] CurrentUserID: $currentUserId")

            val friendEntries = supabaseClient.postgrest["friends"].select {
                filter { eq("user_id", currentUserId) }
            }.decodeList<DbFriend>()
            Log.d("FamilyRepoImpl", "[getFamilyMembers] Found ${friendEntries.size} friend entries: $friendEntries")

            val friendIds = friendEntries.map { it.friendId }
            if (friendIds.isEmpty()) return emptyList()
            Log.d("FamilyRepoImpl", "[getFamilyMembers] Friend IDs: $friendIds")

            val friendsProfileData = supabaseClient.postgrest["profiles"].select {
                filter { isIn("id", friendIds) }
            }.decodeList<DbProfile>()
            Log.d("FamilyRepoImpl", "[getFamilyMembers] Fetched ${friendsProfileData.size} profiles for friends")

            friendsProfileData.map { profile ->
                FamilyMember(
                    id = profile.id,
                    name = profile.name?.takeIf { it.isNotBlank() } ?: profile.username?.takeIf {it.isNotBlank()} ?: "Unknown User",
                    status = profile.status?.takeIf { it.isNotBlank() } ?: "Offline"
                )
            }
        } catch (e: Exception) {
            Log.e("FamilyRepoImpl", "[getFamilyMembers] Exception", e)
            emptyList()
        }
    }

    override suspend fun getCurrentUser(): FamilyMember {
        Log.i("FamilyRepoImpl", "[getCurrentUser] Called")
        val defaultName: (String?) -> String = { email ->
            email?.substringBefore('@')?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "Unknown"
        }
        try {
            val authUser = supabaseClient.auth.currentUserOrNull()
            val userId = authUser?.id
            val userEmail = authUser?.email
            if (userId == null) {
                Log.w("FamilyRepoImpl", "[getCurrentUser] Auth user ID is null")
                return FamilyMember(id = null, name = defaultName(userEmail), status = "Offline")
            }
            Log.d("FamilyRepoImpl", "[getCurrentUser] Auth User ID: $userId")

            val profileData = supabaseClient.postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<DbProfile>()
            Log.d("FamilyRepoImpl", "[getCurrentUser] Profile data from DB: $profileData")

            val finalName = profileData?.name?.takeIf { it.isNotBlank() }
                ?: profileData?.username?.takeIf { it.isNotBlank() }
                ?: defaultName(userEmail)
            val finalStatus = profileData?.status?.takeIf { it.isNotBlank() } ?: "Offline"
            return FamilyMember(id = userId, name = finalName, status = finalStatus)
        } catch (e: Exception) {
            Log.e("FamilyRepoImpl", "[getCurrentUser] Exception", e)
            val userEmail = supabaseClient.auth.currentUserOrNull()?.email
            return FamilyMember(id = null, name = defaultName(userEmail), status = "Offline")
        }
    }

    override suspend fun getCurrentUserId(): String? {
        val userId = supabaseClient.auth.currentUserOrNull()?.id
        Log.d("FamilyRepoImpl", "[getCurrentUserId] User ID: $userId")
        return userId
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun sendFriendshipRequest(receiverId: String): Result<Unit> {
        Log.i("FamilyRepoImpl", "[sendFriendshipRequest] To: $receiverId")
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
            ?: return Result.failure(Exception("User not authenticated"))
        if (currentUserId == receiverId) return Result.failure(IllegalArgumentException("Cannot send request to self."))

        return try {
            val existing = supabaseClient.postgrest["friendship_requests"].select {
                filter { or {
                    and { eq("sender_id", currentUserId); eq("receiver_id", receiverId) }
                    and { eq("sender_id", receiverId); eq("receiver_id", currentUserId) }
                }}
            }.decodeList<DbFriendshipRequest>()

            if (existing.any { it.status == "accepted" }) return Result.failure(IllegalStateException("Already friends."))
            if (existing.any { it.status == "pending" && it.senderId == currentUserId }) return Result.failure(IllegalStateException("FRIEND_REQUEST_ALREADY_PENDING_SENT"))
            if (existing.any { it.status == "pending" && it.senderId == receiverId }) return Result.failure(IllegalStateException("FRIEND_REQUEST_ALREADY_PENDING_RECEIVED"))

            val senderProfile = supabaseClient.postgrest["profiles"]
                .select { filter { eq("id", currentUserId) } }
                .decodeSingleOrNull<DbProfile>()
            val snapshotName = senderProfile?.name?.takeIf { it.isNotBlank() } ?: senderProfile?.username?.takeIf { it.isNotBlank() } ?: "A User"

            supabaseClient.postgrest["friendship_requests"].insert(
                DbFriendshipRequest(
                    id = UUID.randomUUID().toString(),
                    senderId = currentUserId, receiverId = receiverId, status = "pending",
                    timestampValue = Instant.now().toString(), senderNameSnapshot = snapshotName
                )
            )
            Log.i("FamilyRepoImpl", "[sendFriendshipRequest] Success from $currentUserId to $receiverId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FamilyRepoImpl", "[sendFriendshipRequest] Exception", e)
            if (e.message?.contains("duplicate key") == true) Result.failure(Exception("FRIEND_REQUEST_ALREADY_PENDING_SENT")) else Result.failure(e)
        }
    }

    override suspend fun acceptFriendshipRequest(request: PendingRequest): Result<Unit> {
        Log.i("FamilyRepoImpl", "[acceptFriendshipRequest] Request ID: ${request.id}")
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        return try {
            val fullRequest = supabaseClient.postgrest["friendship_requests"].select {
                filter { eq("id", request.id); eq("receiver_id", currentUserId); eq("status", "pending") }
            }.decodeSingleOrNull<DbFriendshipRequest>()

            if (fullRequest == null) return Result.failure(NoSuchElementException("Request not found/valid for acceptance."))

            supabaseClient.postgrest["friendship_requests"].update( { set("status", "accepted") } ) {
                filter { eq("id", fullRequest.id) }
            }
            supabaseClient.postgrest.rpc(
                function = "create_friendship",
                parameters = mapOf("user1_id" to fullRequest.senderId, "user2_id" to fullRequest.receiverId)
            )
            Log.i("FamilyRepoImpl", "[acceptFriendshipRequest] Success for ID: ${request.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FamilyRepoImpl", "[acceptFriendshipRequest] Exception for ID: ${request.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun declineFriendshipRequest(request: PendingRequest): Result<Unit> {
        Log.i("FamilyRepoImpl", "[declineFriendshipRequest] Request ID: ${request.id}")
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        return try {
            supabaseClient.postgrest["friendship_requests"].delete {
                filter { eq("id", request.id); eq("receiver_id", currentUserId); eq("status", "pending") }
            }
            Log.i("FamilyRepoImpl", "[declineFriendshipRequest] Success for ID: ${request.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FamilyRepoImpl", "[declineFriendshipRequest] Exception for ID: ${request.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun getPendingRequests(userId: String): List<PendingRequest> {
        Log.i("FamilyRepoImpl", "[getPendingRequests] For receiver_id: $userId")
        return try {
            val requestsData = supabaseClient.postgrest["friendship_requests"].select {
                filter { eq("receiver_id", userId); eq("status", "pending") }
                order("timestamp", Order.DESCENDING) // Optional: order by newest first
            }.decodeList<DbFriendshipRequest>()
            Log.d("FamilyRepoImpl", "[getPendingRequests] Found ${requestsData.size} raw requests from DB for $userId")

            requestsData.map { dbReq ->
                PendingRequest(
                    id = dbReq.id,
                    senderId = dbReq.senderId,
                    senderName = dbReq.senderNameSnapshot?.takeIf { it.isNotBlank() } ?: "Unknown Sender",
                    timestamp = dbReq.timestampValue
                )
            }
        } catch (e: Exception) {
            Log.e("FamilyRepoImpl", "[getPendingRequests] Exception for $userId", e)
            emptyList()
        }
    }

    override suspend fun searchUsers(query: String): List<FamilyMember> {
        Log.i("FamilyRepoImpl", "[searchUsers] Query: $query")
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: return emptyList()
            val profiles = supabaseClient.postgrest["profiles"].select {
                filter {
                    or { ilike("name", "%$query%"); ilike("username", "%$query%") }
                    neq("id", currentUserId)
                }
                limit(10)
            }.decodeList<DbProfile>()
            Log.d("FamilyRepoImpl", "[searchUsers] Found ${profiles.size} profiles for query '$query'")

            profiles.map { profile ->
                FamilyMember(
                    id = profile.id,
                    name = profile.name?.takeIf { it.isNotBlank() } ?: profile.username?.takeIf { it.isNotBlank() } ?: "User",
                    status = profile.status?.takeIf { it.isNotBlank() } ?: "Offline"
                )
            }
        } catch (e: Exception) {
            Log.e("FamilyRepoImpl", "[searchUsers] Exception for query '$query'", e)
            emptyList()
        }
    }
}