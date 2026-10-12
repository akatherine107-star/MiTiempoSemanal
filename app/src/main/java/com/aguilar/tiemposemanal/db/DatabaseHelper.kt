package com.aguilar.tiemposemanal.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.aguilar.tiemposemanal.model.Categoria
import com.aguilar.tiemposemanal.model.Tarea

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "mitiemposemanal.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_CATEGORIAS = "categorias"
        private const val COL_CAT_ID = "id"
        private const val COL_CAT_NOMBRE = "nombre"

        private const val TABLE_TAREAS = "tareas"
        private const val COL_TAREA_ID = "id"
        private const val COL_TAREA_NOMBRE = "nombre"
        private const val COL_TAREA_CAT_ID = "categoria_id"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableCategorias = """
            CREATE TABLE $TABLE_CATEGORIAS (
                $COL_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CAT_NOMBRE TEXT UNIQUE NOT NULL
            )
        """.trimIndent()

        val createTableTareas = """
            CREATE TABLE $TABLE_TAREAS (
                $COL_TAREA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TAREA_NOMBRE TEXT UNIQUE NOT NULL,
                $COL_TAREA_CAT_ID INTEGER NOT NULL,
                FOREIGN KEY($COL_TAREA_CAT_ID) REFERENCES $TABLE_CATEGORIAS($COL_CAT_ID) ON DELETE RESTRICT
            )
        """.trimIndent()

        db.execSQL(createTableCategorias)
        db.execSQL(createTableTareas)

        // Insertar las 4 categorías por defecto al instalar la app
        val categoriasIniciales = listOf("Estudio", "Trabajo", "Descanso", "Ocio")
        for (cat in categoriasIniciales) {
            val values = ContentValues().apply {
                put(COL_CAT_NOMBRE, cat.trim())
            }
            db.insert(TABLE_CATEGORIAS, null, values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TAREAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIAS")
        onCreate(db)
    }

    // --- OPERACIONES CATEGORÍAS ---

    fun obtenerTodasCategorias(): List<Categoria> {
        val lista = mutableListOf<Categoria>()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT $COL_CAT_ID, $COL_CAT_NOMBRE FROM $TABLE_CATEGORIAS ORDER BY $COL_CAT_NOMBRE ASC", null)
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CAT_ID))
                    val nombre = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_NOMBRE))
                    lista.add(Categoria(id, nombre))
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lista
    }

    fun existeCategoria(nombre: String, excludeId: Int = -1): Boolean {
        var exists = false
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT $COL_CAT_ID FROM $TABLE_CATEGORIAS WHERE LOWER(TRIM($COL_CAT_NOMBRE)) = LOWER(TRIM(?)) AND $COL_CAT_ID != ?",
                arrayOf(nombre.trim(), excludeId.toString())
            )
            exists = cursor.count > 0
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return exists
    }

    fun insertarCategoria(nombre: String): Long {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank() || existeCategoria(nombreLimpio)) return -1
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_CAT_NOMBRE, nombreLimpio)
            }
            db.insert(TABLE_CATEGORIAS, null, values)
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    fun actualizarCategoria(id: Int, nombre: String): Boolean {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank() || existeCategoria(nombreLimpio, excludeId = id)) return false
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_CAT_NOMBRE, nombreLimpio)
            }
            val rows = db.update(TABLE_CATEGORIAS, values, "$COL_CAT_ID = ?", arrayOf(id.toString()))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun tieneTareasCategoria(categoriaId: Int): Boolean {
        var hasTasks = false
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT $COL_TAREA_ID FROM $TABLE_TAREAS WHERE $COL_TAREA_CAT_ID = ?",
                arrayOf(categoriaId.toString())
            )
            hasTasks = cursor.count > 0
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
            hasTasks = true
        }
        return hasTasks
    }

    fun eliminarCategoria(categoriaId: Int): Boolean {
        if (tieneTareasCategoria(categoriaId)) return false
        return try {
            val db = writableDatabase
            val rows = db.delete(TABLE_CATEGORIAS, "$COL_CAT_ID = ?", arrayOf(categoriaId.toString()))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun obtenerCantidadTareasPorCategoria(categoriaId: Int): Int {
        var count = 0
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_TAREAS WHERE $COL_TAREA_CAT_ID = ?",
                arrayOf(categoriaId.toString())
            )
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }

    // --- OPERACIONES TAREAS ---

    fun obtenerTodasTareas(): List<Tarea> {
        val lista = mutableListOf<Tarea>()
        try {
            val db = readableDatabase
            val query = """
                SELECT t.$COL_TAREA_ID, t.$COL_TAREA_NOMBRE, t.$COL_TAREA_CAT_ID, c.$COL_CAT_NOMBRE
                FROM $TABLE_TAREAS t
                INNER JOIN $TABLE_CATEGORIAS c ON t.$COL_TAREA_CAT_ID = c.$COL_CAT_ID
                ORDER BY t.$COL_TAREA_ID DESC
            """.trimIndent()
            val cursor = db.rawQuery(query, null)
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TAREA_ID))
                    val nombre = cursor.getString(cursor.getColumnIndexOrThrow(COL_TAREA_NOMBRE))
                    val catId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TAREA_CAT_ID))
                    val catNombre = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_NOMBRE))
                    lista.add(Tarea(id, nombre, catId, catNombre))
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lista
    }

    fun existeTarea(nombre: String, excludeId: Int = -1): Boolean {
        var exists = false
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT $COL_TAREA_ID FROM $TABLE_TAREAS WHERE LOWER(TRIM($COL_TAREA_NOMBRE)) = LOWER(TRIM(?)) AND $COL_TAREA_ID != ?",
                arrayOf(nombre.trim(), excludeId.toString())
            )
            exists = cursor.count > 0
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return exists
    }

    fun insertarTarea(nombre: String, categoriaId: Int): Long {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank() || existeTarea(nombreLimpio)) return -1
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_TAREA_NOMBRE, nombreLimpio)
                put(COL_TAREA_CAT_ID, categoriaId)
            }
            db.insert(TABLE_TAREAS, null, values)
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    fun actualizarTarea(id: Int, nombre: String, categoriaId: Int): Boolean {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank() || existeTarea(nombreLimpio, excludeId = id)) return false
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_TAREA_NOMBRE, nombreLimpio)
                put(COL_TAREA_CAT_ID, categoriaId)
            }
            val rows = db.update(TABLE_TAREAS, values, "$COL_TAREA_ID = ?", arrayOf(id.toString()))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun eliminarTarea(tareaId: Int): Boolean {
        return try {
            val db = writableDatabase
            val rows = db.delete(TABLE_TAREAS, "$COL_TAREA_ID = ?", arrayOf(tareaId.toString()))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
