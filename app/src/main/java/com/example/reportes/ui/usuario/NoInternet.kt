package com.example.reportes.ui.usuario

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment
import com.example.reportes.R

class NoInternet : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.fragment_no_internet_dialog)
        dialog.setCancelable(false)

        dialog.window?.apply {
            // Fondo transparente para que se vea el layout
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Que ocupe toda la pantalla
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            // Respetar la barra de estado (no cubrirla con el overlay)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            // Mantener color original de la status bar
            statusBarColor = requireContext().getColor(R.color.colorAccent)

            // Evitar que cambie iconos o se oscurezca
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        // Asegurar que el ProgressBar esté animando
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)
        progressBar?.let {
            it.isIndeterminate = true
            it.visibility = View.VISIBLE
        }

        return dialog
    }
}