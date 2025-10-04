package com.example.reportes.data.Remote

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Task

object FirebaseService {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun linkEmailAndPassword(
        email: String,
        password: String
    ): Task<AuthResult>? {
        val user = auth.currentUser
        return if (user != null) {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.linkWithCredential(credential)
        } else {
            null
        }
    }
}