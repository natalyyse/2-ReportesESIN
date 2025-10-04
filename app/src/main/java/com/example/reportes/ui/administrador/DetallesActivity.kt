package com.example.reportes.ui.administrador

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.reportes.ui.usuario.BaseActivity
import com.example.reportes.R
import com.example.reportes.viewmodels.ViewModelAdmin.DetallesViewModel
import com.github.chrisbanes.photoview.PhotoView


class DetallesActivity : BaseActivity() {
    private val viewModel: DetallesViewModel by viewModels()
    private var reporteId: String? = null

    companion object {
        private const val ASIGNAR_REQUEST_CODE = 100
        private const val EDITAR_REQUEST_CODE = 101
        private const val INFO_REQUEST_CODE = 102
    }

    private var botonBloqueado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalles)

        reporteId = intent.getStringExtra("reporteId")
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        val btnAsignar = findViewById<Button>(R.id.btnAsignar)
        btnAsignar.isEnabled = false
        btnAsignar.text = ""

        val ivInfo = findViewById<ImageView>(R.id.ivInfo)
        val ivEditar = findViewById<ImageView>(R.id.ivEditar)

        if (reporteId.isNullOrEmpty()) {
            Log.e("DetallesActivity", "Error: ID de reporte no válido")
            Toast.makeText(this, "Error: ID de reporte no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Configuración inicial y listeners
        setupObservers()
        setupClickListeners()

        // Inicia la escucha de datos del reporte
        viewModel.escucharDetallesEnTiempoReal(reporteId!!)
    }

    /**
     * Configura los observadores para la UI y los errores.
     */
    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            if (state != null) {
                actualizarUI(state)
            }
        }
        viewModel.error.observe(this) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Configura los listeners para los botones y elementos interactivos.
     */
    private fun setupClickListeners() {
        val btnAsignar = findViewById<Button>(R.id.btnAsignar)
        val ivInfo = findViewById<ImageView>(R.id.ivInfo)
        val ivEditar = findViewById<ImageView>(R.id.ivEditar)

        btnAsignar.setOnClickListener {
            if (reporteId != null) {
                val intent = Intent(this, AsignarActivity::class.java)
                intent.putExtra("reporteId", reporteId)
                startActivityForResult(intent, ASIGNAR_REQUEST_CODE)
            }
        }

        ivInfo.setOnClickListener {
            if (botonBloqueado) return@setOnClickListener
            if (ivInfo.alpha < 1.0f) {
                bloquearBotonConToast("No hay información disponible para este reporte")
                return@setOnClickListener
            }
            if (reporteId != null) {
                val intent = Intent(this, InformacionActivity::class.java)
                intent.putExtra("reporteId", reporteId)
                startActivityForResult(intent, INFO_REQUEST_CODE)
            }
        }

        ivEditar.setOnClickListener {
            if (botonBloqueado) return@setOnClickListener
            if (ivEditar.alpha < 1.0f) {
                val mensaje = when (btnAsignar.text) {
                    "CERRADO" -> "Solo puedes editar cuando el reporte está ASIGNADO o CERRADO PARCIAL"
                    "PENDIENTE" -> "No puedes editar cuando el reporte está PENDIENTE"
                    else -> "No se puede editar este reporte en su estado actual"
                }
                bloquearBotonConToast(mensaje)
                return@setOnClickListener
            }

            val currentReporteId = reporteId
            val currentState = viewModel.uiState.value
            if (currentReporteId != null && currentState != null) {
                if (currentState.estado == "Asignado") {
                    val resultadoEdicion = viewModel.puedeEditar(currentState.fechaAsignacion, currentState.fechaLimite)
                    if (resultadoEdicion.first) {
                        abrirEditarActivity(currentReporteId, false)
                    } else {
                        Toast.makeText(this, resultadoEdicion.second, Toast.LENGTH_SHORT).show()
                    }
                } else if (currentState.estado == "Cerrado parcial") {
                    abrirEditarActivity(currentReporteId, true)
                } else {
                    Toast.makeText(this, "Solo puedes editar cuando el reporte está ASIGNADO o CERRADO PARCIAL", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Actualiza la interfaz de usuario con los datos del estado actual.
     */
    private fun actualizarUI(state: DetallesViewModel.DetallesUiState) {
        val tvTipo = findViewById<TextView>(R.id.tvTipo)
        val tvDescripcion = findViewById<TextView>(R.id.tvDescripcion)
        val tvLugar = findViewById<TextView>(R.id.tvLugar)
        val tvFecha = findViewById<TextView>(R.id.tvFecha)
        val tvNombre = findViewById<TextView>(R.id.tvNombre)
        val imgReporte = findViewById<ImageView>(R.id.imgReporte)
        val btnAsignar = findViewById<Button>(R.id.btnAsignar)
        val ivInfo = findViewById<ImageView>(R.id.ivInfo)
        val ivEditar = findViewById<ImageView>(R.id.ivEditar)

        tvTipo.text = state.tipo
        tvDescripcion.text = state.descripcion
        tvLugar.text = state.lugar
        tvNombre.text = state.nombre
        tvFecha.text = state.fecha
        if (!state.imagenUrl.isNullOrEmpty()) {
            Glide.with(this).load(state.imagenUrl).into(imgReporte)
            imgReporte.setOnClickListener { showImagePreview(state.imagenUrl) }
        } else {
            imgReporte.setImageResource(R.drawable.ic_image_placeholder)
        }

        // Lógica para habilitar o deshabilitar el icono de información
        ivInfo.alpha = if (state.estadoCierre != null || state.estado == "Cerrado") 1.0f else 0.5f

        // Lógica para el estado del botón principal y el icono de edición
        if (state.estadoCierre == "pendiente") {
            btnAsignar.text = "PENDIENTE"
            btnAsignar.isEnabled = false
            btnAsignar.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccent))
            ivEditar.alpha = 0.5f
        } else {
            when (state.estado) {
                "Pendiente" -> {
                    btnAsignar.text = "ASIGNAR"
                    btnAsignar.isEnabled = true
                    btnAsignar.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccent))
                    ivEditar.alpha = 0.5f
                }
                "Asignado" -> {
                    btnAsignar.text = "ASIGNADO"
                    btnAsignar.isEnabled = false
                    btnAsignar.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccent))
                    val resultadoEdicion = viewModel.puedeEditar(state.fechaAsignacion, state.fechaLimite)
                    ivEditar.alpha = if (resultadoEdicion.first) 1.0f else 0.5f
                }
                "Cerrado" -> {
                    btnAsignar.text = "CERRADO"
                    btnAsignar.isEnabled = false
                    btnAsignar.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccent))
                    ivEditar.alpha = 0.5f
                }
                "Cerrado parcial" -> {
                    btnAsignar.text = "CERRADO PARCIAL"
                    btnAsignar.isEnabled = false
                    btnAsignar.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccent))
                    ivEditar.alpha = 1.0f
                }
                else -> {
                    btnAsignar.text = "ASIGNAR"
                    btnAsignar.isEnabled = true
                    btnAsignar.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccent))
                    ivEditar.alpha = 0.5f
                }
            }
        }
    }

    /**
     * Inicia la EditarActivity con los extras necesarios.
     */
    private fun abrirEditarActivity(reporteId: String, esCerradoParcial: Boolean) {
        val intent = Intent(this, EditarActivity::class.java)
        intent.putExtra("reporteId", reporteId)
        if (esCerradoParcial) {
            intent.putExtra("esReporteCerradoParcial", true)
        }
        startActivityForResult(intent, EDITAR_REQUEST_CODE)
    }

    /**
     * Bloquea temporalmente los botones para evitar clics múltiples y muestra un Toast.
     */
    private fun bloquearBotonConToast(mensaje: String) {
        botonBloqueado = true
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({ botonBloqueado = false }, 2000)
    }

    /**
     * Maneja el resultado de las actividades iniciadas con startActivityForResult.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == RESULT_OK) {
            val returnedReporteId = data?.getStringExtra("reporteId") ?: this.reporteId
            
            when (requestCode) {
                ASIGNAR_REQUEST_CODE -> {
                    val emailEnviado = data?.getBooleanExtra("emailEnviado", false) ?: false
                    if (emailEnviado && returnedReporteId != null) {
                        viewModel.cargarDetallesCompletos(returnedReporteId)
                        val resultIntent = Intent()
                        resultIntent.putExtra("navigateToAsignadoTab", true)
                        setResult(RESULT_OK, resultIntent)
                    }
                }
                EDITAR_REQUEST_CODE, INFO_REQUEST_CODE -> {
                    if (returnedReporteId != null) {
                        viewModel.cargarDetallesCompletos(returnedReporteId)
                    }
                }
            }
        }
    }

    /**
     * Muestra un diálogo de pantalla completa para previsualizar la imagen del reporte.
     */
    private fun showImagePreview(imageUrl: String) {
        if (imageUrl.isEmpty()) {
            Toast.makeText(this, "No hay imagen disponible", Toast.LENGTH_SHORT).show()
            return
        }
        
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        dialog.setContentView(R.layout.dialog_image_preview)
        dialog.setCancelable(true)
        
        val photoView = dialog.findViewById<PhotoView>(R.id.photoView)
        val btnClose = dialog.findViewById<ImageButton>(R.id.btnClose)
        
        Glide.with(this).load(imageUrl).into(photoView)
        
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        dialog.show()
    }
}