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

data class FamilyMemberSupabase(
    val name: String,
    val status: String? = null
)

data class FriendshipRequest(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val status: String, // "pending", "accepted", "rejected"
    val timestamp: String
)

class FamilyRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : FamilyRepository {
    override suspend fun getFamilyMembers(): List<FamilyMember> {
        return try {
            val response = supabaseClient.from("family_members").select()
            // Verwacht een lijst van family_members met user_id, status, etc.
            val members = response.decodeList<FamilyMemberSupabase>()
            members.map { FamilyMember(it.name, it.status ?: "") }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getCurrentUser(): FamilyMember {
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return FamilyMember("Unknown", "")
            val response = supabaseClient.from("users").select { filter { eq("user_id", userId) } }
            val users = response.decodeList<FamilyMemberSupabase>()
            val user = users.firstOrNull()
            if (user != null) {
                FamilyMember(user.name, user.status ?: "")
            } else {
                FamilyMember("Unknown", "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            FamilyMember("Unknown", "")
        }
    }

    override suspend fun sendFriendshipRequest(receiverId: String): Result<Unit> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id 
                ?: return Result.failure(Exception("User not authenticated"))
            
            supabaseClient.from("friendship_requests").insert(
                FriendshipRequest(
                    id = UUID.randomUUID().toString(),
                    senderId = currentUserId,
                    receiverId = receiverId,
                    status = "pending",
                    timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(System.currentTimeMillis()))
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
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
                .update({ set("status", "rejected") }) {
                    filter { eq("id", requestId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPendingFriendshipRequests(): List<FriendshipRequest> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id 
                ?: return emptyList()
            
            supabaseClient.from("friendship_requests")
                .select {
                    filter { 
                        eq("receiver_id", currentUserId)
                        eq("status", "pending")
                    }
                }
                .decodeList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getCurrentUserId(): String? {
        return supabaseClient.auth.currentUserOrNull()?.id
    }
} 