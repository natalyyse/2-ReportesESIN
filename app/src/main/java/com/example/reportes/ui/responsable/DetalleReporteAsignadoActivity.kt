package com.example.reportes.ui.responsable

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import com.bumptech.glide.Glide
import com.example.reportes.ui.usuario.BaseActivity
import com.example.reportes.R
import com.example.reportes.data.model.Reporte
import com.example.reportes.viewmodels.ViewModelResponsable.DetalleReporteAViewModel
import com.github.chrisbanes.photoview.PhotoView
import com.google.firebase.auth.FirebaseAuth

class DetalleReporteAsignadoActivity : BaseActivity() {

    // --- Constantes ---
    companion object {
        private const val REQUEST_CERRAR_REPORTE = 1001
        private const val EXTRA_REPORTE = "reporte"
        private const val ESTADO_CERRADO = "Cerrado"
        private const val ESTADO_CERRADO_PARCIAL = "Cerrado parcial"
        private const val ESTADO_ACEPTADO = "aceptado"
        private const val ESTADO_RECHAZADO = "rechazado"
        private const val ESTADO_PENDIENTE = "pendiente"
    }

    // --- ViewModel ---
    private val viewModel: DetalleReporteAViewModel by viewModels()

    // --- Vistas ---
    private lateinit var tvTipoTitulo: TextView
    private lateinit var tvDescripcion: TextView
    private lateinit var tvLugar: TextView
    private lateinit var imgReporte: ImageView
    private lateinit var btnLevantar: Button
    private lateinit var tvEstadoAceptado: TextView
    private lateinit var tvEstadoRechazado: TextView
    private lateinit var tvMotivoRechazo: TextView
    private lateinit var tvNoHistorial: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_reporte_asignado)

        // --- Obtención de datos del Intent ---
        val reporte = intent.getSerializableExtra(EXTRA_REPORTE) as? Reporte
        if (reporte == null) {
            Toast.makeText(this, "No se pudo cargar el reporte", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // --- Inicialización de vistas y configuración inicial ---
        initViews()
        populateReportDetails(reporte)
        setupListeners(reporte)
        setupObservers()

        // --- Carga de datos ---
        viewModel.cargarHistorial(reporte.id)
    }

    // --- Inicialización de Vistas ---
    private fun initViews() {
        tvTipoTitulo = findViewById(R.id.tvTipoTitulo)
        tvDescripcion = findViewById(R.id.tvDescripcion)
        tvLugar = findViewById(R.id.tvLugar)
        imgReporte = findViewById(R.id.imgReporte)
        btnLevantar = findViewById(R.id.btnLevantar)
        tvEstadoAceptado = findViewById(R.id.tvEstadoAceptado)
        tvEstadoRechazado = findViewById(R.id.tvEstadoRechazado)
        tvMotivoRechazo = findViewById(R.id.tvMotivoRechazo)
        tvNoHistorial = findViewById(R.id.tvNoHistorial)
    }

    // --- Configuración de datos del reporte en las vistas ---
    private fun populateReportDetails(reporte: Reporte) {
        tvTipoTitulo.text = reporte.tipo
        tvDescripcion.text = reporte.descripcion
        tvLugar.text = reporte.lugar

        if (reporte.imagenUrl.isNotEmpty()) {
            Glide.with(this).load(reporte.imagenUrl).into(imgReporte)
        }

        // Deshabilitar botón si el reporte ya está cerrado desde la lista principal
        if (reporte.estado.equals(ESTADO_CERRADO, ignoreCase = true) ||
            reporte.estado.equals(ESTADO_CERRADO_PARCIAL, ignoreCase = true)) {
            setLevantarButtonState(false)
        }
    }

    // --- Configuración de Listeners ---
    private fun setupListeners(reporte: Reporte) {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        imgReporte.setOnClickListener {
            if (reporte.imagenUrl.isNotEmpty()) {
                showImagePreview(reporte.imagenUrl)
            }
        }

        btnLevantar.setOnClickListener {
            val intent = Intent(this, ReportecerradoActivity::class.java).apply {
                putExtra("reporteId", reporte.id)
                putExtra("correoResponsable", FirebaseAuth.getInstance().currentUser?.email ?: "")
            }
            startActivityForResult(intent, REQUEST_CERRAR_REPORTE)
        }
    }

    // --- Configuración de Observadores de LiveData ---
    private fun setupObservers() {
        viewModel.historialCierre.observe(this) { cierre ->
            hideAllHistorialViews()
            if (cierre != null) {
                when (cierre.estadoCierre.lowercase()) {
                    ESTADO_ACEPTADO -> {
                        setLevantarButtonState(false)
                        tvEstadoAceptado.text = "Aceptado"
                        tvEstadoAceptado.visibility = View.VISIBLE
                    }
                    ESTADO_RECHAZADO -> {
                        setLevantarButtonState(true)
                        tvEstadoRechazado.text = "Rechazado"
                        tvEstadoRechazado.visibility = View.VISIBLE
                        if (!cierre.motivo.isNullOrEmpty()) {
                            tvMotivoRechazo.text = "Motivo: ${cierre.motivo}"
                            tvMotivoRechazo.visibility = View.VISIBLE
                        }
                    }
                    ESTADO_PENDIENTE -> {
                        setLevantarButtonState(false)
                        tvNoHistorial.text = "Pendiente de revisión"
                        tvNoHistorial.visibility = View.VISIBLE
                    }
                }
            } else {
                // No hay historial, el botón se mantiene en su estado por defecto (habilitado)
                tvNoHistorial.text = "No hay datos de historial"
                tvNoHistorial.visibility = View.VISIBLE
            }
        }

        viewModel.error.observe(this) { errorMsg ->
            hideAllHistorialViews()
            tvNoHistorial.text = errorMsg
            tvNoHistorial.visibility = View.VISIBLE
        }
    }

    // --- Métodos de Ayuda ---
    private fun setLevantarButtonState(enabled: Boolean) {
        btnLevantar.isEnabled = enabled
        btnLevantar.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun hideAllHistorialViews() {
        tvEstadoAceptado.visibility = View.GONE
        tvEstadoRechazado.visibility = View.GONE
        tvMotivoRechazo.visibility = View.GONE
        tvNoHistorial.visibility = View.GONE
    }

    private fun showImagePreview(imageUrl: String) {
        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
            setContentView(R.layout.dialog_image_preview)
            setCancelable(true)
        }

        val photoView = dialog.findViewById<PhotoView>(R.id.photoView)
        val btnClose = dialog.findViewById<ImageButton>(R.id.btnClose)

        Glide.with(this).load(imageUrl).into(photoView)
        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.show()
    }

    // --- Resultado de Actividad ---
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CERRAR_REPORTE && resultCode == RESULT_OK) {
            // Recargar o finalizar para reflejar el nuevo estado pendiente
            viewModel.cargarHistorial((intent.getSerializableExtra(EXTRA_REPORTE) as Reporte).id)
        }
    }
}