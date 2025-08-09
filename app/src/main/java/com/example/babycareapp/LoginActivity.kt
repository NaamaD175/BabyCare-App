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

        //If the user is already signed in, skip the login flow and go to MainActivity
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        //Configure sign-in providers (Email/Password and Google)
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        //Build the FirebaseUI sign-in Intent
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()
        //Launch the FirebaseUI sign-in flow using Activity Result API
        signInLauncher.launch(signInIntent)
    }
    //Receives the sign-in result from FirebaseUI
    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            //Sign-in success: get the current user
            val user = FirebaseAuth.getInstance().currentUser

            val userMap = hashMapOf(
                "uid" to user?.uid,
                "email" to user?.email
            )

            FirebaseFirestore.getInstance().collection("users")
                .document(user?.uid ?: "")
                .set(userMap)
                .addOnSuccessListener {
                    //Navigate to MainActivity after successful save
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving user\n", Toast.LENGTH_LONG).show()
                }

        } else {
            //Sign-in failed or canceled by user
            Toast.makeText(this, "Login failed", Toast.LENGTH_LONG).show()
        }
    }
}
