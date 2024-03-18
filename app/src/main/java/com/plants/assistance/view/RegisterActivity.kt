package com.plants.assistance.view

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.plants.assistance.R
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private companion object {
        const val PICK_IMAGE_REQUEST = 71
    }

    private lateinit var mAuth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private var filePath: Uri? = null
    private lateinit var imgProfile: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        mAuth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        val btnChooseImage: Button = findViewById(R.id.btnChooseImage)
        val btnRegister: Button = findViewById(R.id.finish_register)
        val btnCancel: Button = findViewById(R.id.registerCancel)
        imgProfile = findViewById(R.id.imgProfile)

        btnChooseImage.setOnClickListener { chooseImage() }

        btnRegister.setOnClickListener { registerUser() }

        btnCancel.setOnClickListener { finish() }
    }

    private fun chooseImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    private fun registerUser() {
        val email = findViewById<EditText>(R.id.registerEmailAddress).text.toString().trim()
        val password = findViewById<EditText>(R.id.registerPassword).text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email or password should not be empty", Toast.LENGTH_LONG).show()
            return
        }

        val radioGroupUserType = findViewById<RadioGroup>(R.id.radioGroupUserType)
        val userType = when (radioGroupUserType.checkedRadioButtonId) {
            R.id.radioButtonRegular -> "Regular"
            R.id.radioButtonExpert -> "Expert"
            else -> "Regular"
        }

        if (filePath != null) {
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, upload the profile image
                            val user = mAuth.currentUser
                            user?.let {
                                val ref = storageReference.child("profileImages/${user.uid}")
                                ref.putFile(filePath!!)
                                        .addOnSuccessListener { taskSnapshot ->
                                            ref.downloadUrl.addOnSuccessListener { uri ->
                                                // Here we get the image URL
                                                val profileUpdates = UserProfileChangeRequest.Builder()
                                                        .setPhotoUri(uri)
                                                        .build()

                                                user.updateProfile(profileUpdates)
                                                        .addOnCompleteListener { task ->
                                                            if (task.isSuccessful) {
                                                                Toast.makeText(
                                                                        this@RegisterActivity,
                                                                        "User profile updated.",
                                                                        Toast.LENGTH_SHORT
                                                                ).show()
                                                                saveUserTypeToFirestore(user.uid, userType)
                                                                val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                                                                startActivity(intent)
                                                                finish() // Finish current activity
                                                            }
                                                        }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this@RegisterActivity, "Failed ${e.message}", Toast.LENGTH_SHORT)
                                                    .show()
                                        }
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(this@RegisterActivity, "Authentication failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
        } else {
            Toast.makeText(this, "Please choose a profile image", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            filePath = data.data
            try {
                val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                imgProfile.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    private fun saveUserTypeToFirestore(userId: String, userType: String) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)

        val userData = hashMapOf(
            "userType" to userType
        )

        userRef.set(userData)
            .addOnSuccessListener {
            }
            .addOnFailureListener { e ->
            }
    }
}
