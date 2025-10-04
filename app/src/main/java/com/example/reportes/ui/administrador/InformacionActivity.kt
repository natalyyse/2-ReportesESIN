package com.example.reportes.ui.administrador

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.reportes.ui.usuario.BaseActivity
import com.example.reportes.R
import com.example.reportes.viewmodels.ViewModelAdmin.InformacionViewModel
import com.github.chrisbanes.photoview.PhotoView
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.*

class InformacionActivity : BaseActivity() {
    // ViewModel para la lógica de negocio y datos de la pantalla
    private val viewModel: InformacionViewModel by viewModels()

    // Formateador de fecha reutilizable
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Vistas de la UI
    private lateinit var tvNombre: TextView
    private lateinit var tvComentario: TextView
    private lateinit var tvFecha: TextView
    private lateinit var ivImagen: ImageView
    private lateinit var btnAceptar: ImageButton
    private lateinit var btnRechazar: ImageButton
    private lateinit var progressAceptar: ProgressBar
    private lateinit var rvHistorial: RecyclerView
    private lateinit var tvNoHistorial: TextView
    private lateinit var tvAceptadoTitulo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_informacion)

        // Inicializa las vistas y obtiene el ID del reporte
        inicializarVistas()
        val reporteId = intent.getStringExtra("reporteId") ?: return

        // Configura los observadores de LiveData
        configurarObservadores(reporteId)

        // Carga los datos iniciales del reporte
        viewModel.cargarDatos(reporteId)
    }

    /**
     * Vincula las variables de la clase con las vistas del layout.
     */
    private fun inicializarVistas() {
        tvNombre = findViewById(R.id.tvNombre)
        tvComentario = findViewById(R.id.tvComentario)
        tvFecha = findViewById(R.id.tvFecha)
        ivImagen = findViewById(R.id.ivImagen)
        btnAceptar = findViewById(R.id.btnAceptar)
        btnRechazar = findViewById(R.id.btnRechazar)
        progressAceptar = findViewById(R.id.progressBar)
        rvHistorial = findViewById(R.id.rvHistorial)
        tvNoHistorial = findViewById(R.id.tvNoHistorial)
        tvAceptadoTitulo = findViewById(R.id.tvAceptadoTitulo)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
    }

    /**
     * Configura los observadores para la comunicación con el ViewModel.
     */
    private fun configurarObservadores(reporteId: String) {
        // Observador para los datos del reporte principal
        viewModel.reporteDoc.observe(this, Observer { reporteDoc ->
            tvNombre.text = reporteDoc.getString("responsable") ?: ""
        })

        // Observador para los datos del reporte de cierre
        viewModel.cerradoDoc.observe(this, Observer { cerradoDoc ->
            actualizarUIConDatosDeCierre(cerradoDoc, reporteId)
        })

        // Observador para manejar errores
        viewModel.error.observe(this, Observer { errorMsg ->
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            finish()
        })

        // Observador para acciones exitosas (aceptar/rechazar)
        viewModel.accionExitosa.observe(this, Observer { exito ->
            if (exito) {
                setResult(RESULT_OK, Intent().apply { putExtra("reporteId", reporteId) })
                finish()
            }
        })
    }

    /**
     * Actualiza la interfaz de usuario con la información del documento de cierre.
     */
    private fun actualizarUIConDatosDeCierre(cerrado: DocumentSnapshot?, reporteId: String) {
        if (cerrado == null) {
            // Caso donde no hay un reporte de cierre aún
            mostrarEstadoSinLevantamiento()
            return
        }

        // Extrae datos del documento de cierre
        val comentario = cerrado.getString("comentario") ?: ""
        val fechaLevantamiento = cerrado.getString("fechalevantamiento") ?: ""
        val evidenciaUrl = cerrado.getString("imagenUrl")
        val estadoCierre = cerrado.getString("estadoCierre")
        val motivoRechazo = cerrado.getString("motivo")
        val fechaRechazo = cerrado.getString("fecharechazo")
        val cerradoId = cerrado.id

        // Actualiza la UI con los datos generales
        tvComentario.text = comentario
        tvFecha.text = fechaLevantamiento
        cargarImagenEvidencia(evidenciaUrl)

        // Configura la visibilidad y acción de los botones Aceptar/Rechazar
        configurarBotonesDeAccion(estadoCierre, cerradoId, reporteId)

        // Muestra el historial de rechazos
        configurarHistorial(motivoRechazo, fechaRechazo, estadoCierre)

        // Muestra un título si el reporte ya fue aceptado
        tvAceptadoTitulo.visibility = if (estadoCierre.equals("aceptado", ignoreCase = true)) View.VISIBLE else View.GONE
    }

    /**
     * Muestra un estado inicial cuando no se ha enviado un levantamiento.
     */
    private fun mostrarEstadoSinLevantamiento() {
        tvComentario.text = "Aún no se ha enviado un levantamiento para este reporte."
        ivImagen.visibility = View.GONE
        btnAceptar.visibility = View.GONE
        btnRechazar.visibility = View.GONE
        tvFecha.text = ""
    }

    /**
     * Carga la imagen de evidencia usando Glide.
     */
    private fun cargarImagenEvidencia(url: String?) {
        ivImagen.visibility = View.VISIBLE
        if (!url.isNullOrEmpty()) {
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(ivImagen)
            ivImagen.setOnClickListener { showImagePreview(url) }
        } else {
            ivImagen.setImageResource(R.drawable.ic_image_placeholder)
            Toast.makeText(this, "No hay imagen de evidencia.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Decide si mostrar los botones de acción y configura sus listeners.
     */
    private fun configurarBotonesDeAccion(estadoCierre: String?, cerradoId: String, reporteId: String) {
        val levantamientoEnviado = viewModel.reporteDoc.value?.getBoolean("levantamientoEnviado") ?: false
        val mostrarBotones = estadoCierre.equals("pendiente", ignoreCase = true) ||
                (estadoCierre.equals("rechazado", ignoreCase = true) && levantamientoEnviado)

        if (mostrarBotones) {
            btnAceptar.visibility = View.VISIBLE
            btnRechazar.visibility = View.VISIBLE
            btnAceptar.setOnClickListener {
                progressAceptar.visibility = View.VISIBLE
                viewModel.aceptarCierre(cerradoId, reporteId)
            }
            btnRechazar.setOnClickListener {
                mostrarDialogoRechazo(cerradoId, reporteId)
            }
        } else {
            btnAceptar.visibility = View.GONE
            btnRechazar.visibility = View.GONE
        }
    }

    /**
     * Muestra el historial de rechazos en el RecyclerView.
     */
    private fun configurarHistorial(motivo: String?, fecharechazo: String?, estadoCierre: String?) {
        rvHistorial.layoutManager = LinearLayoutManager(this)
        if (!motivo.isNullOrEmpty() && !fecharechazo.isNullOrEmpty()) {
            val historialItems = listOf(mapOf("motivo" to motivo, "fecharechazo" to fecharechazo))
            rvHistorial.adapter = HistorialAdapter(historialItems)
            rvHistorial.visibility = View.VISIBLE
            tvNoHistorial.visibility = View.GONE
        } else {
            rvHistorial.visibility = View.GONE
            val mostrarMensajeNoHistorial = !estadoCierre.equals("aceptado", ignoreCase = true)
            tvNoHistorial.visibility = if (mostrarMensajeNoHistorial) View.VISIBLE else View.GONE
            tvNoHistorial.text = "No hay rechazos anteriores"
        }
    }

    /**
     * Muestra el diálogo para ingresar el motivo del rechazo y opcionalmente una nueva fecha.
     */
    private fun mostrarDialogoRechazo(cerradoId: String, reporteId: String) {
        val reporteDoc = viewModel.reporteDoc.value ?: return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rechazo, null)
        val etMotivo = dialogView.findViewById<EditText>(R.id.etMotivo)
        val etFechaLimite = dialogView.findViewById<EditText>(R.id.etFechaLimite)

        val fechaLimiteActualStr = reporteDoc.getString("fechaLimite") ?: ""
        var nuevaFechaLimite = fechaLimiteActualStr
        var seCambioFecha = false

        // Determina si se debe mostrar el campo para cambiar la fecha límite
        if (haVencidoFechaLimite(fechaLimiteActualStr)) {
            etFechaLimite.visibility = View.VISIBLE
            val fechaActual = Date()
            etFechaLimite.setText(sdf.format(fechaActual))
            nuevaFechaLimite = sdf.format(fechaActual)
            seCambioFecha = true
            etFechaLimite.setOnClickListener {
                mostrarDatePicker(etFechaLimite) { fechaSeleccionada ->
                    nuevaFechaLimite = fechaSeleccionada
                }
            }
        } else {
            etFechaLimite.visibility = View.GONE
        }

        // Construye y muestra el AlertDialog
        AlertDialog.Builder(this)
            .setTitle("Motivo del rechazo")
            .setView(dialogView)
            .setPositiveButton("Enviar") { _, _ ->
                val motivo = etMotivo.text.toString().trim()
                if (motivo.isNotEmpty()) {
                    procesarRechazo(cerradoId, reporteId, motivo, seCambioFecha, nuevaFechaLimite, fechaLimiteActualStr)
                } else {
                    Toast.makeText(this, "Debes ingresar un motivo", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Llama al ViewModel para rechazar el cierre y envía el correo de notificación.
     */
    private fun procesarRechazo(cerradoId: String, reporteId: String, motivo: String, seCambioFecha: Boolean, nuevaFechaLimite: String, fechaLimiteActualStr: String) {
        val reporteDoc = viewModel.reporteDoc.value ?: return
        val correoResponsable = reporteDoc.getString("responsable") ?: ""
        val descripcionReporte = reporteDoc.getString("descripcion") ?: ""
        val fechaRechazo = sdf.format(Date())

        viewModel.rechazarCierre(
            cerradoId,
            reporteId,
            motivo,
            fechaRechazo,
            if (seCambioFecha) nuevaFechaLimite else null,
            seCambioFecha
        )
        enviarCorreoRechazo(
            correoResponsable,
            reporteId,
            motivo,
            if (seCambioFecha) nuevaFechaLimite else fechaLimiteActualStr,
            seCambioFecha,
            descripcionReporte
        )
    }

    /**
     * Verifica si la fecha límite del reporte ya ha pasado.
     */
    private fun haVencidoFechaLimite(fechaLimiteStr: String): Boolean {
        val fechaLimiteDate = try { sdf.parse(fechaLimiteStr) } catch (e: Exception) { null }
        if (fechaLimiteDate != null) {
            val calLimite = Calendar.getInstance().apply { time = fechaLimiteDate; add(Calendar.DAY_OF_YEAR, 1) }
            val calHoy = Calendar.getInstance()
            // Normalizar horas para comparar solo fechas
            calLimite.set(Calendar.HOUR_OF_DAY, 0)
            calHoy.set(Calendar.HOUR_OF_DAY, 0)
            return !calHoy.before(calLimite)
        }
        return false // Si no hay fecha límite, no se considera vencida
    }

    /**
     * Muestra un DatePickerDialog para seleccionar una fecha.
     */
    private fun mostrarDatePicker(editText: EditText, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val fechaFormateada = sdf.format(calendar.time)
                editText.setText(fechaFormateada)
                onDateSelected(fechaFormateada)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000 // Inicia desde hoy
        datePicker.show()
    }

    /**
     * Construye y lanza un Intent para enviar un correo de rechazo.
     */
    private fun enviarCorreoRechazo(correo: String, reporteId: String, motivo: String, fechaLimite: String, esNuevaFecha: Boolean, descripcion: String) {
        val asunto = "Rechazo del levantamiento del reporte"
        val fechaTexto = if (esNuevaFecha) "Nueva Fecha Límite" else "Fecha Límite"
        val cuerpo = """
            Estimado/a Responsable,

            Nos dirigimos a usted en relación al proceso de cierre del reporte realizado.
            Tras la revisión correspondiente, se han encontrado algunas observaciones que requieren su atención.

            Descripción del Reporte: $descripcion

            Motivo del Rechazo: $motivo

            $fechaTexto: $fechaLimite

            Por favor, revise la evidencia y realice las correcciones necesarias a través de la aplicación móvil para proceder con el levantamiento definitivo del reporte.

            Agradecemos de antemano su colaboración.

            Atentamente,

            SISTEMA DE GESTIÓN DE REPORTES ESIN
        """.trimIndent()

        // Intent principal para Gmail
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(correo))
            putExtra(Intent.EXTRA_SUBJECT, asunto)
            putExtra(Intent.EXTRA_TEXT, cuerpo)
            setPackage("com.google.android.gm")
        }

        // Intent de fallback para otros clientes de correo
        val fallbackIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(correo))
            putExtra(Intent.EXTRA_SUBJECT, asunto)
            putExtra(Intent.EXTRA_TEXT, cuerpo)
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e("InformacionActivity", "Gmail no encontrado, intentando con otro cliente.", e)
            try {
                startActivity(Intent.createChooser(fallbackIntent, "Enviar correo de rechazo"))
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(this, "No se encontró una aplicación de correo.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Muestra un diálogo con la imagen en pantalla completa para hacer zoom.
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
        Glide.with(this).load(imageUrl).into(photoView)

        dialog.findViewById<ImageButton>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.show()
    }

    /**
     * Adaptador para el RecyclerView que muestra el historial de rechazos.
     */
    private class HistorialAdapter(private val items: List<Map<String, Any>>) :
        RecyclerView.Adapter<HistorialAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTipo: TextView = view.findViewById(R.id.tvTipo)
            val tvMotivo: TextView = view.findViewById(R.id.tvMotivoHist)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_historial, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTipo.text = "Rechazado"
            val motivo = item["motivo"]?.toString() ?: ""
            if (motivo.isNotEmpty()) {
                holder.tvMotivo.text = "Motivo: $motivo"
            } else {
                holder.tvMotivo.text = ""
            }
        }

        override fun getItemCount() = items.size
    }
}