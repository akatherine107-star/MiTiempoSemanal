package com.aguilar.tiemposemanal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aguilar.tiemposemanal.R
import com.aguilar.tiemposemanal.model.Categoria

class CategoriaAdapter(
    private var listaCategorias: List<Categoria>,
    private val getTaskCount: (Int) -> Int,
    private val onEditClick: (Categoria) -> Unit,
    private val onDeleteClick: (Categoria) -> Unit
) : RecyclerView.Adapter<CategoriaAdapter.CategoriaViewHolder>() {

    class CategoriaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNombreCategoria: TextView = itemView.findViewById(R.id.txtNombreCategoria)
        val txtCantidadTareas: TextView = itemView.findViewById(R.id.txtCantidadTareas)
        val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditar)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_categoria, parent, false)
        return CategoriaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        val categoria = listaCategorias[position]
        holder.txtNombreCategoria.text = categoria.nombre

        val count = getTaskCount(categoria.id)
        holder.txtCantidadTareas.text = if (count == 1) "1 tarea asociada" else "$count tareas asociadas"

        holder.btnEditar.setOnClickListener { onEditClick(categoria) }
        holder.btnEliminar.setOnClickListener { onDeleteClick(categoria) }
    }

    override fun getItemCount(): Int = listaCategorias.size

    fun actualizarLista(nuevaLista: List<Categoria>) {
        listaCategorias = nuevaLista
        notifyDataSetChanged()
    }
}
