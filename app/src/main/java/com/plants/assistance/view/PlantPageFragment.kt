package com.plants.assistance.view

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.plants.assistance.R
import com.plants.assistance.database.AppDatabase
import com.plants.assistance.database.PlantProblemDao
import com.plants.assistance.model.PlantProblem
import com.squareup.picasso.Picasso
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class PlantPageFragment : Fragment(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var appDatabase: AppDatabase
    private lateinit var plantProblemDao: PlantProblemDao
    private lateinit var currentPlant: PlantProblem
    private lateinit var imageView: ImageView
    private lateinit var editTextPlantName: EditText
    private lateinit var editTextPlantDescription: EditText
    private lateinit var editTextDateStarted: EditText
    private lateinit var editTextAgeOfPlant: EditText
    private lateinit var editTextSuggestion: EditText
    private lateinit var editTextExpertName: EditText
    private lateinit var buttonSaveEdits: Button
    private lateinit var deleteButton: Button
    private var selectedImageUri: Uri? = null
    private var selectedLocation: LatLng? = null

    companion object {
        private const val REQUEST_CODE_GALLERY = 1
    }

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap
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
        appDatabase = Room.databaseBuilder(requireContext().applicationContext,
            AppDatabase::class.java, "plant_database").allowMainThreadQueries().build()
        plantProblemDao = appDatabase.plantProblemDao()
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

        editTextPlantName = rootView.findViewById(R.id.editTextPlantName)
        editTextPlantDescription = rootView.findViewById(R.id.editTextPlantDescription)
        editTextDateStarted = rootView.findViewById(R.id.editTextDateStarted)
        editTextAgeOfPlant = rootView.findViewById(R.id.editTextAgeOfPlant)
        editTextSuggestion = rootView.findViewById(R.id.editTextSuggestion)
        editTextExpertName = rootView.findViewById(R.id.editTextExpertName)
        buttonSaveEdits = rootView.findViewById(R.id.buttonSaveEdits)
        deleteButton = rootView.findViewById(R.id.deleteButton)
        imageView = rootView.findViewById(R.id.plantImage)

        imageView.setOnClickListener { openGallery() }

        buttonSaveEdits.setOnClickListener {
            val newPlantName = editTextPlantName.text.toString()
            val newDescription = editTextPlantDescription.text.toString()
            val newDateStarted = editTextDateStarted.text.toString()
            val newAgeOfPlant = editTextAgeOfPlant.text.toString()
            val newSuggestion = editTextSuggestion.text.toString()
            val newExpertName = editTextExpertName.text.toString()

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

    private fun openGallery() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireContext() as Activity, arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_GALLERY
            )
        } else {
            imagePickerLauncher.launch("image/*")
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        //here i am checking if the phone has granted permission to the app to access gallery
        if (requestCode == REQUEST_CODE_GALLERY) {
            imagePickerLauncher.launch("image/*")
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    private fun setCurrentPlant(bundle: Bundle) {
        val plantId = bundle.getString("plantId")
        val userEmail = bundle.getString("userEmail")
        val plantName = bundle.getString("plantName")
        val imageUrl = bundle.getString("imageUrl")
        selectedImageUri  =  Uri.parse(imageUrl)
        val description = bundle.getString("description")
        val latitude = bundle.getDouble("latitude")
        val longitude = bundle.getDouble("longitude")
        val dateStarted = bundle.getString("dateStarted")
        val ageOfPlant = bundle.getString("ageOfPlant")
        val suggestion = bundle.getString("suggestion")
        val expertName = bundle.getString("expertName")

        currentPlant = PlantProblem(plantId!!, userEmail!!,  plantName!!, imageUrl!!, description!!, latitude, longitude, dateStarted!!, ageOfPlant!!, suggestion, expertName)
        if (latitude != 0.0 && longitude != 0.0) {
            val plantPosition = LatLng(latitude, longitude)
            val marker = map.addMarker(MarkerOptions().position(plantPosition).title(plantName).snippet(plantId))

        }
        val isEdit = bundle.getBoolean("isEdit")
        if (!isEdit) {
            editTextPlantName.isEnabled = false
            editTextPlantDescription.isEnabled = false
            editTextDateStarted.isEnabled = false
            editTextAgeOfPlant.isEnabled = false
            editTextSuggestion.isEnabled = false
            editTextExpertName.isEnabled = false
            buttonSaveEdits.visibility = View.GONE
            deleteButton.visibility = View.GONE
            imageView.isEnabled = false
        }else{
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
