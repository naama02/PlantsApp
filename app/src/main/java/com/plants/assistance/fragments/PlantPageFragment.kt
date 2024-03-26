package com.plants.assistance.fragments

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.plants.assistance.R
import com.plants.assistance.db.MyDatabse
import com.plants.assistance.db.PlantProblemDao
import com.plants.assistance.model.PlantProblem
import com.squareup.picasso.Picasso
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlantPageFragment : Fragment(), OnMapReadyCallback , DatePickerDialog.OnDateSetListener {
    private lateinit var map: GoogleMap
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var myDB: MyDatabse
    private lateinit var plantProblemDao: PlantProblemDao
    private lateinit var currentPlant: PlantProblem
    private lateinit var imageView: ImageView
    private lateinit var plantNameEditText: EditText
    private lateinit var textPlantDescription: EditText
    private lateinit var textDateStarted: EditText
    private lateinit var textAgeOfPlant: EditText
    private lateinit var textSuggestion: EditText
    private lateinit var suggestionTextView: TextView
    private lateinit var saveEdits: Button
    private lateinit var deleteButton: Button
    private var selectedImageUri: Uri? = null
    private var selectedLocation: LatLng? = null
    private lateinit var mAuth: FirebaseAuth
    companion object {
        private const val REQUEST_CODE_GALLERY = 1
    }

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap
        val bundle = arguments
        bundle?.let {
            setCurrentPlant(bundle)
            plantNameEditText.setText(currentPlant.title)
            textAgeOfPlant.setText(currentPlant.ageOfPlant)
            suggestionTextView.setText(currentPlant.suggestion)
            suggestionTextView.setText(currentPlant.expertName)
            textDateStarted.setText(currentPlant.dateStarted)
            textPlantDescription.setText(currentPlant.description)
            Picasso.get().load(currentPlant.imageUrl).into(imageView)
            val hotelPosition = LatLng(currentPlant.latitude, currentPlant.longitude)
            val marker = map.addMarker(MarkerOptions().position(hotelPosition).title(currentPlant.title).snippet(currentPlant.key))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(hotelPosition, 15f))
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { result ->
        result?.let {
            try {
                Picasso.get().load(it).into(imageView)
                selectedImageUri = it
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseFirestore = FirebaseFirestore.getInstance()
        myDB = Room.databaseBuilder(requireContext().applicationContext,
            MyDatabse::class.java, "plant_database").allowMainThreadQueries().build()
        plantProblemDao = myDB.plantProblemDao()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val callback1 = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        }
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapsFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(callback)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback1)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_plant_page, container, false)
        mAuth = FirebaseAuth.getInstance()
        plantNameEditText = rootView.findViewById(R.id.name_edittext)
        textPlantDescription = rootView.findViewById(R.id.description_edittext)
        textDateStarted = rootView.findViewById(R.id.date_edittext)
        textAgeOfPlant = rootView.findViewById(R.id.age_edittext)
        textSuggestion = rootView.findViewById(R.id.suggestion_edittext)
        suggestionTextView = rootView.findViewById(R.id.suggestion_textview)
        saveEdits = rootView.findViewById(R.id.save_button)
        deleteButton = rootView.findViewById(R.id.delete_button)
        imageView = rootView.findViewById(R.id.plantImage)

        imageView.setOnClickListener { openGallery() }
        textDateStarted.setOnClickListener {
            showDatePickerDialog()
        }
        saveEdits.setOnClickListener {
            val newPlantName = plantNameEditText.text.toString()
            val newDescription = textPlantDescription.text.toString()
            val newDateStarted = textDateStarted.text.toString()
            val newAgeOfPlant = textAgeOfPlant.text.toString()
            val user = mAuth.currentUser
            var newExpertName = "";
            user?.let {
                newExpertName = user.displayName.toString()
            }
            val newSuggestion = suggestionTextView.text.toString() + "\n" + newExpertName+":" +textSuggestion.text.toString()
            selectedImageUri?.let { uri ->
                val storageRef = FirebaseStorage.getInstance().getReference().child("plant_images/${newPlantName}")
                storageRef.putFile(uri)
                    .addOnSuccessListener { taskSnapshot ->
                        storageRef.downloadUrl.addOnSuccessListener { imageUrl ->
                            currentPlant.imageUrl = imageUrl.toString()
                            firebaseFirestore.collection("Plants").document(currentPlant.key.toString())
                                .update("name", newPlantName, "description", newDescription,"imageUrl", imageUrl.toString(),"latitude", selectedLocation!!.latitude,"longitude", selectedLocation!!.longitude,"dateStarted", newDateStarted, "ageOfPlant", newAgeOfPlant, "suggestion", newSuggestion, "expertName", newExpertName)

                                .addOnSuccessListener {
                                    currentPlant.apply {
                                        title = newPlantName
                                        description = newDescription
                                        dateStarted = newDateStarted
                                        ageOfPlant = newAgeOfPlant
                                        suggestion = newSuggestion
                                        expertName = newExpertName
                                    }
                                    plantProblemDao.update(currentPlant)
                                    Toast.makeText(context, "Plant updated successfully", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { Toast.makeText(context, "Error updating plant", Toast.LENGTH_SHORT).show() }

                            Toast.makeText(context, "Image updated successfully", Toast.LENGTH_SHORT).show()

                        }
                    }
                    .addOnFailureListener { Toast.makeText(context, "Error uploading image", Toast.LENGTH_SHORT).show() }
            }

        }

        deleteButton.setOnClickListener {
            currentPlant.key?.let { plantKey ->
                firebaseFirestore.collection("Plants").document(plantKey)
                    .delete()
                    .addOnSuccessListener {
                        plantProblemDao.delete(currentPlant)
                        Toast.makeText(context, "Plant deleted successfully", Toast.LENGTH_SHORT).show()
                        activity?.onBackPressed()
                    }
                    .addOnFailureListener { Toast.makeText(context, "Error deleting plant", Toast.LENGTH_SHORT).show() }
            }
        }

        return rootView
    }
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), this, year, month, dayOfMonth)
        datePickerDialog.show()
    }
    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }.time)
        textDateStarted.setText(selectedDate)
    }
    private fun openGallery() {
        imagePickerLauncher.launch("image/*")
    }

    private fun setCurrentPlant(bundle: Bundle) {
        val plantId = bundle.getString("plantId")?: ""
        val userEmail = bundle.getString("userEmail")?: ""
        val plantName = bundle.getString("plantName")?: ""
        val imageUrl = bundle.getString("imageUrl")?: ""
        selectedImageUri  =  Uri.parse(imageUrl)
        val description = bundle.getString("description")?: ""
        val latitude = bundle.getDouble("latitude")
        val longitude = bundle.getDouble("longitude")
        val dateStarted = bundle.getString("dateStarted")?: ""
        val ageOfPlant = bundle.getString("ageOfPlant")?: ""
        val suggestion = bundle.getString("suggestion")?: ""
        val expertName = bundle.getString("expertName") ?: ""

        currentPlant = PlantProblem(plantId!!, userEmail!!,  imageUrl!!, plantName!!, description!!, latitude, longitude, dateStarted!!, ageOfPlant!!, suggestion, expertName)
        if (latitude != 0.0 && longitude != 0.0) {
            val plantPosition = LatLng(latitude, longitude)
            val marker = map.addMarker(MarkerOptions().position(plantPosition).title(plantName).snippet(plantId))

        }
        val isEdit = bundle.getBoolean("isEdit")
        if (!isEdit) {
            plantNameEditText.isEnabled = false
            textPlantDescription.isEnabled = false
            textDateStarted.isEnabled = false
            textAgeOfPlant.isEnabled = false
            textSuggestion.isEnabled = false
            saveEdits.visibility = View.GONE
            deleteButton.visibility = View.GONE
            imageView.isEnabled = false
        }else{
            suggestionTextView.visibility = View.GONE
            map.setOnMapClickListener { latLng ->
                selectedLocation = latLng
                map.clear()
                map.addMarker(MarkerOptions().position(latLng))
            }
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        TODO("Not yet implemented")
    }

}
