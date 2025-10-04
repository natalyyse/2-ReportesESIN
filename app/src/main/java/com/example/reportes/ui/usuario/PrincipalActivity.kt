package com.example.reportes.ui.usuario

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import com.example.reportes.R
import com.example.reportes.ui.ReportesEstadoActivity
import com.example.reportes.ui.administrador.ResumenActivity
import com.example.reportes.viewmodels.ViewModelUsuario.PrincipalViewModel
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

/**
 * Actividad principal que muestra el menú de navegación y las opciones
 * según el rol del usuario logueado.
 */
class PrincipalActivity : BaseActivity() {

    // ViewModel que provee los datos y el estado de la UI.
    private val viewModel: PrincipalViewModel by viewModels()
    private lateinit var navigationView: NavigationView
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        // Inicialización de vistas.
        drawerLayout = findViewById(R.id.drawer_layout)
        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)
        navigationView = findViewById(R.id.navigation_view)

        // Configura el comportamiento del menú lateral (Drawer).
        setupDrawer(btnMenu)

        // Configura los listeners para los items del menú.
        setupNavigationListener()

        // Inicia la observación de los datos del ViewModel.
        observeViewModel()

        // Pide al ViewModel que comience a escuchar los cambios de rol del usuario.
        viewModel.iniciarEscuchaDeRol()
    }

    /**
     * Configura el comportamiento del DrawerLayout y el botón de menú.
     */
    private fun setupDrawer(btnMenu: ImageButton) {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    /**
     * Observa los LiveData del ViewModel y actualiza la UI en consecuencia.
     */
    private fun observeViewModel() {
        // Observa el rol y la condición de levantamiento para actualizar el menú.
        val observer = Observer<Any> {
            val rol = viewModel.rol.value ?: "usuario"
            val hayLevantamiento = viewModel.hayLevantamiento.value ?: false
            configurarMenu(rol, hayLevantamiento)
        }
        viewModel.rol.observe(this, observer)
        viewModel.hayLevantamiento.observe(this, observer)
    }

    /**
     * Configura la visibilidad de los items del menú según el rol del usuario
     * y si tiene levantamientos pendientes.
     * @param rol El rol actual del usuario ("admin", "responsable", "usuario").
     * @param hayLevantamiento True si el responsable tiene reportes no cerrados.
     */
    private fun configurarMenu(rol: String, hayLevantamiento: Boolean) {
        val menu = navigationView.menu
        val administracionItem = menu.findItem(R.id.nav_administracion)
        val resumenItem = menu.findItem(R.id.nav_resumen)
        val levantamientoItem = menu.findItem(R.id.nav_levantamiento)

        // Visibilidad basada en el rol.
        administracionItem?.isVisible = rol == "admin"
        resumenItem?.isVisible = rol == "admin"
        // El item de levantamiento solo es visible para un "responsable" Y si tiene reportes pendientes.
        levantamientoItem?.isVisible = rol == "responsable" && hayLevantamiento
    }

    /**
     * Define las acciones para cada item del menú de navegación.
     */
    private fun setupNavigationListener() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawers()
            when (menuItem.itemId) {
                R.id.nav_reporte -> startActivity(Intent(this, ReporteActivity::class.java))
                R.id.nav_configuracion -> startActivity(Intent(this, ConfiguracionActivity::class.java))
                R.id.nav_resumen -> startActivity(Intent(this, ResumenActivity::class.java))
                R.id.nav_administracion, R.id.nav_levantamiento -> {
                    val intent = Intent(this, ReportesEstadoActivity::class.java)
                    intent.putExtra("esAdmin", menuItem.itemId == R.id.nav_administracion)
                    startActivity(intent)
                }
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            true
        }
    }
}