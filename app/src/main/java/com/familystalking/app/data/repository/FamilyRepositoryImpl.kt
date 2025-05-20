package com.familystalking.app.data.repository

import com.familystalking.app.domain.repository.FamilyRepository
import com.familystalking.app.presentation.family.FamilyMember
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.gotrue.auth
import javax.inject.Inject

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
                FamilyMember(user.name ?: "Unknown", user.status ?: "")
            } else {
                FamilyMember("Unknown", "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            FamilyMember("Unknown", "")
        }
    }
}

// Data class voor Supabase mapping (pas aan naar jouw kolommen)
data class FamilyMemberSupabase(
    val name: String,
    val status: String? = null
) 