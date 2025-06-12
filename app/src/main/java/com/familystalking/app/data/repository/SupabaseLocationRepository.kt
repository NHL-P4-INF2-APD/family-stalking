package com.familystalking.app.data.repository
import android.os.Build
import androidx.annotation.RequiresApi
import com.familystalking.app.domain.model.FamilyMemberLocation
import com.familystalking.app.domain.model.Location
import com.familystalking.app.domain.repository.LocationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
private data class LocationRow(
    @Contextual val userId: UUID,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String,
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun toDomain() =
        Location(
            id = UUID.randomUUID(),
            userId = userId,
            latitude = latitude,
            longitude = longitude,
            timestamp = Instant.parse(timestamp),
        )
}

@Serializable
private data class ProfileRow(
    @Contextual val id: UUID,
    val name: String,
)

@Singleton
class SupabaseLocationRepository
@Inject
constructor(private val supabaseClient: SupabaseClient) : LocationRepository {
    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun updateUserLocation(
        latitude: Double,
        longitude: Double,
    ) {
        val userId =
            supabaseClient.auth.currentUserOrNull()?.id?.let {
                UUID.fromString(it)
            }
                ?: throw IllegalStateException("User must be authenticated")

        supabaseClient.postgrest["locations"].insert(
            LocationRow(
                userId = userId,
                latitude = latitude,
                longitude = longitude,
                timestamp = Instant.now().toString(),
            ),
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getUserLocation(userId: UUID): Location? {
        return supabaseClient.postgrest["locations"]
            .select(Columns.list("*")) {
                filter { eq("user_id", userId.toString()) }
                order("timestamp", Order.DESCENDING)
                limit(1)
            }
            .decodeList<LocationRow>()
            .firstOrNull()
            ?.toDomain()
    }

    private suspend fun getFamilyMemberName(userId: UUID): String {
        return supabaseClient.postgrest["profiles"]
            .select(Columns.list("name")) {
                filter { eq("id", userId.toString()) }
            }
            .decodeList<ProfileRow>()
            .firstOrNull()
            ?.name ?: "Unknown"
    }

    override suspend fun getAuthenticatedUserId(): UUID? {
        return supabaseClient.auth.currentUserOrNull()?.id?.let {
            UUID.fromString(it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getFamilyMembersLocations(): Flow<List<FamilyMemberLocation>> = flow {
        val familyMembersLocations = supabaseClient.postgrest["locations"]
            .select(Columns.list("user_id", "latitude", "longitude", "timestamp")) {
                order("timestamp", Order.DESCENDING)
            }
            .decodeList<LocationRow>()
            .groupBy { it.userId }
            .mapNotNull { (userId, locations) ->
                val latestLocation = locations.maxByOrNull { Instant.parse(it.timestamp) }
                latestLocation?.let {
                    FamilyMemberLocation(
                        userId = userId,
                        name = getFamilyMemberName(userId),
                        location = it.toDomain()
                    )
                }
            }
        emit(familyMembersLocations)
    }
}