package com.plants.assistance.activities

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.os.Bundle
import android.view.View.VISIBLE
import androidx.navigation.Navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.libraries.places.api.Places
import com.google.firebase.auth.FirebaseAuth
import com.plants.assistance.R
import com.plants.assistance.activities.LoginActivity.Companion.getUserType

class MainActivity : AppCompatActivity() {

        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_main)
                Places.initialize(applicationContext, "AIzaSyCT1LawqV43MkqtMqwUuKVSArVpRNdQ5aA")
                if( getUserType().equals("Regular") ) {
                        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
                        bottomNavigationView.visibility = VISIBLE
                        val navController = findNavController(this, R.id.nav_host_fragment)
                        bottomNavigationView.setupWithNavController( navController)
                }else{
                        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_expert)
                        bottomNavigationView.visibility = VISIBLE
                        val navController = findNavController(this, R.id.nav_host_fragment)
                        bottomNavigationView.setupWithNavController( navController)
                }
        }

}
