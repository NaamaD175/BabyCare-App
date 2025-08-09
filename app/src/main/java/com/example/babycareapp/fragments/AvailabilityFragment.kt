package com.example.babycareapp.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.babycareapp.R
import com.example.babycareapp.models.Babysitter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AvailabilityFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var saveButton: Button
    private lateinit var titleText: TextView
    private lateinit var babysitter: Babysitter
    private lateinit var availabilityListContainer: LinearLayout
    private val availabilityMap = mutableMapOf<String, String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var isOwner = false //If the user is someone who upload some babysitter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        babysitter = requireArguments().getSerializable("babysitter") as Babysitter
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        isOwner = (babysitter.uploaderId == currentUid)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_availability, container, false)

        calendarView = view.findViewById(R.id.availability_calendar)
        saveButton = view.findViewById(R.id.availability_save_button)
        titleText = view.findViewById(R.id.availability_title)
        availabilityListContainer = view.findViewById(R.id.availability_list_container)
        //Save button - shown only to owner
        saveButton.visibility = if (isOwner) View.VISIBLE else View.GONE
        saveButton.setOnClickListener {
            saveAvailability()
        }
        //Calender view - shows dates and availability
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            val existingNote = availabilityMap[date]

            showSingleDateAvailability(date)
            //If owner - can add availability
            if (isOwner) {
                val input = EditText(requireContext())
                input.hint = "Add availability details"
                input.setText(existingNote ?: "")

                AlertDialog.Builder(requireContext())
                    .setTitle("Availability for $date")
                    .setView(input)
                    .setPositiveButton("Save") { _, _ ->
                        val note = input.text.toString().trim()
                        if (note.isNotEmpty()) {
                            availabilityMap[date] = note
                            showSingleDateAvailability(date)
                            Toast.makeText(requireContext(), "Saved: $note", Toast.LENGTH_SHORT).show()
                        } else {
                            availabilityMap.remove(date)
                            showSingleDateAvailability(date)
                            Toast.makeText(requireContext(), "Removed availability for $date", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            //Else - can show the availability of the babysitters
            } else {
                if (existingNote != null) {
                    Toast.makeText(requireContext(), "Available on $date:\n$existingNote", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Not available on $date", Toast.LENGTH_SHORT).show()
                }
            }
        }

        loadAvailability()
        return view
    }

    //Load the availability from the babysitter - from firestore database
    private fun loadAvailability() {
        FirebaseFirestore.getInstance().collection("babysitters")
            .whereEqualTo("uploaderId", babysitter.uploaderId)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                if (doc != null) {
                    val map = doc.get("availabilityMap") as? Map<String, String> ?: emptyMap()
                    availabilityMap.clear()
                    availabilityMap.putAll(map)
                }
            }
    }

    private fun showSingleDateAvailability(date: String) {
        availabilityListContainer.removeAllViews()

        val note = availabilityMap[date]
        val textView = TextView(requireContext())
        textView.setPadding(8, 8, 8, 8)
        textView.textSize = 16f

        if (note != null) {
            textView.text = "$date – $note"
        } else {
            textView.text = "No availability for $date"
        }

        availabilityListContainer.addView(textView)
    }

    //Save and upload the availability to firestore database
    private fun saveAvailability() {
        FirebaseFirestore.getInstance().collection("babysitters")
            .whereEqualTo("uploaderId", babysitter.uploaderId)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                doc?.reference?.update("availabilityMap", availabilityMap)
                    ?.addOnSuccessListener {
                        Toast.makeText(requireContext(), "Availability saved!", Toast.LENGTH_SHORT).show()
                    }
                    ?.addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to save.", Toast.LENGTH_SHORT).show()
                    }
            }
    }
    //Creates a new instance of AvailabilityFragment with a Babysitter object
    companion object {
        fun newInstance(babysitter: Babysitter): AvailabilityFragment {
            val fragment = AvailabilityFragment()
            val args = Bundle()
            args.putSerializable("babysitter", babysitter)
            fragment.arguments = args
            return fragment
        }
    }
}
