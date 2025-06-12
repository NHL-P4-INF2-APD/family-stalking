package com.familystalking.app.data.model // Ensure this package matches its location

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable // Necessary for Kotlinx Serialization to work with Supabase client
data class Profile(
    @SerialName("id") // This tells the serializer that the JSON key "id" maps to this field
    val id: String,   // Matches the 'uuid' type in Supabase, treated as String in Kotlin

    @SerialName("username") // This tells the serializer that the JSON key "username" maps to this field
    val username: String? = null // Matches the 'text' type in Supabase, nullable because it can be NULL
)