package com.example.reportes.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.reportes.R
import com.example.reportes.adapter.ReporteAdp
import com.example.reportes.data.Remote.FirebaseService
import com.example.reportes.data.model.Reporte
import com.example.reportes.ui.administrador.DetallesActivity
import com.example.reportes.ui.responsable.DetalleReporteAsignadoActivity
import com.example.reportes.ui.usuario.PrincipalActivity
import com.example.reportes.viewmodels.ReportesEstadoViewModel
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class ReportesEstadoActivity : AppCompatActivity() {

    private val viewModel: ReportesEstadoViewModel by viewModels()
    private lateinit var adapter: ReporteAdp
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var titulo: TextView

    // Guarda la lista actual de reportes
    private var listaReportesActual: List<Reporte> = emptyList()

    companion object {
        private const val DETALLES_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estado_reportes)

        val recycler = findViewById<RecyclerView>(R.id.recyclerReportes)
        tabLayout = findViewById(R.id.tabLayout)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        titulo = findViewById(R.id.tvLeyendaTitulo)

        adapter = ReporteAdp(listaReportesActual, false) { reporte ->
            val esAdmin = viewModel.rolUsuario.value == "admin"
            if (esAdmin) {
                val intent = Intent(this, DetallesActivity::class.java)
                intent.putExtra("reporteId", reporte.id)
                startActivityForResult(intent, DETALLES_REQUEST_CODE)
            } else {
                val intent = Intent(this, DetalleReporteAsignadoActivity::class.java)
                intent.putExtra("reporte", reporte)
                startActivity(intent)
            }
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        swipeRefresh.setColorSchemeResources(
            R.color.colorPrimaryDark,
            R.color.colorAccent
        )
        swipeRefresh.setOnRefreshListener { viewModel.cargarReportes() }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            val intent = Intent(this, PrincipalActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        viewModel.rolUsuario.observe(this, Observer { rol ->
            configurarUI(rol)
            viewModel.cargarReportes()
        })

        viewModel.reportes.observe(this, Observer {
            aplicarFiltro(tabLayout.getTabAt(tabLayout.selectedTabPosition)?.text.toString())
        })

        viewModel.pendientesCerradosIds.observe(this, Observer {
            aplicarFiltro(tabLayout.getTabAt(tabLayout.selectedTabPosition)?.text.toString())
        })

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                aplicarFiltro(tab.text.toString())
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        viewModel.cargarRolUsuario()
    }

    private fun configurarUI(rolUsuario: String) {
        val esAdmin = rolUsuario == "admin"
        titulo.text = if (esAdmin) "Reportes" else "Reportes Asignados"
        if (!esAdmin) titulo.gravity = Gravity.CENTER

        tabLayout.removeAllTabs()
        if (esAdmin) {
            listOf("Todos", "Pendiente", "Asignado", "Cerrado").forEach {
                tabLayout.addTab(tabLayout.newTab().setText(it))
            }
        } else {
            listOf("Todos", "Asignado", "Pendiente", "Cerrado").forEach {
                tabLayout.addTab(tabLayout.newTab().setText(it))
            }
        }

        val layoutRevision = findViewById<LinearLayout>(R.id.layoutRevision)
        layoutRevision.visibility = if (esAdmin) View.VISIBLE else View.GONE

        // Reinicializa el adaptador con la lista actual y el valor correcto de esAdmin
        adapter = ReporteAdp(listaReportesActual, esAdmin) { reporte ->
            if (esAdmin) {
                val intent = Intent(this, DetallesActivity::class.java)
                intent.putExtra("reporteId", reporte.id)
                startActivityForResult(intent, DETALLES_REQUEST_CODE)
            } else {
                val intent = Intent(this, DetalleReporteAsignadoActivity::class.java)
                intent.putExtra("reporte", reporte)
                startActivity(intent)
            }
        }
        findViewById<RecyclerView>(R.id.recyclerReportes).adapter = adapter
    }

    private fun aplicarFiltro(estado: String) {
        val listaFinal = viewModel.filtrarReportes(estado)
        listaReportesActual = listaFinal // Actualiza la lista actual
        adapter.updateList(listaFinal)
        swipeRefresh.isRefreshing = false
    }

    // Agregar método onActivityResult
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == DETALLES_REQUEST_CODE && resultCode == RESULT_OK) {
            val navigateToAsignadoTab = data?.getBooleanExtra("navigateToAsignadoTab", false) ?: false

            if (navigateToAsignadoTab) {
                // Encontrar el índice del tab "Asignado"
                val tabCount = tabLayout.tabCount
                for (i in 0 until tabCount) {
                    val tab = tabLayout.getTabAt(i)
                    if (tab?.text == "Asignado") {
                        tab.select()
                        break
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Actualizar datos cuando la actividad vuelve a primer plano
        if (::adapter.isInitialized && adapter.itemCount == 0) {
            viewModel.cargarReportes()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Eliminar listeners para evitar fugas de memoria
        viewModel.limpiarListeners()
    }
}