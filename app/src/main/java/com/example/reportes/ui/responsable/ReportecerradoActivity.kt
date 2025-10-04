package com.example.reportes.ui.responsable

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.reportes.R
import com.example.reportes.ui.usuario.BaseActivity
import com.example.reportes.viewmodels.ViewModelResponsable.ReporteCerradoViewModel
import java.io.ByteArrayOutputStream

class ReportecerradoActivity : BaseActivity() {

    //region Constantes
    companion object {
        private const val ADMIN_PHONE_NUMBER = "51949711663" // Número del administrador
    }
    //endregion

    //region Vistas y ViewModel
    private lateinit var etComentarioCierre: EditText
    private lateinit var btnCerrarReporte: Button
    private lateinit var archivoNombre: TextView
    private lateinit var ivPreview: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressBackground: View

    private val viewModel: ReporteCerradoViewModel by viewModels()
    //endregion

    //region Variables de estado
    private var imageUri: Uri? = null
    private var imageName: String = ""
    private var reporteId: String? = null
    //endregion

    //region ActivityResultLaunchers para manejo de resultados de intents
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            bitmap?.let {
                val uri = getImageUriFromBitmap(it)
                handleImageSelection(uri, "IMG_${System.currentTimeMillis()}.jpg")
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleImageSelection(uri, getFileNameFromUri(uri))
            }
        }
    }

    private val smsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        Toast.makeText(this, "Levantamiento enviado exitosamente", Toast.LENGTH_LONG).show()
        finalizarActividadConExito()
    }
    //endregion

    //region Ciclo de vida y configuración inicial
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportecerrado)

        reporteId = intent.getStringExtra("reporteId")

        initViews()
        initListeners()
        observeViewModel()
    }

    private fun initViews() {
        etComentarioCierre = findViewById(R.id.etComentarioCierre)
        btnCerrarReporte = findViewById(R.id.btnCerrarReporte)
        archivoNombre = findViewById(R.id.archivoNombre)
        ivPreview = findViewById(R.id.ivPreview)
        progressBar = findViewById(R.id.progressBar)
        progressBackground = findViewById(R.id.progressBackground)
    }

    private fun initListeners() {
        findViewById<ImageView>(R.id.archivoIcon).setOnClickListener { showImagePickerDialog() }
        btnCerrarReporte.setOnClickListener { guardarReporte() }
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
    }
    //endregion

    //region Observadores de ViewModel
    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            showLoading(isLoading)
        }

        viewModel.cierreResult.observe(this) { result ->
            result.onSuccess {
                abrirMensajeriaParaEnviar()
            }.onFailure { error ->
                Toast.makeText(this, error.message ?: "Error desconocido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBackground.visibility = if (isLoading) View.VISIBLE else View.GONE
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnCerrarReporte.isEnabled = !isLoading
    }
    //endregion

    //region Lógica de negocio y UI
    private fun guardarReporte() {
        val comentario = etComentarioCierre.text.toString().trim()
        viewModel.guardarReporte(reporteId ?: "", comentario, imageUri, imageName)
    }

    private fun finalizarActividadConExito() {
        setResult(RESULT_OK)
        finish()
    }
    //endregion

    //region Selección de imagen (Cámara y Galería)
    private fun showImagePickerDialog() {
        val opciones = arrayOf("Tomar foto", "Elegir de galería")
        AlertDialog.Builder(this)
            .setTitle("Subir evidencia")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            cameraLauncher.launch(intent)
        } else {
            Toast.makeText(this, "No se encontró una app de cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        galleryLauncher.launch(intent)
    }

    private fun handleImageSelection(uri: Uri, name: String) {
        imageUri = uri
        imageName = name
        archivoNombre.text = name
        Glide.with(this).load(uri).into(ivPreview)
    }
    //endregion

    //region Utilidades de imagen y URI
    private fun getImageUriFromBitmap(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "IMG_${System.currentTimeMillis()}", null)
        return Uri.parse(path)
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.lastPathSegment
        }
        return result ?: "imagen.jpg"
    }
    //endregion

    //region Notificación por SMS
    private fun abrirMensajeriaParaEnviar() {
        val mensaje = "Se ha levantado un nuevo reporte. Por favor, estimado administrador, revisar la aplicación de Reporte ESIN."

        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$ADMIN_PHONE_NUMBER")
            putExtra("sms_body", mensaje)
        }

        try {
            smsLauncher.launch(smsIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir la app de mensajes. El reporte fue guardado.", Toast.LENGTH_LONG).show()
            finalizarActividadConExito()
        }
    }
    //endregion
}