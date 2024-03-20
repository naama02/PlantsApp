package com.plants.assistance.view

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.os.Bundle
import androidx.navigation.Navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.plants.assistance.R

class MainActivity : AppCompatActivity() {

        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_main)

                val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
                val navController = findNavController(this, R.id.nav_host_fragment)
                bottomNavigationView.setupWithNavController( navController)

        }

}
