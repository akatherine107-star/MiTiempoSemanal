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
        private const val DATABASE_VERSION = 1

        private const val TABLE_CATEGORIAS = "categorias"
        private const val COL_CAT_ID = "id"
        private const val COL_CAT_NOMBRE = "nombre"

        private const val TABLE_TAREAS = "tareas"
        private const val COL_TAREA_ID = "id"
        private const val COL_TAREA_NOMBRE = "nombre"
        private const val COL_TAREA_CAT_ID = "categoria_id"
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
                $COL_TAREA_NOMBRE TEXT NOT NULL,
                $COL_TAREA_CAT_ID INTEGER NOT NULL,
                FOREIGN KEY($COL_TAREA_CAT_ID) REFERENCES $TABLE_CATEGORIAS($COL_CAT_ID)
            )
        """.trimIndent()

        db.execSQL(createTableCategorias)
        db.execSQL(createTableTareas)

        // Insertar las 4 categorías por defecto al instalar la app
        val categoriasIniciales = listOf("Estudio", "Trabajo", "Descanso", "Ocio")
        for (cat in categoriasIniciales) {
            val values = ContentValues().apply {
                put(COL_CAT_NOMBRE, cat)
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
        return lista
    }

    fun existeCategoria(nombre: String, excludeId: Int = -1): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_CAT_ID FROM $TABLE_CATEGORIAS WHERE LOWER($COL_CAT_NOMBRE) = LOWER(?) AND $COL_CAT_ID != ?",
            arrayOf(nombre.trim(), excludeId.toString())
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun insertarCategoria(nombre: String): Long {
        if (existeCategoria(nombre)) return -1
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CAT_NOMBRE, nombre.trim())
        }
        return db.insert(TABLE_CATEGORIAS, null, values)
    }

    fun actualizarCategoria(id: Int, nombre: String): Boolean {
        if (existeCategoria(nombre, excludeId = id)) return false
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CAT_NOMBRE, nombre.trim())
        }
        val rows = db.update(TABLE_CATEGORIAS, values, "$COL_CAT_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }

    fun tieneTareasCategoria(categoriaId: Int): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_TAREA_ID FROM $TABLE_TAREAS WHERE $COL_TAREA_CAT_ID = ?",
            arrayOf(categoriaId.toString())
        )
        val hasTasks = cursor.count > 0
        cursor.close()
        return hasTasks
    }

    fun eliminarCategoria(categoriaId: Int): Boolean {
        if (tieneTareasCategoria(categoriaId)) return false
        val db = writableDatabase
        val rows = db.delete(TABLE_CATEGORIAS, "$COL_CAT_ID = ?", arrayOf(categoriaId.toString()))
        return rows > 0
    }

    fun obtenerCantidadTareasPorCategoria(categoriaId: Int): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_TAREAS WHERE $COL_TAREA_CAT_ID = ?",
            arrayOf(categoriaId.toString())
        )
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    // --- OPERACIONES TAREAS ---

    fun obtenerTodasTareas(): List<Tarea> {
        val lista = mutableListOf<Tarea>()
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
        return lista
    }

    fun existeTarea(nombre: String, excludeId: Int = -1): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_TAREA_ID FROM $TABLE_TAREAS WHERE LOWER($COL_TAREA_NOMBRE) = LOWER(?) AND $COL_TAREA_ID != ?",
            arrayOf(nombre.trim(), excludeId.toString())
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun insertarTarea(nombre: String, categoriaId: Int): Long {
        if (existeTarea(nombre)) return -1
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TAREA_NOMBRE, nombre.trim())
            put(COL_TAREA_CAT_ID, categoriaId)
        }
        return db.insert(TABLE_TAREAS, null, values)
    }

    fun actualizarTarea(id: Int, nombre: String, categoriaId: Int): Boolean {
        if (existeTarea(nombre, excludeId = id)) return false
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TAREA_NOMBRE, nombre.trim())
            put(COL_TAREA_CAT_ID, categoriaId)
        }
        val rows = db.update(TABLE_TAREAS, values, "$COL_TAREA_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }

    fun eliminarTarea(tareaId: Int): Boolean {
        val db = writableDatabase
        val rows = db.delete(TABLE_TAREAS, "$COL_TAREA_ID = ?", arrayOf(tareaId.toString()))
        return rows > 0
    }
}
