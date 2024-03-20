package com.plants.assistance.view

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.plants.assistance.R
import com.plants.assistance.database.AppDatabase
import com.plants.assistance.model.PlantProblem
import com.squareup.picasso.Picasso
class AddPlantsFragment : Fragment(), OnMapReadyCallback {
    private lateinit var problemTitleEditText: EditText
    private lateinit var problemDescriptionEditText: EditText
    private lateinit var problemImageView: ImageView
    private lateinit var dateStartedEditText: EditText // 날짜 선택기를 사용하기 위한 EditText
    private lateinit var ageOfPlantEditText: EditText // 텍스트 필드
    private lateinit var addProblemButton: Button
    private lateinit var cancelButton: Button
    private lateinit var map: GoogleMap
    private var selectedImageUri: Uri? = null
    private var selectedLocation: LatLng? = null

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var localDb: AppDatabase

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        localDb = AppDatabase.getInstance(requireContext())
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
        problemTitleEditText = rootView.findViewById(R.id.problemTitle)
        problemDescriptionEditText = rootView.findViewById(R.id.problemDescription)
        problemImageView = rootView.findViewById(R.id.problemImage)
        dateStartedEditText = rootView.findViewById(R.id.dateStartedEditText) // 추가된 날짜 선택기
        ageOfPlantEditText = rootView.findViewById(R.id.ageOfPlantEditText) // 추가된 텍스트 필드
        addProblemButton = rootView.findViewById(R.id.addProblemButton)
        cancelButton = rootView.findViewById(R.id.cancelButton)
    }

    private fun setupMapFragment() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapsFragment) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    private fun setupListeners() {
        problemImageView.setOnClickListener { selectImage() }
        addProblemButton.setOnClickListener { uploadProblemData() }
        cancelButton.setOnClickListener { NavHostFragment.findNavController(this).popBackStack() }
    }

    private fun selectImage() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireContext() as Activity, arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        } else {
            imagePickerLauncher.launch("image/*")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        // Allow user to select a location from the map
        map.setOnMapClickListener { latLng ->
            selectedLocation = latLng
            map.clear()
            map.addMarker(MarkerOptions().position(latLng))
        }
    }

    private fun uploadProblemData() {
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Adding problem...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val problemTitle = problemTitleEditText.text.toString().trim()
        val problemDescription = problemDescriptionEditText.text.toString().trim()
        val dateStarted = dateStartedEditText.text.toString().trim() // 추가된 날짜 선택기의 값
        val ageOfPlant = ageOfPlantEditText.text.toString().trim() // 추가된 텍스트 필드의 값

        if (problemTitle.isEmpty() || problemDescription.isEmpty() || selectedImageUri == null || selectedLocation == null || dateStarted.isEmpty() || ageOfPlant.isEmpty()) {
            Toast.makeText(requireContext(), "All fields and location must be filled", Toast.LENGTH_SHORT).show()
            return
        }

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

                        // 추가된 필드 값 설정
                        problem["dateStarted"] = dateStarted
                        problem["ageOfPlant"] = ageOfPlant
                        problem["suggestion"] = "" // 원하는 값으로 설정
                        problem["expertName"] = "" // 원하는 값으로 설정

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
                                        "", // 원하는 값으로 설정
                                        "" // 원하는 값으로 설정
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
