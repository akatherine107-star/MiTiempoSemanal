package com.aguilar.tiemposemanal.model

data class Categoria(
    val id: Int,
    val nombre: String
) {
    override fun toString(): String {
        return nombre
    }
}
