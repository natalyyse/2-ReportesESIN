package com.example.reportes.ui.administrador

import android.app.DatePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import com.example.reportes.ui.usuario.BaseActivity
import com.example.reportes.R
import com.example.reportes.data.repository.admin.AsignarRepository
import com.example.reportes.viewmodels.ViewModelAdmin.AsignarViewModel
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Activity para asignar reportes a responsables
 * Permite seleccionar un responsable, nivel de riesgo y fecha límite para gestionar reportes
 */
class AsignarActivity : BaseActivity() {

    // UI Components
    private lateinit var etResponsable: EditText
    private lateinit var etFechaLimite: EditText
    private lateinit var btnAsignar: Button
    private lateinit var layoutLoading: FrameLayout
    private lateinit var spinnerNivelRiesgo: Spinner
    
    // Data fields
    private var reporteId: String? = null
    private var fechaSeleccionada: Long = 0L
    private var nivelRiesgoSeleccionado: String = ""
    
    // Dependencies
    private val repository = AsignarRepository()
    private val viewModel: AsignarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asignar)

        // Obtener ID del reporte pasado desde la actividad anterior
        reporteId = intent.getStringExtra("reporteId")

        // Inicializar la interfaz y configurar componentes
        initViews()
        setupNivelRiesgoSpinner()
        setupDatePicker()
        setupButtons()
        observarViewModel()
    }

    /**
     * Inicializa las vistas y configura sus propiedades básicas
     */
    private fun initViews() {
        etResponsable = findViewById(R.id.etResponsable)
        etFechaLimite = findViewById(R.id.etFechaLimite)
        btnAsignar = findViewById(R.id.btnAsignarReporte)
        layoutLoading = findViewById(R.id.layoutLoading)
        spinnerNivelRiesgo = findViewById(R.id.spinnerNivelRiesgo)

        // Configurar botón de regreso
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        // Deshabilitar la edición directa del campo de fecha
        etFechaLimite.keyListener = null
    }

    /**
     * Configura el spinner de nivel de riesgo con sus opciones y listener
     */
    private fun setupNivelRiesgoSpinner() {
        val opciones = listOf("Seleccionar", "Alto", "Medio", "Bajo")
        val adapter = ArrayAdapter(this, R.layout.spinner_item, opciones)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinnerNivelRiesgo.adapter = adapter
        spinnerNivelRiesgo.setSelection(0)
        
        // Listener para capturar la selección del usuario
        spinnerNivelRiesgo.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                nivelRiesgoSeleccionado = opciones[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                nivelRiesgoSeleccionado = ""
            }
        })
    }

    /**
     * Configura el selector de fecha con un listener táctil personalizado
     */
    private fun setupDatePicker() {
        // Detectar toques en el área del ícono del calendario
        etFechaLimite.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Verifica si el toque fue en el área del ícono
                if (event.rawX >= (etFechaLimite.right - etFechaLimite.compoundDrawables[2].bounds.width() - etFechaLimite.paddingEnd)) {
                    showDatePicker()
                    return@setOnTouchListener true
                }
            }
            return@setOnTouchListener false
        }
    }

    /**
     * Muestra el diálogo para seleccionar la fecha límite
     */
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                fechaSeleccionada = selectedDate.timeInMillis

                // Formato de fecha: "dd/MM/yyyy"
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                etFechaLimite.setText(sdf.format(selectedDate.time))
            },
            year, month, day
        )

        // Configurar la fecha mínima permitida como el día actual
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        datePickerDialog.datePicker.minDate = today.timeInMillis

        datePickerDialog.show()
    }

    /**
     * Configura los listeners para los botones
     */
    private fun setupButtons() {
        btnAsignar.setOnClickListener {
            asignarReporte()
        }
    }

    /**
     * Configura los observadores del ViewModel para actualizar la UI
     */
    private fun observarViewModel() {
        // Observar estado de carga
        viewModel.loading.observe(this, Observer { showLoading(it) })
        
        // Observar asignación exitosa
        viewModel.asignacionExitosa.observe(this, Observer { data ->
            data?.let {
                enviarEmail(it, etResponsable.text.toString().trim().lowercase())
                viewModel.limpiarEstado()
            }
        })
        
        // Observar errores
        viewModel.error.observe(this, Observer { errorMsg ->
            errorMsg?.let {
                showLoading(false)
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.limpiarEstado()
            }
        })
    }

    /**
     * Valida los datos y envía la solicitud para asignar el reporte
     */
    private fun asignarReporte() {
        val responsable = etResponsable.text.toString().trim().lowercase()

        // Validación del email del responsable
        if (responsable.isEmpty()) {
            etResponsable.error = "Debe ingresar un correo electrónico"
            etResponsable.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(responsable).matches()) {
            etResponsable.error = "Ingrese un correo electrónico válido"
            etResponsable.requestFocus()
            return
        }

        // Validación del nivel de riesgo
        if (nivelRiesgoSeleccionado == "Seleccionar" || nivelRiesgoSeleccionado.isEmpty()) {
            Toast.makeText(this, "Debe seleccionar el nivel de riesgo", Toast.LENGTH_SHORT).show()
            return
        }

        // Validación de la fecha límite
        if (fechaSeleccionada == 0L) {
            Toast.makeText(this, "Debe seleccionar una fecha límite", Toast.LENGTH_SHORT).show()
            return
        }

        // Validación del ID del reporte
        if (reporteId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: ID de reporte no válido", Toast.LENGTH_SHORT).show()
            return
        }

        // Formatea la fecha seleccionada a String legible
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaLimiteStr = sdf.format(Date(fechaSeleccionada))

        // Enviar petición al ViewModel
        viewModel.asignarReporte(
            reporteId!!,
            responsable,
            fechaLimiteStr,
            nivelRiesgoSeleccionado
        )
    }

    /**
     * Muestra u oculta el indicador de carga y actualiza el estado del botón
     * @param show true para mostrar el indicador de carga, false para ocultarlo
     */
    private fun showLoading(show: Boolean) {
        if (show) {
            layoutLoading.visibility = View.VISIBLE
            btnAsignar.isEnabled = false
        } else {
            layoutLoading.visibility = View.GONE
            btnAsignar.isEnabled = true
        }
    }

    /**
     * Envía un email de notificación al responsable asignado
     * @param reporteData Datos del reporte a enviar
     * @param responsable Email del responsable asignado
     */
    private fun enviarEmail(reporteData: Map<String, Any>, responsable: String) {
        // Extraer datos del reporte
        val tipo = reporteData["tipo"] as? String ?: ""
        val descripcion = reporteData["descripcion"] as? String ?: ""
        val lugar = reporteData["lugar"] as? String ?: ""
        val reportante = reporteData["reportante"] as? String ?: ""
        val fechaLimite = reporteData["fechaLimite"] as? String ?: ""
        val fechaAsignacion = reporteData["fechaAsignacion"] as? String ?: ""
        val imagenUrl = reporteData["imagenUrl"] as? String ?: ""
        val nivelRiesgo = reporteData["nivelRiesgo"] as? String ?: ""

        // Construir asunto y cuerpo del email
        val asunto = "ASIGNACIÓN DE REPORTE - ${tipo.uppercase()}"
        val cuerpoEmail = """
        Estimado/a Responsable,

        Se le ha asignado un reporte para su atención y resolución.

        INFORMACIÓN DEL REPORTE

        Clasificación del Reporte: $tipo
        Nivel de Riesgo: $nivelRiesgo
        Descripción Detallada: $descripcion
        Ubicación del Incidente: $lugar
        Persona Reportante: $reportante
        Fecha Límite para Resolución: $fechaLimite
        Fecha de Asignación: $fechaAsignacion
        ${if (imagenUrl.isNotEmpty()) """
        Imagen del Reporte:
        $imagenUrl
        """ else ""}

        Atentamente,

        SISTEMA DE GESTIÓN DE REPORTES ESIN

        NOTA: El levantamiento del reporte debe realizarse exclusivamente por medio de la aplicación móvil del sistema.
    """.trimIndent()

        // Si hay imagen, descargar y adjuntar
        if (imagenUrl.isNotEmpty()) {
            Thread {
                try {
                    // Descargar la imagen desde la URL
                    val url = URL(imagenUrl)
                    val connection = url.openConnection()
                    connection.connect()
                    val input = connection.getInputStream()
                    val file = File.createTempFile("reporte_img", ".jpg", cacheDir)
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    val uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.provider",
                        file
                    )

                    runOnUiThread {
                        // Intento primario: usar Gmail
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            setType("image/*")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(responsable))
                            putExtra(Intent.EXTRA_SUBJECT, asunto)
                            putExtra(Intent.EXTRA_TEXT, cuerpoEmail)
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            setPackage("com.google.android.gm")
                        }
                        try {
                            startActivity(intent)
                            setResultAndFinish()
                        } catch (e: Exception) {
                            Log.e("AsignarActivity", "No se pudo abrir Gmail, intentando con otro cliente", e)
                            
                            // Intento fallback: usar cualquier cliente de email
                            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                                setType("image/*")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(responsable))
                                putExtra(Intent.EXTRA_SUBJECT, asunto)
                                putExtra(Intent.EXTRA_TEXT, cuerpoEmail)
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                startActivity(Intent.createChooser(fallbackIntent, "Enviar email de asignación"))
                                setResultAndFinish()
                            } catch (ex: Exception) {
                                showLoading(false)
                                Toast.makeText(this@AsignarActivity, "No se pudo abrir ningún cliente de email", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AsignarActivity", "Error descargando imagen", e)
                    runOnUiThread {
                        enviarEmailSinAdjunto(responsable, asunto, cuerpoEmail)
                    }
                }
            }.start()
        } else {
            // Si no hay imagen, enviar email simple
            enviarEmailSinAdjunto(responsable, asunto, cuerpoEmail)
        }
    }

    /**
     * Envía un email sin adjunto al responsable
     * @param responsable Email del destinatario
     * @param asunto Asunto del correo
     * @param cuerpoEmail Cuerpo del correo
     */
    private fun enviarEmailSinAdjunto(responsable: String, asunto: String, cuerpoEmail: String) {
        // Intento primario: usar Gmail
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            setData(Uri.parse("mailto:"))
            putExtra(Intent.EXTRA_EMAIL, arrayOf(responsable))
            putExtra(Intent.EXTRA_SUBJECT, asunto)
            putExtra(Intent.EXTRA_TEXT, cuerpoEmail)
            setPackage("com.google.android.gm")
        }
        try {
            startActivity(intent)
            setResultAndFinish()
        } catch (e: Exception) {
            Log.e("AsignarActivity", "No se pudo abrir Gmail, intentando con otro cliente", e)
            
            // Intento fallback: usar cualquier cliente de email
            val fallbackIntent = Intent(Intent.ACTION_SENDTO).apply {
                setData(Uri.parse("mailto:"))
                putExtra(Intent.EXTRA_EMAIL, arrayOf(responsable))
                putExtra(Intent.EXTRA_SUBJECT, asunto)
                putExtra(Intent.EXTRA_TEXT, cuerpoEmail)
            }
            try {
                startActivity(Intent.createChooser(fallbackIntent, "Enviar email de asignación"))
                setResultAndFinish()
            } catch (ex: Exception) {
                showLoading(false)
                Toast.makeText(this, "No se pudo abrir ningún cliente de email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Configura el resultado de la actividad y la termina
     * con una retroalimentación visual
     */
    private fun setResultAndFinish() {
        // Configurar resultado para la actividad que inició esta
        val intent = Intent()
        intent.putExtra("reporteId", reporteId)
        intent.putExtra("emailEnviado", true)
        intent.putExtra("navigateToAsignadoTab", true)
        setResult(RESULT_OK, intent)
        
        // Retroalimentación visual antes de terminar
        runOnUiThread {
            btnAsignar.isEnabled = false
            btnAsignar.text = "REPORTE ASIGNADO"
            btnAsignar.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            Toast.makeText(this, "Reporte asignado exitosamente", Toast.LENGTH_SHORT).show()
            Handler().postDelayed({ finish() }, 1000)
        }
    }
}