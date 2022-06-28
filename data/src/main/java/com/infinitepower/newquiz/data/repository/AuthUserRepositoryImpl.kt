package com.infinitepower.newquiz.data.repository

import android.net.Uri
import androidx.core.net.toUri
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.infinitepower.newquiz.core.common.DEFAULT_USER_PHOTO
import com.infinitepower.newquiz.domain.repository.user.auth.AuthUserRepository
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthUserRepositoryImpl @Inject constructor() : AuthUserRepository {
    private val authUser: FirebaseUser?
        get() = Firebase.auth.currentUser

    override val isSignedIn: Boolean
        get() = authUser != null

    override val uid: String?
        get() = authUser?.uid

    override val name: String?
        get() = authUser?.displayName

    override val photoUrl: Uri?
        get() = authUser?.photoUrl

    override suspend fun refreshAuthUser() {
        val name = authUser?.displayName ?: "user${uid?.hashCode() ?: SecureRandom().nextInt()}"
        val photoUri = authUser?.photoUrl ?: DEFAULT_USER_PHOTO.toUri()

        val requestConfig = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .setPhotoUri(photoUri)
            .build()

        authUser?.updateProfile(requestConfig)?.await()
    }
}