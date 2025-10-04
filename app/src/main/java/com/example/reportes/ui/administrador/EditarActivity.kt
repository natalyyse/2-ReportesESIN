package com.example.reportes.ui.administrador

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.example.reportes.ui.usuario.BaseActivity
import com.example.reportes.R
import com.example.reportes.viewmodels.ViewModelAdmin.EditarViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Activity para la edición de fechas límite de reportes.
 * Permite modificar fechas límite de reportes asignados o reabrir reportes cerrados parcialmente.
 */
class EditarActivity : BaseActivity() {
    // Variables de datos del reporte
    private var responsableEmail: String = ""
    private var reporteId: String? = null
    private var fechaAsignacionStr: String = ""
    private var fechaLimiteOriginalStr: String = ""
    private var esReporteCerradoParcial: Boolean = false
    
    // Formato de fecha utilizado en toda la actividad
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    // ViewModel para gestionar los datos y operaciones con la base de datos
    private val viewModel: EditarViewModel by viewModels()

    // UI Elements
    private lateinit var etFechaLimite: EditText
    private lateinit var etMotivo: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar)

        // Recuperar datos del intent
        reporteId = intent.getStringExtra("reporteId")
        esReporteCerradoParcial = intent.getBooleanExtra("esReporteCerradoParcial", false)

        // Inicializar vistas
        initializeViews()
        
        // Configurar observadores del ViewModel
        setupObservers()
        
        // Cargar datos del reporte
        if (!reporteId.isNullOrEmpty()) {
            viewModel.cargarDatosReporte(reporteId!!)
        }
    }

    /**
     * Inicializa y configura todas las vistas de la UI
     */
    private fun initializeViews() {
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val tvTipo = findViewById<TextView>(R.id.tvTipo)
        etMotivo = findViewById(R.id.etMotivo)
        etFechaLimite = findViewById(R.id.etFechaLimite)
        val btnEnviar = findViewById<Button>(R.id.btnEnviar)
        val tvTitulo = findViewById<TextView>(R.id.tvTituloEditar)

        // Cambiar título si es reporte cerrado parcial
        if (esReporteCerradoParcial) {
            tvTitulo.text = "Reasignación de fecha"
        }

        // Configurar botón de regreso
        btnBack.setOnClickListener { finish() }

        // Configurar selector de fecha
        etFechaLimite.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                val editText = v as EditText
                if (editText.compoundDrawables[drawableEnd] != null) {
                    val iconStart = editText.width - editText.paddingEnd - editText.compoundDrawables[drawableEnd]!!.bounds.width()
                    if (event.x >= iconStart) {
                        mostrarDatePicker()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        // Configurar botón de envío
        btnEnviar.setOnClickListener {
            validarYEnviar()
        }
    }

    /**
     * Configura los observadores para los LiveData del ViewModel
     */
    private fun setupObservers() {
        // Observar datos del reporte
        viewModel.datosReporte.observe(this, Observer { datos ->
            procesarDatosRecibidos(datos)
        })

        // Observar resultado exitoso
        viewModel.success.observe(this, Observer { ok ->
            if (ok == true) {
                procesarActualizacionExitosa()
            }
        })

        // Observar errores
        viewModel.error.observe(this, Observer { errorMsg ->
            errorMsg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.limpiarEstado()
            }
        })
    }

    /**
     * Procesa y muestra los datos recibidos del reporte
     */
    private fun procesarDatosRecibidos(datos: Map<String, String>) {
        val tvTipo = findViewById<TextView>(R.id.tvTipo)
        tvTipo.text = datos["tipo"] ?: ""
        responsableEmail = datos["responsable"] ?: ""
        fechaAsignacionStr = datos["fechaAsignacion"] ?: ""
        fechaLimiteOriginalStr = datos["fechaLimite"] ?: ""
        
        // Establecer fecha en el campo según el tipo de reporte
        if (esReporteCerradoParcial) {
            // Para reportes cerrados parciales, usar la fecha actual
            etFechaLimite.setText(sdf.format(Date()))
        } else if (fechaLimiteOriginalStr.isNotEmpty()) {
            try {
                // Para reportes normales, usar un día antes de la fecha límite original
                val cal = Calendar.getInstance()
                cal.time = sdf.parse(fechaLimiteOriginalStr) ?: return
                cal.add(Calendar.DAY_OF_MONTH, -1)
                etFechaLimite.setText(sdf.format(cal.time))
            } catch (_: Exception) {
                etFechaLimite.setText(fechaLimiteOriginalStr)
            }
        } else {
            etFechaLimite.setText(fechaLimiteOriginalStr)
        }
    }

    /**
     * Muestra el selector de fecha adecuado según el tipo de reporte
     */
    private fun mostrarDatePicker() {
        if (esReporteCerradoParcial) {
            mostrarDatePickerSinRestricciones()
        } else {
            mostrarDatePickerConRestricciones()
        }
    }

    /**
     * Muestra un DatePicker con restricciones para reportes normales
     */
    private fun mostrarDatePickerConRestricciones() {
        if (fechaAsignacionStr.isEmpty() || fechaLimiteOriginalStr.isEmpty()) {
            Toast.makeText(this, "No se pudieron obtener las fechas necesarias", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Inicializar calendarios para manejo de fechas
            val cal = obtenerCalendarioActual()
            
            // Obtener fechas relevantes normalizadas
            val fechaActual = obtenerFechaActualNormalizada()
            val fechaLimiteOriginal = obtenerFechaLimiteOriginalNormalizada()
            
            // Verificar si ya pasó la fecha límite original
            if (fechaActual.after(fechaLimiteOriginal)) {
                Toast.makeText(this, "No es posible modificar la fecha límite ya que ha pasado la fecha límite original", 
                    Toast.LENGTH_LONG).show()
                return
            }
            
            // Configurar y mostrar el DatePickerDialog
            mostrarDatePickerDialog(
                cal,
                fechaActual.timeInMillis,
                obtenerFechaMaximaPermitida(fechaLimiteOriginal).timeInMillis,
                { selectedCal ->
                    validarFechaSeleccionada(selectedCal, fechaActual, fechaLimiteOriginal)
                }
            )
            
        } catch (e: Exception) {
            Log.e("EditarActivity", "Error al configurar DatePicker con restricciones", e)
            Toast.makeText(this, "Error al configurar el selector de fecha", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Muestra un DatePicker sin restricciones máximas para reportes cerrados parcialmente
     */
    private fun mostrarDatePickerSinRestricciones() {
        try {
            // Inicializar calendario para la fecha actual
            val cal = obtenerCalendarioActual()
            
            // Obtener fecha actual normalizada
            val fechaActual = obtenerFechaActualNormalizada()
            
            // Configurar y mostrar el DatePickerDialog
            mostrarDatePickerDialog(
                cal,
                fechaActual.timeInMillis,
                null, // Sin fecha máxima
                { selectedCal ->
                    // Solo validar que no sea anterior a hoy
                    if (selectedCal.before(fechaActual)) {
                        Toast.makeText(
                            this,
                            "No puedes seleccionar una fecha anterior a hoy",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        etFechaLimite.setText(sdf.format(selectedCal.time))
                    }
                }
            )
            
        } catch (e: Exception) {
            Log.e("EditarActivity", "Error al configurar DatePicker sin restricciones", e)
            Toast.makeText(this, "Error al configurar el selector de fecha", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Configura y muestra un DatePickerDialog con los parámetros especificados
     */
    private fun mostrarDatePickerDialog(
        cal: Calendar,
        minDate: Long,
        maxDate: Long?,
        onDateSelected: (Calendar) -> Unit
    ) {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = DatePickerDialog(this, { _, y, m, d ->
            val selectedCal = Calendar.getInstance()
            selectedCal.set(y, m, d)
            selectedCal.set(Calendar.HOUR_OF_DAY, 0)
            selectedCal.set(Calendar.MINUTE, 0)
            selectedCal.set(Calendar.SECOND, 0)
            selectedCal.set(Calendar.MILLISECOND, 0)
            
            onDateSelected(selectedCal)
        }, year, month, day)
        
        // Establecer fecha mínima
        datePickerDialog.datePicker.minDate = minDate
        
        // Establecer fecha máxima si está especificada
        maxDate?.let { datePickerDialog.datePicker.maxDate = it }
        
        // Mostrar el DatePicker
        datePickerDialog.show()
    }

    /**
     * Obtiene un calendario con la fecha actual configurada desde el campo de texto
     */
    private fun obtenerCalendarioActual(): Calendar {
        val cal = Calendar.getInstance()
        val fechaActualStr = etFechaLimite.text.toString()
        if (fechaActualStr.isNotEmpty()) {
            try {
                val fecha = sdf.parse(fechaActualStr)
                fecha?.let { cal.time = it }
            } catch (e: Exception) {
                Log.e("EditarActivity", "Error al parsear fecha actual", e)
            }
        }
        return cal
    }

    /**
     * Obtiene un calendario normalizado para la fecha actual
     */
    private fun obtenerFechaActualNormalizada(): Calendar {
        val fechaActual = Calendar.getInstance()
        fechaActual.set(Calendar.HOUR_OF_DAY, 0)
        fechaActual.set(Calendar.MINUTE, 0)
        fechaActual.set(Calendar.SECOND, 0)
        fechaActual.set(Calendar.MILLISECOND, 0)
        return fechaActual
    }

    /**
     * Obtiene un calendario normalizado para la fecha límite original
     */
    private fun obtenerFechaLimiteOriginalNormalizada(): Calendar {
        val fechaLimiteOriginal = Calendar.getInstance()
        fechaLimiteOriginal.time = sdf.parse(fechaLimiteOriginalStr) ?: Date()
        fechaLimiteOriginal.set(Calendar.HOUR_OF_DAY, 23)
        fechaLimiteOriginal.set(Calendar.MINUTE, 59)
        fechaLimiteOriginal.set(Calendar.SECOND, 59)
        fechaLimiteOriginal.set(Calendar.MILLISECOND, 999)
        return fechaLimiteOriginal
    }

    /**
     * Calcula la fecha máxima permitida (un día antes de la fecha límite original)
     */
    private fun obtenerFechaMaximaPermitida(fechaLimiteOriginal: Calendar): Calendar {
        val fechaMaxima = Calendar.getInstance()
        fechaMaxima.time = fechaLimiteOriginal.time
        fechaMaxima.add(Calendar.DAY_OF_MONTH, -1)
        return fechaMaxima
    }

    /**
     * Valida que la fecha seleccionada cumpla con las restricciones
     */
    private fun validarFechaSeleccionada(
        selectedCal: Calendar,
        fechaActual: Calendar,
        fechaLimiteOriginal: Calendar
    ) {
        when {
            selectedCal.before(fechaActual) -> {
                Toast.makeText(
                    this,
                    "No puedes seleccionar una fecha anterior a hoy",
                    Toast.LENGTH_SHORT
                ).show()
            }
            selectedCal.after(fechaLimiteOriginal) -> {
                Toast.makeText(
                    this,
                    "No puedes seleccionar una fecha posterior a la fecha límite original",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                etFechaLimite.setText(sdf.format(selectedCal.time))
            }
        }
    }

    /**
     * Valida todos los campos y envía la actualización
     */
    private fun validarYEnviar() {
        val motivo = etMotivo.text.toString().trim()
        val fechaLimiteStr = etFechaLimite.text.toString().trim()
        
        // Validar campos obligatorios
        if (motivo.isEmpty() || fechaLimiteStr.isEmpty() || responsableEmail.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validar fechas
        try {
            val fechaLimite = Calendar.getInstance()
            fechaLimite.time = sdf.parse(fechaLimiteStr) ?: return
            
            val fechaActual = obtenerFechaActualNormalizada()
            
            if (fechaLimite.before(fechaActual)) {
                Toast.makeText(this, "La fecha límite no puede ser anterior a hoy", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Si no es reporte cerrado parcial, validar contra fecha límite original
            if (!esReporteCerradoParcial) {
                val fechaLimiteOriginal = obtenerFechaLimiteOriginalNormalizada()
                
                if (fechaLimite.after(fechaLimiteOriginal)) {
                    Toast.makeText(this, "La fecha no puede ser posterior a la fecha límite original", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            
            // Todo validado, actualizar fecha límite
            viewModel.actualizarFechaLimite(reporteId!!, fechaLimiteStr, esReporteCerradoParcial)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error al validar fechas", Toast.LENGTH_SHORT).show()
            Log.e("EditarActivity", "Error en validación de fechas", e)
        }
    }

    /**
     * Procesa una actualización exitosa enviando correo electrónico
     */
    private fun procesarActualizacionExitosa() {
        val motivo = etMotivo.text.toString().trim()
        val fechaLimiteStr = etFechaLimite.text.toString().trim()
        
        if (esReporteCerradoParcial) {
            enviarCorreo(
                responsableEmail,
                motivo,
                fechaLimiteStr,
                true
            )
            Toast.makeText(this, "Fecha actualizada ", Toast.LENGTH_LONG).show()
        } else {
            enviarCorreo(
                responsableEmail,
                motivo,
                fechaLimiteStr,
                false
            )
            Toast.makeText(this, "Fecha actualizada", Toast.LENGTH_LONG).show()
        }
        
        viewModel.limpiarEstado()
        finish()
    }

    /**
     * Envía un correo electrónico al responsable con la información de la actualización
     * @param email Dirección de correo del responsable
     * @param motivo Motivo del cambio
     * @param fechaLimiteStr Nueva fecha límite
     * @param esFechaVencida Indica si es para un reporte con fecha vencida
     */
    private fun enviarCorreo(
        email: String, 
        motivo: String, 
        fechaLimiteStr: String,
        esFechaVencida: Boolean
    ) {
        // Preparar datos del correo
        val asunto = if (esFechaVencida) 
            "Reporte con fecha límite vencida" 
        else 
            "Actualización de fecha límite - Reporte asignado"
            
        val cuerpoEmail = if (esFechaVencida) {
            """
                Estimado/a responsable,

                Se ha habilitado una nueva oportunidad para atender este reporte.

                Motivo del cambio: $motivo
                
                Nueva fecha límite: $fechaLimiteStr

                Por favor, complete el levantamiento del reporte antes de la nueva fecha límite.

                Atentamente,
                SISTEMA DE GESTIÓN DE REPORTES ESIN
               
                NOTA: El levantamiento del reporte debe realizarse exclusivamente por medio de la aplicación móvil del sistema.
            """.trimIndent()
        } else {
            """
                Estimado/a responsable,
                
                Se ha realizado una modificación en la fecha límite del reporte asignado a usted.
                
                Motivo del cambio: $motivo
                
                Nueva fecha límite: $fechaLimiteStr
                
                Por favor, complete el levantamiento del reporte antes de la nueva fecha límite.
                
                Atentamente,
                SISTEMA DE GESTIÓN DE REPORTES ESIN
               
                NOTA: El levantamiento del reporte debe realizarse exclusivamente por medio de la aplicación móvil del sistema.
            """.trimIndent()
        }

        // Preparar el intent de resultado
        val resultIntent = Intent()
        resultIntent.putExtra("reporteId", reporteId)
        resultIntent.putExtra("estadoActualizado", true)
        setResult(RESULT_OK, resultIntent)

        // Crear intent para enviar correo con Gmail
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            setData(Uri.parse("mailto:"))
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, asunto)
            putExtra(Intent.EXTRA_TEXT, cuerpoEmail)
            setPackage("com.google.android.gm")
        }
        
        // Intentar enviar por Gmail o cualquier cliente de correo
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("EditarActivity", "No se pudo abrir Gmail, intentando con otro cliente", e)
            val fallbackIntent = Intent(Intent.ACTION_SENDTO).apply {
                setData(Uri.parse("mailto:"))
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, asunto)
                putExtra(Intent.EXTRA_TEXT, cuerpoEmail)
            }
            try {
                startActivity(Intent.createChooser(fallbackIntent, "Enviar correo de actualización"))
            } catch (ex: Exception) {
                Toast.makeText(this, "No se encontró la app de correo", Toast.LENGTH_SHORT).show()
            }
        }
    }
}