package com.example.babycareapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        Log.d("AuthDebug", "User on start: ${currentUser?.email ?: "null"}")

        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()

        signInLauncher.launch(signInIntent)
    }

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val user = FirebaseAuth.getInstance().currentUser
            Log.d("AuthDebug", "Login success: ${user?.email}")

            val userMap = hashMapOf(
                "uid" to user?.uid,
                "email" to user?.email
            )

            FirebaseFirestore.getInstance().collection("users")
                .document(user?.uid ?: "")
                .set(userMap)
                .addOnSuccessListener {
                    Log.d("AuthDebug", "User saved to Firestore")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("AuthDebug", "Failed to save user: ${e.message}")
                    Toast.makeText(this, "Error saving user\n", Toast.LENGTH_LONG).show()
                }

        } else {
            Toast.makeText(this, "Login failed", Toast.LENGTH_LONG).show()
        }
    }
}
