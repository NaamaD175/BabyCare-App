package com.example.babycareapp.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.babycareapp.R
import com.example.babycareapp.databinding.FragmentBabysitterDetailsBinding
import com.example.babycareapp.models.Babysitter
import com.example.babycareapp.models.Review
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class BabysitterDetailsFragment : Fragment() {

    private var babysitter: Babysitter? = null
    private var _binding: FragmentBabysitterDetailsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_BABYSITTER = "babysitter"

        fun newInstance(babysitter: Babysitter): BabysitterDetailsFragment {
            val fragment = BabysitterDetailsFragment()
            val bundle = Bundle()
            bundle.putSerializable(ARG_BABYSITTER, babysitter)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        babysitter = arguments?.getSerializable(ARG_BABYSITTER) as? Babysitter
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBabysitterDetailsBinding.inflate(inflater, container, false)

        babysitter?.let { b ->

            binding.detailsIMGBack.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_FRAME_container, HomeFragment())
                    .commit()
            }

            binding.detailsLBLName.text = b.name
            binding.detailsLBLAddress.text = b.address
            binding.detailsLBLPrice.text = "₪${b.price} per hour"
            binding.detailsLBLAbout.text = b.description
            Glide.with(requireContext()).load(b.imageUrl).into(binding.detailsIMGPhoto)

            val currentUid = FirebaseAuth.getInstance().currentUser?.uid

            binding.detailsRatingBar.rating = b.averageRating
            binding.detailsRatingBar.isEnabled = (currentUid != b.uploaderId)

            if (currentUid != b.uploaderId) {
                binding.detailsRatingBar.setOnRatingBarChangeListener { _, newRating, _ ->
                    saveRating(newRating)
                }
                binding.detailsBTNAddReview.visibility = View.VISIBLE
                binding.detailsBTNAddReview.setOnClickListener {
                    showReviewDialog()
                }
            } else {
                binding.detailsBTNAddReview.visibility = View.GONE
            }

            loadReviews()

            if (currentUid == b.uploaderId) {
                binding.detailsBTNDelete.visibility = View.VISIBLE
                binding.detailsBTNDelete.setOnClickListener {
                    deleteBabysitter(b)
                }
            } else {
                binding.detailsBTNDelete.visibility = View.GONE
            }

            if (b.uploaderId == currentUid) {
                binding.detailsBTNChat.visibility = View.GONE
            } else {
                binding.detailsBTNChat.visibility = View.VISIBLE
                binding.detailsBTNChat.setOnClickListener {
                    val chatFragment = ChatFragment.newInstance(b)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main_FRAME_container, chatFragment)
                        .addToBackStack(null)
                        .commit()
                }
            }


            binding.detailsBTNAvailability.text = if (currentUid == b.uploaderId) {
                "Change Availability"
            } else {
                "View Availability"
            }

            binding.detailsBTNAvailability.setOnClickListener {
                val availabilityFragment = AvailabilityFragment.newInstance(b)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_FRAME_container, availabilityFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        return binding.root
    }

    private fun loadReviews() {
        val container = binding.detailsReviewsContainer
        container.removeAllViews()

        val reviews = babysitter?.reviews ?: return
        if (reviews.isEmpty()) {
            val noReviews = TextView(requireContext())
            noReviews.text = "No reviews yet."
            container.addView(noReviews)
            return
        }

        for (review in reviews) {
            val textView = TextView(requireContext())
            textView.text = "⭐ ${review.rating} - ${review.reviewerName}: ${review.comment}"
            textView.setPadding(4, 4, 4, 4)
            container.addView(textView)
        }
    }

    private fun saveRating(newRating: Float) {
        val db = FirebaseFirestore.getInstance()
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("babysitters")
            .whereEqualTo("imageUrl", babysitter?.imageUrl)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull() ?: return@addOnSuccessListener
                val currentAverage = babysitter?.averageRating ?: 0f
                val newAverage = ((currentAverage + newRating) / 2)
                doc.reference.update("averageRating", newAverage)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Rating saved", Toast.LENGTH_SHORT).show()
                        binding.detailsRatingBar.rating = newAverage
                    }
            }
    }

    private fun showReviewDialog() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        val ratingBar = RatingBar(context).apply {
            numStars = 5
            stepSize = 1f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                width = 750
            }
        }


        val input = EditText(context).apply {
            hint = "Write your review"
        }

        layout.addView(ratingBar)
        layout.addView(input)

        AlertDialog.Builder(context)
            .setTitle("Leave a review")
            .setView(layout)
            .setPositiveButton("Submit") { _, _ ->
                val rating = ratingBar.rating
                val comment = input.text.toString().trim()
                val reviewerId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
                val reviewerName = FirebaseAuth.getInstance().currentUser?.email ?: "Anonymous"

                val review = Review(
                    reviewerId = reviewerId,
                    reviewerName = reviewerName,
                    rating = rating,
                    comment = comment
                )

                saveReview(review)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun saveReview(review: Review) {
        val db = FirebaseFirestore.getInstance()
        db.collection("babysitters")
            .whereEqualTo("imageUrl", babysitter?.imageUrl)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull() ?: return@addOnSuccessListener

                val reviewsList = babysitter?.reviews?.toMutableList() ?: mutableListOf()
                reviewsList.add(review)

                val totalRatings = reviewsList.size
                val totalScore = reviewsList.sumOf { it.rating.toDouble() }
                val newAverage = (totalScore / totalRatings).toFloat()

                doc.reference.update(
                    mapOf(
                        "reviews" to reviewsList,
                        "averageRating" to newAverage,
                        "numberOfRatings" to totalRatings
                    )
                ).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Review saved", Toast.LENGTH_SHORT).show()
                    babysitter?.reviews = reviewsList
                    babysitter?.averageRating = newAverage
                    babysitter?.numberOfRatings = totalRatings
                    loadReviews()
                    binding.detailsRatingBar.rating = newAverage
                }
            }
    }


    private fun deleteBabysitter(babysitter: Babysitter) {
        val db = FirebaseFirestore.getInstance()
        db.collection("babysitters")
            .whereEqualTo("imageUrl", babysitter.imageUrl)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    db.collection("babysitters").document(document.id).delete()
                        .addOnSuccessListener {
                            FirebaseStorage.getInstance()
                                .getReferenceFromUrl(babysitter.imageUrl)
                                .delete()

                            Toast.makeText(requireContext(), "Babysitter deleted", Toast.LENGTH_SHORT).show()

                            parentFragmentManager.beginTransaction()
                                .replace(R.id.main_FRAME_container, HomeFragment())
                                .commit()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to delete", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}