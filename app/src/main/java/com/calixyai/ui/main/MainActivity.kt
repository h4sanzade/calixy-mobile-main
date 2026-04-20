package com.calixyai.ui.main

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.calixyai.R
import com.calixyai.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun attachBaseContext(newBase: Context) {
        // Apply saved locale before any view inflation
        val prefs = newBase.getSharedPreferences("calixy_prefs", Context.MODE_PRIVATE)
        val localeTag = prefs.getString("selected_locale", "en") ?: "en"
        val locale = Locale(localeTag)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val prefs = getSharedPreferences("calixy_prefs", Context.MODE_PRIVATE)
        val langChosen = prefs.getBoolean("lang_chosen", false)
        if (langChosen) {
            prefs.edit().remove("lang_chosen").apply()
            val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
            navGraph.setStartDestination(R.id.splashFragment)
            navController.setGraph(navGraph, null)
        }

        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val show = destination.id == R.id.homeFragment
            binding.bottomNavigation.animate()
                .alpha(if (show) 1f else 0f)
                .setDuration(200)
                .start()
            binding.bottomNavigation.visibility =
                if (show) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
}