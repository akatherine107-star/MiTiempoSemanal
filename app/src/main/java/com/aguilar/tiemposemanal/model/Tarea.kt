package com.aguilar.tiemposemanal.model

data class Tarea(
    val id: Int,
    val nombre: String,
    val categoriaId: Int,
    val categoriaNombre: String
)
