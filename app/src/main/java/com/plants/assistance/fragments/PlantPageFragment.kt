package com.plants.assistance.fragments

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import androidx.navigation.fragment.NavHostFragment
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
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.plants.assistance.activities.LoginActivity.Companion.getUserType
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
    private lateinit var map_edittext: EditText
    private lateinit var textPlantDescription: EditText
    private lateinit var textDateStarted: EditText
    private lateinit var textAgeOfPlant: EditText
    private lateinit var suggestionEditText: EditText
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
            map_edittext.setText(currentPlant.address)
            suggestionTextView.setText(currentPlant.suggestion)
            textDateStarted.setText(currentPlant.dateStarted)
            textPlantDescription.setText(currentPlant.description)

            var userName = "";
            val user = mAuth.currentUser
            user?.let {
                userName = user.displayName.toString()
            }
            val suggestions = currentPlant.suggestion!!.split("\n")
            for (sugestion in suggestions) {
                if (sugestion.startsWith("$userName:")) {
                    suggestionEditText.setText(sugestion.split(':')[1])
                }
            }

            Picasso.get().load(currentPlant.imageUrl).into(imageView)
            val position = LatLng(currentPlant.latitude, currentPlant.longitude)
            val marker = map.addMarker(MarkerOptions().position(position).title(currentPlant.title).snippet(currentPlant.key))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
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
    override fun onResume() {
        super.onResume()
        hideBottomNavigationBar()
    }

    override fun onPause() {
        super.onPause()
        showBottomNavigationBar()
    }

    private fun hideBottomNavigationBar() {
        val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.visibility = View.GONE
        val bottomNavigationExpertView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation_expert)
        bottomNavigationExpertView.visibility = View.GONE
    }

    private fun showBottomNavigationBar() {
        if( getUserType().equals("Regular") ) {
            val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNavigationView.visibility = View.VISIBLE
        }else{
            val bottomNavigationExpertView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation_expert)
            bottomNavigationExpertView.visibility = View.VISIBLE
        }
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
        suggestionEditText = rootView.findViewById(R.id.suggestion_edittext)
        suggestionTextView = rootView.findViewById(R.id.suggestion_textview)
        saveEdits = rootView.findViewById(R.id.save_button)
        deleteButton = rootView.findViewById(R.id.delete_button)
        imageView = rootView.findViewById(R.id.plantImage)
        map_edittext = rootView.findViewById(R.id.map_edittext)
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
            var userName = "";
            user?.let {
                userName = user.displayName.toString()
            }
            val newAddress = map_edittext.text.toString()
            val regex = Regex("$userName:.*")
            val existingText = suggestionTextView.text.toString()
            var newSuggestion  = suggestionEditText.text.toString()
            if (newSuggestion.isEmpty()){
                newSuggestion = existingText
            }else{
                newSuggestion = if (existingText.contains(regex)) {
                    existingText.replace(regex, "$userName: $newSuggestion")
                } else {
                    "$existingText\n$userName: $newSuggestion"
                }
            }
            if(selectedImageUri == null){
                val latitude1 = if (selectedLocation != null) selectedLocation!!.latitude else currentPlant.latitude
                val longitude1 = if (selectedLocation != null) selectedLocation!!.longitude else currentPlant.longitude
                Log.e("-------------",currentPlant.key.toString());
                firebaseFirestore.collection("Plants").document(currentPlant.key.toString())
                    .update("title",  newPlantName,
                        "description", newDescription,
                        "imageUrl", currentPlant.imageUrl,
                        "latitude", latitude1,
                        "longitude", longitude1,
                        "dateStarted", newDateStarted,
                        "ageOfPlant", newAgeOfPlant,
                        "suggestion",  newSuggestion,
                        "address", newAddress)


                .addOnSuccessListener {
                    currentPlant.apply {
                        title = newPlantName
                        description = newDescription
                        dateStarted = newDateStarted
                        ageOfPlant = newAgeOfPlant
                        suggestion = newSuggestion
                        address = newAddress
                    }
                    plantProblemDao.update(currentPlant)
                    suggestionTextView.setText(currentPlant.suggestion)
                    Toast.makeText(context, "Plant updated successfully", Toast.LENGTH_SHORT).show()
                    NavHostFragment.findNavController(this).popBackStack()
                }
                .addOnFailureListener { Toast.makeText(context, "Error updating plant", Toast.LENGTH_SHORT).show() }

            }else{
                selectedImageUri?.let { uri ->
                    val storageRef = FirebaseStorage.getInstance().getReference().child("plant_images/${newPlantName}")
                    storageRef.putFile(uri)
                        .addOnSuccessListener { taskSnapshot ->
                            storageRef.downloadUrl.addOnSuccessListener { url ->
                                currentPlant.imageUrl = url.toString()
                                val latitude1 = if (selectedLocation != null) selectedLocation!!.latitude else currentPlant.latitude
                                val longitude1 = if (selectedLocation != null) selectedLocation!!.longitude else currentPlant.longitude
                                firebaseFirestore.collection("Plants").document(currentPlant.key.toString())
                                    .update(
                                        "title",  newPlantName,
                                        "description", newDescription,
                                        "imageUrl", url.toString(),
                                        "latitude", latitude1,
                                        "longitude", longitude1,
                                        "dateStarted", newDateStarted,
                                        "ageOfPlant", newAgeOfPlant,
                                        "suggestion",  newSuggestion,
                                        "address", newAddress)

                                    .addOnSuccessListener {
                                        currentPlant.apply {
                                            title = newPlantName
                                            description = newDescription
                                            imageUrl = url.toString()
                                            dateStarted = newDateStarted
                                            ageOfPlant = newAgeOfPlant
                                            suggestion = newSuggestion
                                            address = newAddress
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
    private fun selectAddress() {
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
            .build(requireContext())
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }
    private val AUTOCOMPLETE_REQUEST_CODE = 1
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val place = Autocomplete.getPlaceFromIntent(data!!)
                val address = place.address
                map_edittext.setText(address)
                val latitude = place.latLng?.latitude
                val longitude = place.latLng?.longitude
                if (latitude != null && longitude != null) {
                    selectedLocation = LatLng(latitude, longitude)
                    map.clear()
                    map.addMarker(MarkerOptions().position(selectedLocation!!).title(address))
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation!!, 15f))
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                val status = Autocomplete.getStatusFromIntent(data!!)
            }
        }
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
        val description = bundle.getString("description")?: ""
        val latitude = bundle.getDouble("latitude")
        val longitude = bundle.getDouble("longitude")
        val dateStarted = bundle.getString("dateStarted")?: ""
        val ageOfPlant = bundle.getString("ageOfPlant")?: ""
        val suggestion = bundle.getString("suggestion")?: ""
        val address = bundle.getString("address") ?: ""

        currentPlant = PlantProblem(plantId!!, userEmail!!,  imageUrl!!, plantName!!, description!!, latitude, longitude, dateStarted!!, ageOfPlant!!, suggestion, address)
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
            deleteButton.visibility = View.GONE
            imageView.isEnabled = false
        }else{
            map_edittext.setOnClickListener { selectAddress() }
            map.setOnMapClickListener { latLng ->
                selectedLocation = latLng
                map.clear()
                map.addMarker(MarkerOptions().position(latLng))
            }
        }
        if (getUserType().equals("Regular"))
            suggestionEditText.visibility = View.GONE
    }

    override fun onMapReady(p0: GoogleMap) {
        TODO("Not yet implemented")
    }

}
