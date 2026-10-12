package com.aguilar.tiemposemanal.model

data class Evento(
    val id: Int,
    val tareaId: Int,
    val tareaNombre: String,
    val categoriaNombre: String,
    val fechaInicio: String,
    val fechaFin: String?,
    val duracionSegundos: Long,
    val enCurso: Boolean
)
