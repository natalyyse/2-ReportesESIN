package com.example.reportes.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.reportes.R
import com.example.reportes.data.model.Reporte

class ReporteAdp(
    private var reportes: List<Reporte>,
    private val esAdmin: Boolean,
    private val onItemClick: (Reporte) -> Unit
) : RecyclerView.Adapter<ReporteAdp.ReporteViewHolder>() {

    class ReporteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tipoHallazgo: TextView = itemView.findViewById(R.id.tvTipoHallazgo)
        val nombreReportante: TextView = itemView.findViewById(R.id.tvNombreReportante)
        val estado: TextView = itemView.findViewById(R.id.tvEstado)
        val circle: View = itemView.findViewById(R.id.circleNivelRiesgo)
        val fecha: TextView = itemView.findViewById(R.id.tvFecha)
        val fechaLimite: TextView = itemView.findViewById(R.id.tvFechaLimite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReporteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reporte_unico, parent, false)
        return ReporteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReporteViewHolder, position: Int) {
        val reporte = reportes[position]
        holder.tipoHallazgo.text = reporte.tipo
        holder.nombreReportante.text = "Reportante: ${reporte.reportante}"

        holder.estado.text = reporte.estado

        // Mostrar solo el campo de fecha correspondiente
        if (esAdmin) {
            holder.fecha.visibility = View.VISIBLE
            holder.fecha.text = "Fecha: ${reporte.fechacreacion}"
            holder.fechaLimite.visibility = View.GONE
        } else {
            holder.fechaLimite.visibility = View.VISIBLE
            holder.fechaLimite.text = "Fecha límite: ${reporte.fechaLimite}"
            holder.fecha.visibility = View.GONE
        }

        // Círculo de riesgo
        when (reporte.nivelRiesgo) {
            "Alto" -> holder.circle.setBackgroundResource(R.drawable.circle_red)
            "Medio" -> holder.circle.setBackgroundResource(R.drawable.circle_yellow)
            "Bajo" -> holder.circle.setBackgroundResource(R.drawable.circle_green)
            else -> holder.circle.setBackgroundResource(R.drawable.circle_white)
        }

        holder.itemView.setOnClickListener { onItemClick(reporte) }
    }

    override fun getItemCount() = reportes.size

    fun updateList(newList: List<Reporte>) {
        reportes = newList
        notifyDataSetChanged()
    }
}