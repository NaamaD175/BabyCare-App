package com.example.babycareapp.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.babycareapp.adapters.BabysitterAdapter
import com.example.babycareapp.databinding.FragmentSearchBinding
import com.example.babycareapp.models.Babysitter
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.common.api.Status
import com.example.babycareapp.R

class SearchFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleMap: GoogleMap
    private val babysittersList = mutableListOf<Babysitter>()
    private lateinit var adapter: BabysitterAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private var userLocation: Location? = null

    private var selectedLatLng: LatLng? = null
    private var selectedAddress: String = ""

    private var selectedBabysitter: Babysitter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)

        //Initialize Places API if not already initialized
        if (!Places.isInitialized()) {
            Places.initialize(requireContext().applicationContext, "AIzaSyBn-c3eYBrAEKXbuCc1ItIxTb86sAFHNR4")
        }
        //Set up Google Map
        val mapFragment = childFragmentManager.findFragmentById(R.id.search_MAP_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        adapter = BabysitterAdapter(babysittersList) { selectedBabysitter ->
            Toast.makeText(requireContext(), "Clicked: ${selectedBabysitter.name}", Toast.LENGTH_SHORT).show()
        }
        binding.searchRVBabysitters.layoutManager = LinearLayoutManager(requireContext())
        binding.searchRVBabysitters.adapter = adapter

        val autocompleteFragment = childFragmentManager.findFragmentById(R.id.search_autocomplete_fragment) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ADDRESS, Place.Field.LAT_LNG))
        autocompleteFragment.setHint("Enter address")

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                selectedAddress = place.address ?: ""
                selectedLatLng = place.latLng

                //Move map to selected location
                selectedLatLng?.let {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 12f))
                }
                //Apply filters based on address and price
                val maxPrice = binding.searchEDTPrice.text.toString().toDoubleOrNull()
                applyFilter(selectedAddress, selectedLatLng, maxPrice)
            }

            override fun onError(status: Status) {
                Toast.makeText(requireContext(), "Failed to get address: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
        })
        //Handle filter button click
        binding.searchBTNFilter.setOnClickListener {
            val maxPrice = binding.searchEDTPrice.text.toString().toDoubleOrNull()
            applyFilter(selectedAddress, selectedLatLng, maxPrice)
        }
        //Handle bottom sheet click to open babysitter details
        binding.bottomSheet.setOnClickListener {
            selectedBabysitter?.let {
                val fragment = BabysitterDetailsFragment.newInstance(it)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_FRAME_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        return binding.root
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    userLocation = it
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 12f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001)
        }

        loadBabysitters()

        googleMap.setOnMarkerClickListener { marker ->
            val babysitter = marker.tag as? Babysitter
            babysitter?.let {
                selectedBabysitter = it
                showBottomSheet(it)
                marker.showInfoWindow()
            }
            true
        }
    }

    private fun loadBabysitters() {
        firestore.collection("babysitters").get().addOnSuccessListener { result ->
            babysittersList.clear()
            googleMap.clear()
            for (doc in result) {
                val babysitter = doc.toObject(Babysitter::class.java)
                babysittersList.add(babysitter)
                if (babysitter.latitude != null && babysitter.longitude != null) {
                    val position = LatLng(babysitter.latitude, babysitter.longitude)
                    val marker = googleMap.addMarker(MarkerOptions().position(position).title(babysitter.name))
                    marker?.tag = babysitter
                }
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun applyFilter(address: String, filterLatLng: LatLng?, maxPrice: Double?) {
        val filtered = babysittersList.filter { babysitter ->
            val matchAddress = address.isEmpty() || babysitter.address.contains(address, ignoreCase = true)
            val matchPrice = maxPrice == null || babysitter.price <= maxPrice

            val isNearby = filterLatLng?.let { filterLoc ->
                val babysitterLoc = Location("").apply {
                    latitude = babysitter.latitude ?: 0.0
                    longitude = babysitter.longitude ?: 0.0
                }
                val filterLocation = Location("").apply {
                    latitude = filterLoc.latitude
                    longitude = filterLoc.longitude
                }
                filterLocation.distanceTo(babysitterLoc) / 1000 <= 10
            } ?: true

            matchAddress && matchPrice && isNearby
        }

        googleMap.clear()
        filtered.forEach { babysitter ->
            if (babysitter.latitude != null && babysitter.longitude != null) {
                val position = LatLng(babysitter.latitude, babysitter.longitude)
                val marker = googleMap.addMarker(MarkerOptions().position(position).title(babysitter.name))
                marker?.tag = babysitter
            }
        }

        adapter.updateList(filtered)
    }

    private fun showBottomSheet(babysitter: Babysitter) {
        binding.bottomSheet.visibility = View.VISIBLE
        binding.bottomSheetLBLName.text = babysitter.name
        if (babysitter.imageUrl.isNotEmpty()) {
            Glide.with(this).load(babysitter.imageUrl).into(binding.bottomSheetIMGPhoto)
        } else {
            binding.bottomSheetIMGPhoto.setImageResource(R.drawable.ic_person_placeholder) // תחליף בתמונה שלך
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
