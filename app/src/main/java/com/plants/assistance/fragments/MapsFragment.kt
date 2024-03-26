package com.plants.assistance.fragments

import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.plants.assistance.R
import java.io.IOException

class MapsFragment : Fragment() {

    private lateinit var map: GoogleMap
    private lateinit var searchField: EditText
    private lateinit var searchButton: Button

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap
        setupMapListeners()
        loadPlants()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maps, container, false)
        initializeViews(view)
        setupButtonListeners()
        return view
    }

    private fun initializeViews(view: View) {
        searchField = view.findViewById(R.id.searchName)
        searchButton = view.findViewById(R.id.search_button)
    }

    private fun setupButtonListeners() {
        searchButton.setOnClickListener { searchLocation() }
    }

    private fun searchLocation() {
        val location = searchField.text.toString()
        if (location.isNotEmpty()) {
            val geocoder = context?.let { Geocoder(it) }
            try {
                val addresses = geocoder?.getFromLocationName(location, 1)
                if (addresses?.isNotEmpty() == true) {
                    val address = addresses?.get(0)
                    val latLng = address?.latitude?.let { LatLng(it, address.longitude) }
                    latLng?.let { CameraUpdateFactory.newLatLngZoom(it, 12f) }?.let { map.moveCamera(it) }
                } else {
                    Toast.makeText(context, "Not a valid address", Toast.LENGTH_LONG).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun setupMapListeners() {
        map.setOnMarkerClickListener { marker ->
            val bundle = Bundle()
            val document = marker.tag as? QueryDocumentSnapshot
            document?.let {
                val plantId = document.id
                val plantName = document.getString("plantName") ?: ""
                val userEmail = document.getString("userEmail") ?: ""
                val imageUrl = document.getString("imageUrl") ?: ""
                val description = document.getString("description") ?: ""
                val latitude = document.getDouble("latitude") ?: 0.0
                val longitude = document.getDouble("longitude") ?: 0.0
                bundle.putString("plantId", plantId)
                bundle.putString("plantName", plantName)
                bundle.putString("userEmail", userEmail)
                bundle.putString("imageUrl", imageUrl)
                bundle.putString("description", description)
                bundle.putDouble("latitude", latitude)
                bundle.putDouble("longitude", longitude)
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_mapsFragment_to_plantPageFragment, bundle)
            }
            false
        }
    }

    private fun loadPlants() {
        val db = FirebaseFirestore.getInstance()

        db.collection("Plants").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                try {
                    for (document in task.result!!) {
                        val plantId = document.id
                        val plantName = document.getString("plantName") ?: ""
                        val latitude = document.getDouble("latitude") ?: 0.0
                        val longitude = document.getDouble("longitude") ?: 0.0

                        if (latitude != 0.0 && longitude != 0.0) {
                            val plantPosition = LatLng(latitude, longitude)
                            val marker = map.addMarker(MarkerOptions().position(plantPosition).title(plantName).snippet(plantId))
                            marker?.tag = document
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error loading plants", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Failed to fetch plants", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapsFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(callback)
    }
}
