package com.example.proyecto_agenda

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav_view)

        setSupportActionBar(toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_consultar, R.id.nav_agregar, R.id.nav_respaldo, R.id.nav_restaurar, R.id.nav_acerca),
            drawerLayout
        )

        toolbar.setupWithNavController(navController, appBarConfiguration)

        // CORRECCIÓN BOTÓN SALIR
        // 1. Vinculamos navegación normal
        bottomNav.setupWithNavController(navController)

        // 2. Sobrescribimos el listener para interceptar "Salir"
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_salir -> {
                    finish() // Cierra la app
                    true
                }
                else -> {
                    // Para los otros items, dejamos que NavigationUI maneje la navegación
                    NavigationUI.onNavDestinationSelected(item, navController)
                    true
                }
            }
        }

        navView.setupWithNavController(navController)

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_salir -> {
                    finish()
                    true
                }
                else -> {
                    val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                    if (handled) {
                        drawerLayout.closeDrawers()
                    }
                    handled
                }
            }
        }
    }
}