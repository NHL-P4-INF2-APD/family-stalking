package com.familystalking.app.domain.repository

import com.familystalking.app.presentation.family.FamilyMember

interface FamilyRepository {
    suspend fun getFamilyMembers(): List<FamilyMember>
    suspend fun getCurrentUser(): FamilyMember
} 