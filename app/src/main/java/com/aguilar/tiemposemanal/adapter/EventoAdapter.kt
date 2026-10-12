package com.aguilar.tiemposemanal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.aguilar.tiemposemanal.R
import com.aguilar.tiemposemanal.model.Evento

class EventoAdapter(
    private var listaEventos: List<Evento>
) : RecyclerView.Adapter<EventoAdapter.EventoViewHolder>() {

    class EventoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNombreTarea: TextView = itemView.findViewById(R.id.txtNombreTarea)
        val chipEstado: Chip = itemView.findViewById(R.id.chipEstado)
        val chipCategoria: Chip = itemView.findViewById(R.id.chipCategoria)
        val txtInicio: TextView = itemView.findViewById(R.id.txtInicio)
        val txtFin: TextView = itemView.findViewById(R.id.txtFin)
        val txtDuracion: TextView = itemView.findViewById(R.id.txtDuracion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evento, parent, false)
        return EventoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        val evento = listaEventos[position]
        holder.txtNombreTarea.text = evento.tareaNombre
        holder.chipCategoria.text = evento.categoriaNombre

        if (evento.enCurso) {
            holder.chipEstado.text = "En curso"
            holder.chipEstado.setChipBackgroundColorResource(R.color.md_theme_primaryContainer)
            holder.chipEstado.setTextColor(holder.itemView.context.getColor(R.color.md_theme_onPrimaryContainer))

            holder.txtInicio.text = "Inicio: ${evento.fechaInicio}"
            holder.txtFin.text = "Fin: (En curso...)"
            holder.txtDuracion.text = "Duración: Registrando..."
        } else {
            holder.chipEstado.text = "Finalizado"
            holder.chipEstado.setChipBackgroundColorResource(R.color.md_theme_secondaryContainer)
            holder.chipEstado.setTextColor(holder.itemView.context.getColor(R.color.md_theme_onSecondaryContainer))

            holder.txtInicio.text = "Inicio: ${evento.fechaInicio}"
            holder.txtFin.text = "Fin: ${evento.fechaFin ?: "-"}"
            holder.txtDuracion.text = "Duración: ${formatearDuracion(evento.duracionSegundos)}"
        }
    }

    override fun getItemCount(): Int = listaEventos.size

    fun actualizarLista(nuevaLista: List<Evento>) {
        listaEventos = nuevaLista
        notifyDataSetChanged()
    }

    private fun formatearDuracion(segundosTotales: Long): String {
        if (segundosTotales <= 0) return "0 seg"
        val horas = segundosTotales / 3600
        val minutos = (segundosTotales % 3600) / 60
        val segundos = segundosTotales % 60

        return when {
            horas > 0 -> "${horas}h ${minutos}m ${segundos}s"
            minutos > 0 -> "${minutos}m ${segundos}s"
            else -> "${segundos}s"
        }
    }
}
