package com.plants.assistance.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.plants.assistance.R

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
//    public var userType: String? = null

    companion object {
        private var userType: String? = null
        private const val EXTRA_USER_TYPE = "user_type"

        fun setUserType(userType: String) {
            Companion.userType = userType
        }

        fun getUserType(): String? {
            return userType
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val currentUser: FirebaseUser? = mAuth.currentUser
        if (currentUser != null) {
            navigateToPlantList()
        }

        val buttonRegister: Button = findViewById(R.id.register_button)
        val buttonLogin: Button = findViewById(R.id.login_button)
        buttonRegister.setOnClickListener {
            navigateToRegister()
        }
        buttonLogin.setOnClickListener {
            val email: String = findViewById<EditText>(R.id.emai_textview).text.toString()
            val password: String = findViewById<EditText>(R.id.password_edittext).text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            getUserTypeAndNavigate()
                        } else {
                            Toast.makeText(this@LoginActivity, "Login Failed", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this@LoginActivity, "You did not enter your info", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToRegister() {
        val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToPlantList() {
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun getUserTypeAndNavigate() {
        val currentUser: FirebaseUser? = mAuth.currentUser
        currentUser?.let { user ->
            firestore.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        userType = document.getString("userType")
                        setUserType(userType ?: "")
                        navigateToPlantList()
                    } else {
                        Toast.makeText(this@LoginActivity, "User data not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this@LoginActivity, "Error getting user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
