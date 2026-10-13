package com.aguilar.tiemposemanal.model

data class EstadisticaCategoria(
    val categoriaId: Int,
    val categoriaNombre: String,
    val totalSegundos: Long
) {
    val totalHoras: Double
        get() = totalSegundos / 3600.0

    val totalMinutos: Double
        get() = totalSegundos / 60.0

    // Porcentaje sobre 168 horas (10,080 minutos)
    val porcentajeSemanal: Double
        get() = (totalMinutos / 10080.0) * 100.0
}
