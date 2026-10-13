package com.aguilar.tiemposemanal.model

data class ReporteSemana(
    val semanaInicio: String,
    val semanaFin: String,
    val rangoFormateado: String
) {
    override fun toString(): String {
        return rangoFormateado
    }
}
