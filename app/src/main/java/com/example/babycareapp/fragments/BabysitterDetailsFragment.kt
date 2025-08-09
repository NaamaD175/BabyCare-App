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
        private const val RATING_STARS = 5 //Number of start in rating dialog

        //Method to create a new instance with a babysitter object
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

            //Back button - navigates to HomeFragment
            binding.detailsIMGBack.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_FRAME_container, HomeFragment())
                    .commit()
            }

            //Fill babysitter details in the UI
            binding.detailsLBLName.text = b.name
            binding.detailsLBLAddress.text = b.address
            binding.detailsLBLPrice.text = "₪${b.price} per hour"
            binding.detailsLBLAbout.text = b.description
            Glide.with(requireContext()).load(b.imageUrl).into(binding.detailsIMGPhoto)

            val currentUid = FirebaseAuth.getInstance().currentUser?.uid

            //If the current user is the uploader - hide "Add Review" button
            if (currentUid == b.uploaderId) {
                binding.detailsBTNAddReview.visibility = View.GONE
            } else {
                //If not the uploader - show "Add Review" button and open review dialog
                binding.detailsBTNAddReview.visibility = View.VISIBLE
                binding.detailsBTNAddReview.setOnClickListener { showReviewDialog() }
            }
            //Load existing reviews for this babysitter
            loadReviews()

            //If uploader - show delete button
            if (currentUid == b.uploaderId) {
                binding.detailsBTNDelete.visibility = View.VISIBLE
                binding.detailsBTNDelete.setOnClickListener { deleteBabysitter(b) }
            } else {
                binding.detailsBTNDelete.visibility = View.GONE
            }
            //If uploader - hide chat button (can't chat with yourself)
            if (b.uploaderId == currentUid) {
                binding.detailsBTNChat.visibility = View.GONE
            } else {
                //Show chat button and open chat fragment
                binding.detailsBTNChat.visibility = View.VISIBLE
                binding.detailsBTNChat.setOnClickListener {
                    val chatFragment = ChatFragment.newInstance(b)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main_FRAME_container, chatFragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
            //Set text for availability button based on user role
            binding.detailsBTNAvailability.text = if (currentUid == b.uploaderId) {
                "Change Availability"
            } else {
                "View Availability"
            }
            //Open availability fragment when clicked
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
    //Load and display reviews
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

    //Save new review and update average rating
    private fun saveReview(review: Review) {
        val db = FirebaseFirestore.getInstance()
        db.collection("babysitters")
            .whereEqualTo("imageUrl", babysitter?.imageUrl) //Find the babysitter by image URL
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull() ?: return@addOnSuccessListener
                //Add the new review to the list
                val reviewsList = babysitter?.reviews?.toMutableList() ?: mutableListOf()
                reviewsList.add(review)
                //Calculate new average rating
                val totalRatings = reviewsList.size
                val totalScore = reviewsList.sumOf { it.rating.toDouble() }
                val newAverage = (totalScore / totalRatings).toFloat()
                //Update Firestore document
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
                    loadReviews()//Refresh UI
                }
            }
    }
    //Show dialog for adding a review
    private fun showReviewDialog() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }
        //Rating bar for selecting stars
        val ratingBar = RatingBar(context).apply {
            numStars = RATING_STARS
            stepSize = 1f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { width = 750 }
        }
        //Text input for review comment
        val input = EditText(context).apply {
            hint = "Write your review"
        }

        layout.addView(ratingBar)
        layout.addView(input)
        //AlertDialog to submit or cancel review
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

                saveReview(review)//Save to Firestore
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    //Delete babysitter from Firestore and Firebase Storage
    private fun deleteBabysitter(babysitter: Babysitter) {
        val db = FirebaseFirestore.getInstance()
        db.collection("babysitters")
            .whereEqualTo("imageUrl", babysitter.imageUrl)//Find babysitter by image URL
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    db.collection("babysitters").document(document.id).delete()
                        .addOnSuccessListener {
                            //Delete image from storage
                            FirebaseStorage.getInstance()
                                .getReferenceFromUrl(babysitter.imageUrl)
                                .delete()

                            Toast.makeText(requireContext(), "Babysitter deleted", Toast.LENGTH_SHORT).show()
                            //Go back to HomeFragment
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
    //Avoid memory leaks
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
