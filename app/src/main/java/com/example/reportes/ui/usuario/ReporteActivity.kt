package com.example.reportes.ui.usuario

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.reportes.R
import com.example.reportes.viewmodels.ViewModelUsuario.ReporteViewModel
import java.io.File

class ReporteActivity : BaseActivity() {

    // --- Constantes ---
    companion object {
        private const val REQUEST_IMAGE_PICK = 1001
        private const val REQUEST_IMAGE_CAPTURE = 1002
    }

    // --- ViewModel ---
    private val viewModel: ReporteViewModel by viewModels()

    // --- Variables de UI ---
    private lateinit var spinnerTipo: Spinner
    private lateinit var etDescripcion: EditText
    private lateinit var etLugar: EditText
    private lateinit var etNombre: EditText
    private lateinit var archivoNombre: TextView
    private lateinit var archivoPreview: ImageView
    private lateinit var progressBackground: View
    private lateinit var progressBar: ProgressBar
    private lateinit var btnEnviar: Button

    // --- Variables para manejo de imagen ---
    private var selectedImageUri: Uri? = null
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reporte)

        // --- Inicialización de Vistas ---
        inicializarVistas()

        // --- Configuración de Spinners ---
        inicializarSpinners()

        // --- Configuración de Listeners ---
        configurarListeners()

        // --- Observadores de ViewModel ---
        configurarObservadores()
    }

    // --- Inicializa todas las referencias a las vistas del layout ---
    private fun inicializarVistas() {
        spinnerTipo = findViewById(R.id.spinnerTipo)
        etDescripcion = findViewById(R.id.etDescripcion)
        etLugar = findViewById(R.id.etLugar)
        etNombre = findViewById(R.id.etNombre)
        archivoNombre = findViewById(R.id.archivoNombre)
        archivoPreview = findViewById(R.id.archivoPreview)
        progressBackground = findViewById(R.id.progressBackground)
        progressBar = findViewById(R.id.progressBar)
        btnEnviar = findViewById(R.id.btnEnviar)

        // Cargar íconos desde drawables
        findViewById<ImageView>(R.id.archivoIcon).setImageResource(R.drawable.ic_upload)
        archivoPreview.setImageResource(R.drawable.ic_image_placeholder)
    }

    // --- Configura los listeners para los elementos interactivos de la UI ---
    private fun configurarListeners() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.archivoIcon).setOnClickListener { mostrarDialogoSeleccionImagen() }

        btnEnviar.setOnClickListener {
            val tipo = spinnerTipo.selectedItem.toString()
            val descripcion = etDescripcion.text.toString()
            val lugar = etLugar.text.toString()
            val reportante = etNombre.text.toString()

            if (tipo == "Seleccionar" || descripcion.isEmpty() || lugar.isEmpty() || reportante.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Delegar la lógica de envío al ViewModel
            viewModel.enviarReporte(tipo, descripcion, lugar, reportante, selectedImageUri)
        }
    }

    // --- Configura los observadores para reaccionar a los cambios del ViewModel ---
    private fun configurarObservadores() {
        viewModel.loading.observe(this, Observer { loading ->
            progressBackground.visibility = if (loading) View.VISIBLE else View.GONE
            progressBar.visibility = if (loading) ProgressBar.VISIBLE else ProgressBar.GONE
            btnEnviar.isEnabled = !loading
        })

        viewModel.mensaje.observe(this, Observer { mensaje ->
            if (mensaje.isNotEmpty()) Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
        })

        viewModel.reporteGuardado.observe(this, Observer { guardado ->
            if (guardado) {
                limpiarCampos()
            }
        })
    }

    // --- Inicializa los spinners con sus opciones ---
    private fun inicializarSpinners() {
        val opcionesTipo = listOf("Seleccionar", "Condición insegura", "Acto inseguro")
        val adapterTipo = ArrayAdapter(this, R.layout.spinner_item, opcionesTipo)
        adapterTipo.setDropDownViewResource(R.layout.spinner_item)
        spinnerTipo.adapter = adapterTipo
    }

    // --- Muestra diálogo para seleccionar imagen desde galería o cámara ---
    private fun mostrarDialogoSeleccionImagen() {
        val opciones = arrayOf("Seleccionar de galería", "Tomar foto")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar imagen")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> abrirGaleria()
                    1 -> abrirCamara()
                }
            }
            .show()
    }

    // --- Inicia el intent para seleccionar una imagen de la galería ---
    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    // --- Inicia el intent para capturar una imagen con la cámara ---
    private fun abrirCamara() {
        crearArchivoImagen()?.let { uri ->
            photoUri = uri
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            }
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }
    }

    // --- Crea un archivo temporal para la foto tomada con la cámara ---
    private fun crearArchivoImagen(): Uri? {
        return try {
            val imageFileName = "JPEG_${System.currentTimeMillis()}_"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val image = File.createTempFile(imageFileName, ".jpg", storageDir)
            FileProvider.getUriForFile(this, "${packageName}.fileprovider", image)
        } catch (ex: Exception) {
            Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // --- Maneja el resultado de selección de imagen o foto tomada ---
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return

        when (requestCode) {
            REQUEST_IMAGE_PICK -> {
                selectedImageUri = data?.data
                actualizarPreviewImagen("Imagen seleccionada")
            }
            REQUEST_IMAGE_CAPTURE -> {
                selectedImageUri = photoUri
                actualizarPreviewImagen("Foto tomada")
            }
        }
    }

    // --- Actualiza la UI con la imagen seleccionada ---
    private fun actualizarPreviewImagen(mensaje: String) {
        archivoNombre.text = obtenerNombreArchivo(selectedImageUri)
        Glide.with(this)
            .load(selectedImageUri)
            .placeholder(R.drawable.ic_image_placeholder)
            .into(archivoPreview)
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    // --- Obtiene el nombre del archivo a partir de su Uri ---
    private fun obtenerNombreArchivo(uri: Uri?): String {
        if (uri == null) return ""
        var nombre = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    nombre = cursor.getString(nameIndex)
                }
            }
        }
        if (nombre.isEmpty()) {
            nombre = uri.lastPathSegment ?: "archivo.jpg"
        }
        return nombre
    }

    // --- Limpia los campos del formulario tras enviar el reporte ---
    private fun limpiarCampos() {
        etDescripcion.text.clear()
        etLugar.text.clear()
        etNombre.text.clear()
        archivoNombre.text = "Seleccionar archivo"
        archivoPreview.setImageResource(R.drawable.ic_image_placeholder)
        selectedImageUri = null
        photoUri = null
        spinnerTipo.setSelection(0)
    }
}