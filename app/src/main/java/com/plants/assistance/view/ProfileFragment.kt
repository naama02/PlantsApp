package com.plants.assistance.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.plants.assistance.R
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    private lateinit var imgProfile: ImageView
    private var filePath: Uri? = null
    private lateinit var mAuth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var edtName: EditText
    private var selectedImageUri: Uri? = null
    private lateinit var userTypeRadioGroup: RadioGroup
    private lateinit var normalUserRadioButton: RadioButton
    private lateinit var expertUserRadioButton: RadioButton
    companion object {
        private const val REQUEST_CODE_GALLERY = 1
    }
    private val imagePickerLauncher = registerForActivityResult<String, Uri>(
        ActivityResultContracts.GetContent(),
        object : ActivityResultCallback<Uri?> {
            override fun onActivityResult(result: Uri?) {
                if (result != null) {
                    try {
                        result?.let {
                            Picasso.get().load(it).into(imgProfile)
                            selectedImageUri = it
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    )
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        mAuth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        imgProfile = view.findViewById(R.id.profile_image)
        val btnChooseImage = view.findViewById<Button>(R.id.edit_profile_image_button)
        val btnUpdate = view.findViewById<Button>(R.id.edit_profile_name_button)
        val btnLogout = view.findViewById<Button>(R.id.logout_button)
        edtName = view.findViewById(R.id.profile_name_edittext)
        userTypeRadioGroup = view.findViewById(R.id.radioGroupUserType)
        normalUserRadioButton = view.findViewById(R.id.radioButtonRegular)
        expertUserRadioButton = view.findViewById(R.id.radioButtonExpert)
        // Initialize with current user info
        val user = mAuth.currentUser
        user?.let {
            if (user.photoUrl != null) {
                Picasso.get().load(user.photoUrl).into(imgProfile)
            }
            edtName.setText(user.displayName)
        }

        btnChooseImage.setOnClickListener { chooseImage() }

        btnUpdate.setOnClickListener { updateProfile() }

        btnLogout.setOnClickListener { logout() }

        return view
    }

    private fun chooseImage() {
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

    private fun updateProfile() {
        val name = edtName.text.toString().trim()
        val userType = if (normalUserRadioButton.isChecked) "Normal User" else "Expert User"
        filePath?.let { fileUri ->
            val ref = storageReference.child("profileImages/${mAuth.currentUser?.uid}")
            ref.putFile(fileUri).addOnSuccessListener { taskSnapshot ->
                ref.downloadUrl.addOnSuccessListener { uri ->
                    val profileUpdate = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .setPhotoUri(uri)
                            .build()
                    mAuth.currentUser?.updateProfile(profileUpdate)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                    Picasso.get().load(uri).into(imgProfile)
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            // Update name only
            val profileUpdate = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
            mAuth.currentUser?.updateProfile(profileUpdate)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun logout() {
        mAuth.signOut()
        startActivity(Intent(context, LoginActivity::class.java))
        activity?.finish()
    }
}
