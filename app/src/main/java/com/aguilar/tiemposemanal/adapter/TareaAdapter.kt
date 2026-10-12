package com.aguilar.tiemposemanal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.aguilar.tiemposemanal.R
import com.aguilar.tiemposemanal.model.Tarea

class TareaAdapter(
    private var listaTareas: List<Tarea>,
    private val onEditClick: (Tarea) -> Unit,
    private val onDeleteClick: (Tarea) -> Unit
) : RecyclerView.Adapter<TareaAdapter.TareaViewHolder>() {

    class TareaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTextoTareaConCategoria: TextView = itemView.findViewById(R.id.txtTextoTareaConCategoria)
        val chipCategoria: Chip = itemView.findViewById(R.id.chipCategoria)
        val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditar)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tarea, parent, false)
        return TareaViewHolder(view)
    }

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        val tarea = listaTareas[position]
        // Mostrar en el formato exacto requerido: "Leer Kotlin · Estudio"
        holder.txtTextoTareaConCategoria.text = "${tarea.nombre} · ${tarea.categoriaNombre}"
        holder.chipCategoria.text = tarea.categoriaNombre

        holder.btnEditar.setOnClickListener { onEditClick(tarea) }
        holder.btnEliminar.setOnClickListener { onDeleteClick(tarea) }
    }

    override fun getItemCount(): Int = listaTareas.size

    fun actualizarLista(nuevaLista: List<Tarea>) {
        listaTareas = nuevaLista
        notifyDataSetChanged()
    }
}
