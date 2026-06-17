package com.example.util

import android.net.Uri
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    val userId: String? get() = auth.currentUser?.uid

    suspend fun updateProfile(userProfile: UserProfile, avatarUri: Uri?) {
        val uid = userId ?: return
        
        var avatarUrl = userProfile.avatar
        if (avatarUri != null) {
            val ref = storage.reference.child("avatars/$uid.jpg")
            ref.putFile(avatarUri).await()
            avatarUrl = ref.downloadUrl.await().toString()
        }

        val userData = mapOf(
            "name" to userProfile.name,
            "bio" to userProfile.bio,
            "avatar" to avatarUrl,
            "points" to userProfile.points,
            "isStreaming" to userProfile.isStreaming
        )
        
        db.collection("users").document(uid).set(userData).await()
    }
}
