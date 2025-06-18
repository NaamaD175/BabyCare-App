package com.example.babycareapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.babycareapp.LoginActivity
import com.example.babycareapp.R
import com.example.babycareapp.adapters.BabysitterAdapter
import com.example.babycareapp.models.Babysitter
import com.example.babycareapp.models.Review
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BabysitterAdapter
    private val babysitters = mutableListOf<Babysitter>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)


        recyclerView = view.findViewById(R.id.main_RV_babysitters)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = BabysitterAdapter(babysitters) { selectedBabysitter ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_FRAME_container, BabysitterDetailsFragment.newInstance(selectedBabysitter))
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = adapter

        val addButton = view.findViewById<LinearLayout>(R.id.main_BTN_add_babysitter)
        addButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_FRAME_container, AddBabysitterFragment())
                .addToBackStack(null)
                .commit()
        }

        val signOutButton = view.findViewById<LinearLayout>(R.id.main_BTN_sign_out)
        signOutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            activity?.finish()
        }

        loadBabysitters()
        return view
    }

    private fun loadBabysitters() {
        FirebaseFirestore.getInstance().collection("babysitters")
            .get()
            .addOnSuccessListener { result ->
                babysitters.clear()
                for (document in result) {
                    val babysitter = document.toObject(Babysitter::class.java)

                    babysitter.averageRating = (document.getDouble("averageRating") ?: 0.0).toFloat()
                    babysitter.numberOfRatings = (document.getLong("numberOfRatings") ?: 0L).toInt()

                    val reviewsList = document.get("reviews") as? List<Map<String, Any>>
                    babysitter.reviews = reviewsList?.map {
                        Review(
                            reviewerId = it["reviewerId"] as? String ?: "",
                            reviewerName = it["reviewerName"] as? String ?: "",
                            rating = (it["rating"] as? Number)?.toFloat() ?: 0f,
                            comment = it["comment"] as? String ?: ""
                        )
                    } ?: emptyList()

                    babysitters.add(babysitter)
                }
                adapter.updateList(babysitters)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load babysitters", Toast.LENGTH_SHORT).show()
            }
    }

}
