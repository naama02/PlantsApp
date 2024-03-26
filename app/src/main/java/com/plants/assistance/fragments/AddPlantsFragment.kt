package com.plants.assistance.fragments

import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.room.RoomDatabase
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.plants.assistance.R
import com.plants.assistance.db.MyDatabse
import com.plants.assistance.model.PlantProblem
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddPlantsFragment : Fragment(), OnMapReadyCallback , DatePickerDialog.OnDateSetListener{
    private lateinit var problemTitleEditText: EditText
    private lateinit var problemDescriptionEditText: EditText
    private lateinit var problemImageView: ImageView
    private lateinit var dateStartedEditText: EditText
    private lateinit var ageOfPlantEditText: EditText
    private lateinit var map_edittext: EditText
    private lateinit var addProblemButton: Button
    private lateinit var cancelButton: Button
    private lateinit var map: GoogleMap
    private var selectedImageUri: Uri? = null
    private var selectedLocation: LatLng? = null

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var localDb: MyDatabse
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { result ->
        if (result != null) {
            try {
                result?.let {
                    Picasso.get().load(it).into(problemImageView, object : com.squareup.picasso.Callback {
                        override fun onSuccess() {
                            selectedImageUri = it
                        }
                        override fun onError(e: Exception?) {
                            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
                            e?.printStackTrace()
                        }
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val AUTOCOMPLETE_REQUEST_CODE = 1

    private fun selectAddress() {
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
            .build(requireContext())
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        localDb = MyDatabse.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_add_plant, container, false)
        initializeViews(rootView)
        setupMapFragment()
        setupListeners()
        return rootView
    }

    private fun initializeViews(rootView: View) {
        problemTitleEditText = rootView.findViewById(R.id.plantname_textview)
        problemDescriptionEditText = rootView.findViewById(R.id.problemdescription_textview)
        problemImageView = rootView.findViewById(R.id.problemImage)
        dateStartedEditText = rootView.findViewById(R.id.date_edittext)
        ageOfPlantEditText = rootView.findViewById(R.id.age_edittext)
        map_edittext = rootView.findViewById(R.id.map_edittext)
        addProblemButton = rootView.findViewById(R.id.add_button)
        cancelButton = rootView.findViewById(R.id.cancel_button)

    }

    private fun setupMapFragment() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapsFragment) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    private fun setupListeners() {
        problemImageView.setOnClickListener { selectImage() }
        addProblemButton.setOnClickListener { uploadProblemData() }
        cancelButton.setOnClickListener { NavHostFragment.findNavController(this).popBackStack() }
        map_edittext.setOnClickListener { selectAddress() }
        dateStartedEditText.setOnClickListener {   showDatePickerDialog()  }
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
        dateStartedEditText.setText(selectedDate)
    }
    private fun selectImage() {
        imagePickerLauncher.launch("image/*")
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
    }

    private fun uploadProblemData() {


        val problemTitle = problemTitleEditText.text.toString().trim()
        val problemDescription = problemDescriptionEditText.text.toString().trim()
        val dateStarted = dateStartedEditText.text.toString().trim()
        val ageOfPlant = ageOfPlantEditText.text.toString().trim()

        if (problemTitle.isEmpty() || problemDescription.isEmpty() || selectedImageUri == null || selectedLocation == null || dateStarted.isEmpty() || ageOfPlant.isEmpty()) {
            Toast.makeText(requireContext(), "All fields and location must be filled", Toast.LENGTH_SHORT).show()
            return
        }
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Adding problem...")
        progressDialog.setCancelable(false)
        progressDialog.show()
        // First, upload the image to Firebase Storage
        val imageRef: StorageReference = storage.reference.child("problem_images/${problemTitle}.jpg")
        imageRef.putFile(selectedImageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        // Then, save problem data including the image URL to Firestore
                        val problem = hashMapOf<String, Any>()
                        problem["title"] = problemTitle
                        problem["description"] = problemDescription
                        problem["imageUrl"] = uri.toString()
                        problem["latitude"] = selectedLocation!!.latitude
                        problem["longitude"] = selectedLocation!!.longitude
                        problem["userEmail"] = FirebaseAuth.getInstance().currentUser?.email!!

                        problem["dateStarted"] = dateStarted
                        problem["ageOfPlant"] = ageOfPlant
                        problem["suggestion"] = ""
                        problem["expertName"] = ""

                        firestore.collection("Problems")
                            .add(problem)
                            .addOnSuccessListener { documentReference ->
                                progressDialog.dismiss()
                                Toast.makeText(requireContext(), "Problem added successfully", Toast.LENGTH_SHORT).show()
                                NavHostFragment.findNavController(this).popBackStack()

                                // Optionally, you can also save this problem data to Room database for offline access
                                saveProblemToLocalDatabase(
                                    PlantProblem(
                                        documentReference.id,
                                        FirebaseAuth.getInstance().currentUser?.email!!,
                                        problemTitle,
                                        uri.toString(),
                                        problemDescription,
                                        selectedLocation!!.latitude,
                                        selectedLocation!!.longitude,
                                        dateStarted,
                                        ageOfPlant,
                                        "",
                                        ""
                                    )
                                )
                            }
                            .addOnFailureListener { e ->
                                progressDialog.dismiss()
                                Toast.makeText(requireContext(), "Failed to add problem", Toast.LENGTH_SHORT).show()
                            }
                    }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProblemToLocalDatabase(plantProblem: PlantProblem) {
        Thread { localDb.plantProblemDao().insert(plantProblem) }.start()
    }
}
