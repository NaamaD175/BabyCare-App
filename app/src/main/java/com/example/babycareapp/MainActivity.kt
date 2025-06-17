package com.example.babycareapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.babycareapp.fragments.HomeFragment
import com.example.babycareapp.fragments.MessagesFragment
import com.example.babycareapp.fragments.SearchFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_FRAME_container, fragment)
            .commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 💡 בדיקה אם יש משתמש מחובר
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // 💥 אם אין משתמש - מחזירים ישר למסך Login
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // חשוב כדי לעצור את onCreate כאן
            return
        }

        // אם יש משתמש, ממשיכים לטעון את האפליקציה
        setContentView(R.layout.activity_main)

        // ברירת מחדל - עמוד הבית
        loadFragment(HomeFragment())

        val bottomNav = findViewById<BottomNavigationView>(R.id.main_NAV_bottom)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_search -> {
                    loadFragment(SearchFragment())
                    true
                }
                R.id.nav_messages -> {
                    loadFragment(MessagesFragment())
                    true
                }
                else -> false
            }
        }
    }
}
