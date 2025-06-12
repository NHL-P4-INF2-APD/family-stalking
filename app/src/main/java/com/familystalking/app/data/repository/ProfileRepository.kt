package com.familystalking.app.data.repository // Or your domain.repository if interface is there

import android.util.Log
import com.familystalking.app.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put // For building JSON update payload
import javax.inject.Inject
import javax.inject.Singleton

// --- INTERFACE DEFINITION ---
interface ProfileRepository {
    suspend fun getUserProfile(userId: String): Profile?
    suspend fun updateUsername(userId: String, newUsername: String): Boolean // <<< NEW METHOD
}

// --- IMPLEMENTATION CLASS ---
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ProfileRepository {

    companion object {
        private const val TABLE_PROFILES = "profiles"
    }

    override suspend fun getUserProfile(userId: String): Profile? {
        // ... (existing getUserProfile implementation) ...
        return try {
            withContext(Dispatchers.IO) {
                Log.d("ProfileRepositoryImpl", "Attempting to fetch and decode profile for user ID: $userId")
                val result = try {
                    supabaseClient.postgrest[TABLE_PROFILES]
                        .select(Columns.list("id, username")) {
                            filter {
                                eq("id", userId)
                            }
                        }
                        .decodeSingleOrNull<Profile>()
                } catch (e: RestException) {
                    Log.e("ProfileRepositoryImpl", "RestException for user ID $userId: ${e.message}", e)
                    null
                } catch (e: SerializationException) {
                    Log.e("ProfileRepositoryImpl", "SerializationException (decoding failed) for user ID $userId: ${e.message}", e)
                    null
                } catch (e: Exception) {
                    Log.e("ProfileRepositoryImpl", "Generic error fetching/decoding profile for user ID $userId: ${e.message}", e)
                    null
                }

                if (result == null) {
                    Log.w("ProfileRepositoryImpl", "No profile data returned or decoded for user ID: $userId. Check RLS, data, and previous error logs.")
                } else {
                    Log.d("ProfileRepositoryImpl", "Profile DECODED successfully for user ID $userId: ${result.username}")
                }
                result
            }
        } catch (e: Exception) {
            Log.e("ProfileRepositoryImpl", "Outer error in getUserProfile for user ID $userId: ${e.message}", e)
            null
        }
    }

    // --- NEW METHOD IMPLEMENTATION ---
    override suspend fun updateUsername(userId: String, newUsername: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                Log.d("ProfileRepositoryImpl", "Attempting to update username for user ID $userId to '$newUsername'")
                // Create the JSON object for the update
                // Only update the 'username' field
                val updateData = buildJsonObject {
                    put("username", newUsername)
                }

                supabaseClient.postgrest[TABLE_PROFILES]
                    .update(updateData) {
                        filter {
                            eq("id", userId) // Ensure we only update the specific user's profile
                        }
                    }
                // If the update call doesn't throw an exception, assume success for now.
                // Supabase client might not return a detailed success body for updates by default.
                // You could add .execute() and check response status if needed, but often lack of exception is enough.
                Log.i("ProfileRepositoryImpl", "Username update request sent for user ID $userId.")
                true // Indicate success
            }
        } catch (e: RestException) {
            Log.e("ProfileRepositoryImpl", "RestException updating username for $userId: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e("ProfileRepositoryImpl", "Generic error updating username for $userId: ${e.message}", e)
            false
        }
    }
}