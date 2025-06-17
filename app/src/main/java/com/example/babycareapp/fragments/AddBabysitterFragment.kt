package com.example.babycareapp.fragments

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.example.babycareapp.R
import com.example.babycareapp.models.Babysitter
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AddBabysitterFragment : Fragment() {

    private lateinit var photoImageView: ImageView
    private lateinit var nameEditText: EditText
    private lateinit var priceEditText: EditText
    private lateinit var aboutEditText: EditText
    private lateinit var submitButton: Button

    private var selectedImageUri: Uri? = null
    private var selectedAddress: String = ""
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            selectedImageUri = result.data!!.data
            photoImageView.setImageURI(selectedImageUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_babysitter, container, false)

        if (!Places.isInitialized()) {
            Places.initialize(requireContext().applicationContext, "AIzaSyBn-c3eYBrAEKXbuCc1ItIxTb86sAFHNR4")
        }

        val backButton = view.findViewById<ImageButton>(R.id.add_IMG_back)
        backButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_FRAME_container, HomeFragment())
                .commit()
        }

        photoImageView = view.findViewById(R.id.add_IMG_photo)
        nameEditText = view.findViewById(R.id.add_EDT_name)
        priceEditText = view.findViewById(R.id.add_EDT_price)
        aboutEditText = view.findViewById(R.id.add_EDT_about)
        submitButton = view.findViewById(R.id.add_BTN_submit)

        photoImageView.setOnClickListener { openGallery() }
        submitButton.setOnClickListener { uploadBabysitter() }

        val autocompleteFragment =
            childFragmentManager.findFragmentById(R.id.autocomplete_fragment) as com.google.android.libraries.places.widget.AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(listOf(Place.Field.ADDRESS, Place.Field.LAT_LNG))
        autocompleteFragment.setHint("Enter address")

        autocompleteFragment.setOnPlaceSelectedListener(object : com.google.android.libraries.places.widget.listener.PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                selectedAddress = place.address ?: ""
                selectedLatitude = place.latLng?.latitude
                selectedLongitude = place.latLng?.longitude
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Toast.makeText(requireContext(), "Failed to get address", Toast.LENGTH_SHORT).show()
            }
        })

        return view
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun uploadBabysitter() {
        val name = nameEditText.text.toString().trim()
        val priceText = priceEditText.text.toString().trim()
        val about = aboutEditText.text.toString().trim()

        if (name.isEmpty() || selectedAddress.isEmpty() || priceText.isEmpty() || about.isEmpty() || selectedImageUri == null || selectedLatitude == null || selectedLongitude == null) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceText.toDoubleOrNull()
        if (price == null) {
            Toast.makeText(requireContext(), "Invalid price", Toast.LENGTH_SHORT).show()
            return
        }

        val imageRef = FirebaseStorage.getInstance().reference
            .child("babysitters_images/${UUID.randomUUID()}.jpg")

        imageRef.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener

                    val babysitter = Babysitter(
                        name = name,
                        address = selectedAddress,
                        price = price,
                        description = about,
                        imageUrl = uri.toString(),
                        uploaderId = uid,
                        latitude = selectedLatitude,
                        longitude = selectedLongitude
                    )

                    FirebaseFirestore.getInstance().collection("babysitters")
                        .add(babysitter)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Babysitter added!", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.main_FRAME_container, HomeFragment())
                                .commit()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to save babysitter", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
    }
}