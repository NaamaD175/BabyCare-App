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
            // המשתמש כבר מחובר – אין צורך ב־signIn
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // לא מחובר – מפעיל תהליך התחברות
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
            // התחברות הצליחה
            val user = FirebaseAuth.getInstance().currentUser
            Log.d("AuthDebug", "Login success: ${user?.email}")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "ההתחברות נכשלה", Toast.LENGTH_LONG).show()
        }
    }
}
