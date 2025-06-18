package com.familystalking.app.data.repository
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.familystalking.app.domain.model.FamilyMemberLocation
import com.familystalking.app.domain.model.Location
import com.familystalking.app.domain.repository.LocationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class LocationRow(
    @SerialName("user_id") val userId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String,
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun toDomain() =
        Location(
            id = UUID.randomUUID(),
            userId = UUID.fromString(userId),
            latitude = latitude,
            longitude = longitude,
            timestamp = Instant.parse(timestamp),
        )
}

@Serializable
private data class ProfileRow(
    val id: String,
    val name: String,
    val username: String? = null,
)

@Serializable
private data class FriendRow(
    @SerialName("user_id") val userId: String,
    @SerialName("friend_id") val friendId: String
)

@Singleton
class SupabaseLocationRepository
@Inject
constructor(private val supabaseClient: SupabaseClient) : LocationRepository {
    
    companion object {
        private const val TAG = "SupabaseLocationRepo"
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun updateUserLocation(
        latitude: Double,
        longitude: Double,
    ) {
        try {
            Log.d(TAG, "🔄 updateUserLocation called with lat=$latitude, lng=$longitude")
            
            val currentUser = supabaseClient.auth.currentUserOrNull()
            Log.d(TAG, "Current user from Supabase: ${currentUser?.id}")
            
            val userId = currentUser?.id
            if (userId == null) {
                Log.e(TAG, "❌ User must be authenticated")
                return
            }

            val locationRow = LocationRow(
                userId = userId,
                latitude = latitude,
                longitude = longitude,
                timestamp = Instant.now().toString(),
            )
            
            Log.d(TAG, "Inserting location row: $locationRow")
            
            supabaseClient.postgrest["locations"].insert(locationRow)
            Log.d(TAG, "✅ Location updated successfully for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update location", e)
            throw e
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getUserLocation(userId: UUID): Location? {
        return try {
            supabaseClient.postgrest["locations"]
                .select(Columns.list("user_id", "latitude", "longitude", "timestamp")) {
                    filter { eq("user_id", userId.toString()) }
                    order("timestamp", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<LocationRow>()
                .firstOrNull()
                ?.toDomain()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user location for $userId", e)
            null
        }
    }

    private suspend fun getFamilyMemberName(userId: String): String {
        return try {
            supabaseClient.postgrest["profiles"]
                .select(Columns.list("id", "name", "username")) {
                    filter { eq("id", userId) }
                }
                .decodeList<ProfileRow>()
                .firstOrNull()
                ?.let { profile ->
                    profile.username?.takeIf { it.isNotBlank() } 
                        ?: profile.name?.takeIf { it.isNotBlank() } 
                        ?: "Unknown"
                } ?: "Unknown"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get family member name for $userId", e)
            "Unknown"
        }
    }

    private suspend fun getFriendIds(): List<String> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id 
                ?: return emptyList()
                
            supabaseClient.postgrest["friends"]
                .select(Columns.list("friend_id")) {
                    filter { eq("user_id", currentUserId) }
                }
                .decodeList<FriendRow>()
                .map { it.friendId }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get friend IDs", e)
            emptyList()
        }
    }

    override suspend fun getAuthenticatedUserId(): UUID? {
        return supabaseClient.auth.currentUserOrNull()?.id?.let {
            UUID.fromString(it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getFamilyMembersLocations(): Flow<List<FamilyMemberLocation>> {
        return flow {
            // Helper function to fetch locations for a list of friend IDs
            suspend fun fetchLocationsForFriends(friendIds: List<String>): List<FamilyMemberLocation> {
                if (friendIds.isEmpty()) {
                    return emptyList()
                }

                return try {
                    supabaseClient.postgrest["locations"]
                        .select(Columns.list("user_id", "latitude", "longitude", "timestamp")) {
                            filter {
                                isIn("user_id", friendIds)
                            }
                            order("timestamp", Order.DESCENDING)
                        }
                        .decodeList<LocationRow>()
                        .groupBy { it.userId }
                        .mapNotNull { (userId, locations) ->
                            val latestLocation = locations.maxByOrNull { Instant.parse(it.timestamp) }
                            latestLocation?.let {
                                FamilyMemberLocation(
                                    userId = UUID.fromString(userId),
                                    name = getFamilyMemberName(userId),
                                    location = it.toDomain()
                                )
                            }
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching locations for friends", e)
                    emptyList()
                }
            }

            try {
                // Initial fetch
                val initialFriendIds = getFriendIds()
                Log.d(TAG, "Initial fetch: Found ${initialFriendIds.size} friends")
                
                val initialLocations = fetchLocationsForFriends(initialFriendIds)
                Log.d(TAG, "Initial fetch: Found locations for ${initialLocations.size} friends")
                emit(initialLocations)

                // Start polling mechanism
                while (true) {
                    kotlinx.coroutines.delay(30000) // Poll every 30 seconds
                    
                    try {
                        val updatedFriendIds = getFriendIds()
                        Log.d(TAG, "Polling: Found ${updatedFriendIds.size} friends")
                        
                        val updatedLocations = fetchLocationsForFriends(updatedFriendIds)
                        Log.d(TAG, "Polling: Found locations for ${updatedLocations.size} friends")
                        
                        emit(updatedLocations)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during polling for location updates", e)
                        // Continue polling even if there's an error
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in getFamilyMembersLocations", e)
                emit(emptyList())
            }
        }.catch { e ->
            Log.e(TAG, "Flow error in getFamilyMembersLocations", e)
            emit(emptyList())
        }
    }
}