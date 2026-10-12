package com.aguilar.tiemposemanal.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.aguilar.tiemposemanal.model.Categoria
import com.aguilar.tiemposemanal.model.Evento
import com.aguilar.tiemposemanal.model.Tarea

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "mitiemposemanal.db"
        private const val DATABASE_VERSION = 3

        private const val TABLE_CATEGORIAS = "categorias"
        private const val COL_CAT_ID = "id"
        private const val COL_CAT_NOMBRE = "nombre"

        private const val TABLE_TAREAS = "tareas"
        private const val COL_TAREA_ID = "id"
        private const val COL_TAREA_NOMBRE = "nombre"
        private const val COL_TAREA_CAT_ID = "categoria_id"

        private const val TABLE_EVENTOS = "eventos"
        private const val COL_EVT_ID = "id"
        private const val COL_EVT_TAREA_ID = "tarea_id"
        private const val COL_EVT_FECHA_INICIO = "fecha_inicio"
        private const val COL_EVT_FECHA_FIN = "fecha_fin"
        private const val COL_EVT_DURACION = "duracion_segundos"
        private const val COL_EVT_ESTADO = "estado" // 1: En curso, 0: Finalizado
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

        val createTableEventos = """
            CREATE TABLE $TABLE_EVENTOS (
                $COL_EVT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_EVT_TAREA_ID INTEGER NOT NULL,
                $COL_EVT_FECHA_INICIO TEXT NOT NULL,
                $COL_EVT_FECHA_FIN TEXT,
                $COL_EVT_DURACION INTEGER DEFAULT 0,
                $COL_EVT_ESTADO INTEGER DEFAULT 1,
                FOREIGN KEY($COL_EVT_TAREA_ID) REFERENCES $TABLE_TAREAS($COL_TAREA_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db.execSQL(createTableCategorias)
        db.execSQL(createTableTareas)
        db.execSQL(createTableEventos)

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
        if (oldVersion < 3) {
            val createTableEventos = """
                CREATE TABLE IF NOT EXISTS $TABLE_EVENTOS (
                    $COL_EVT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_EVT_TAREA_ID INTEGER NOT NULL,
                    $COL_EVT_FECHA_INICIO TEXT NOT NULL,
                    $COL_EVT_FECHA_FIN TEXT,
                    $COL_EVT_DURACION INTEGER DEFAULT 0,
                    $COL_EVT_ESTADO INTEGER DEFAULT 1,
                    FOREIGN KEY($COL_EVT_TAREA_ID) REFERENCES $TABLE_TAREAS($COL_TAREA_ID) ON DELETE CASCADE
                )
            """.trimIndent()
            db.execSQL(createTableEventos)
        }
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
                SELECT 
                    t.$COL_TAREA_ID AS tarea_id, 
                    t.$COL_TAREA_NOMBRE AS tarea_nombre, 
                    t.$COL_TAREA_CAT_ID AS tarea_cat_id, 
                    c.$COL_CAT_NOMBRE AS cat_nombre
                FROM $TABLE_TAREAS t
                INNER JOIN $TABLE_CATEGORIAS c ON t.$COL_TAREA_CAT_ID = c.$COL_CAT_ID
                ORDER BY t.$COL_TAREA_ID DESC
            """.trimIndent()
            val cursor = db.rawQuery(query, null)
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("tarea_id"))
                    val nombre = cursor.getString(cursor.getColumnIndexOrThrow("tarea_nombre"))
                    val catId = cursor.getInt(cursor.getColumnIndexOrThrow("tarea_cat_id"))
                    val catNombre = cursor.getString(cursor.getColumnIndexOrThrow("cat_nombre"))
                    lista.add(Tarea(id, nombre, catId, catNombre))
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lista
    }

    fun buscarTareasPorNombre(texto: String): List<Tarea> {
        val lista = mutableListOf<Tarea>()
        val textoLimpio = texto.trim()
        if (textoLimpio.length < 2) return lista

        try {
            val db = readableDatabase
            val query = """
                SELECT 
                    t.$COL_TAREA_ID AS tarea_id, 
                    t.$COL_TAREA_NOMBRE AS tarea_nombre, 
                    t.$COL_TAREA_CAT_ID AS tarea_cat_id, 
                    c.$COL_CAT_NOMBRE AS cat_nombre
                FROM $TABLE_TAREAS t
                INNER JOIN $TABLE_CATEGORIAS c ON t.$COL_TAREA_CAT_ID = c.$COL_CAT_ID
                WHERE LOWER(t.$COL_TAREA_NOMBRE) LIKE LOWER(?)
                ORDER BY t.$COL_TAREA_NOMBRE ASC
            """.trimIndent()
            val cursor = db.rawQuery(query, arrayOf("%$textoLimpio%"))
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("tarea_id"))
                    val nombre = cursor.getString(cursor.getColumnIndexOrThrow("tarea_nombre"))
                    val catId = cursor.getInt(cursor.getColumnIndexOrThrow("tarea_cat_id"))
                    val catNombre = cursor.getString(cursor.getColumnIndexOrThrow("cat_nombre"))
                    lista.add(Tarea(id, nombre, catId, catNombre))
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lista
    }

    fun obtenerTareaPorNombre(nombre: String): Tarea? {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank()) return null
        var tarea: Tarea? = null
        try {
            val db = readableDatabase
            val query = """
                SELECT 
                    t.$COL_TAREA_ID AS tarea_id, 
                    t.$COL_TAREA_NOMBRE AS tarea_nombre, 
                    t.$COL_TAREA_CAT_ID AS tarea_cat_id, 
                    c.$COL_CAT_NOMBRE AS cat_nombre
                FROM $TABLE_TAREAS t
                INNER JOIN $TABLE_CATEGORIAS c ON t.$COL_TAREA_CAT_ID = c.$COL_CAT_ID
                WHERE LOWER(TRIM(t.$COL_TAREA_NOMBRE)) = LOWER(TRIM(?))
                LIMIT 1
            """.trimIndent()
            val cursor = db.rawQuery(query, arrayOf(nombreLimpio))
            if (cursor.moveToFirst()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("tarea_id"))
                val nom = cursor.getString(cursor.getColumnIndexOrThrow("tarea_nombre"))
                val catId = cursor.getInt(cursor.getColumnIndexOrThrow("tarea_cat_id"))
                val catNombre = cursor.getString(cursor.getColumnIndexOrThrow("cat_nombre"))
                tarea = Tarea(id, nom, catId, catNombre)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return tarea
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
            // Eliminar primero eventos asociados para no violar la clave foránea
            db.delete(TABLE_EVENTOS, "$COL_EVT_TAREA_ID = ?", arrayOf(tareaId.toString()))
            val rows = db.delete(TABLE_TAREAS, "$COL_TAREA_ID = ?", arrayOf(tareaId.toString()))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- OPERACIONES EVENTOS ---

    fun obtenerEventoEnCurso(): Evento? {
        var evento: Evento? = null
        try {
            val db = readableDatabase
            val query = """
                SELECT 
                    e.$COL_EVT_ID AS evt_id,
                    e.$COL_EVT_TAREA_ID AS evt_tarea_id,
                    t.$COL_TAREA_NOMBRE AS tarea_nombre,
                    c.$COL_CAT_NOMBRE AS cat_nombre,
                    e.$COL_EVT_FECHA_INICIO AS fecha_inicio,
                    e.$COL_EVT_FECHA_FIN AS fecha_fin,
                    e.$COL_EVT_DURACION AS duracion,
                    e.$COL_EVT_ESTADO AS estado
                FROM $TABLE_EVENTOS e
                INNER JOIN $TABLE_TAREAS t ON e.$COL_EVT_TAREA_ID = t.$COL_TAREA_ID
                INNER JOIN $TABLE_CATEGORIAS c ON t.$COL_TAREA_CAT_ID = c.$COL_CAT_ID
                WHERE e.$COL_EVT_ESTADO = 1
                ORDER BY e.$COL_EVT_ID DESC
                LIMIT 1
            """.trimIndent()
            val cursor = db.rawQuery(query, null)
            if (cursor.moveToFirst()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("evt_id"))
                val tareaId = cursor.getInt(cursor.getColumnIndexOrThrow("evt_tarea_id"))
                val tareaNombre = cursor.getString(cursor.getColumnIndexOrThrow("tarea_nombre"))
                val catNombre = cursor.getString(cursor.getColumnIndexOrThrow("cat_nombre"))
                val fechaInicio = cursor.getString(cursor.getColumnIndexOrThrow("fecha_inicio"))
                val fechaFin = cursor.getString(cursor.getColumnIndexOrThrow("fecha_fin"))
                val duracion = cursor.getLong(cursor.getColumnIndexOrThrow("duracion"))
                val estado = cursor.getInt(cursor.getColumnIndexOrThrow("estado"))
                evento = Evento(id, tareaId, tareaNombre, catNombre, fechaInicio, fechaFin, duracion, estado == 1)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return evento
    }

    fun iniciarEvento(tareaId: Int, fechaInicioISO: String): Long {
        if (obtenerEventoEnCurso() != null) return -1
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_EVT_TAREA_ID, tareaId)
                put(COL_EVT_FECHA_INICIO, fechaInicioISO)
                put(COL_EVT_ESTADO, 1)
            }
            db.insert(TABLE_EVENTOS, null, values)
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    fun finalizarEvento(eventoId: Int, fechaFinISO: String, duracionSegundos: Long): Boolean {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_EVT_FECHA_FIN, fechaFinISO)
                put(COL_EVT_DURACION, duracionSegundos)
                put(COL_EVT_ESTADO, 0)
            }
            val rows = db.update(TABLE_EVENTOS, values, "$COL_EVT_ID = ?", arrayOf(eventoId.toString()))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun obtenerTodosEventos(): List<Evento> {
        val lista = mutableListOf<Evento>()
        try {
            val db = readableDatabase
            val query = """
                SELECT 
                    e.$COL_EVT_ID AS evt_id,
                    e.$COL_EVT_TAREA_ID AS evt_tarea_id,
                    t.$COL_TAREA_NOMBRE AS tarea_nombre,
                    c.$COL_CAT_NOMBRE AS cat_nombre,
                    e.$COL_EVT_FECHA_INICIO AS fecha_inicio,
                    e.$COL_EVT_FECHA_FIN AS fecha_fin,
                    e.$COL_EVT_DURACION AS duracion,
                    e.$COL_EVT_ESTADO AS estado
                FROM $TABLE_EVENTOS e
                INNER JOIN $TABLE_TAREAS t ON e.$COL_EVT_TAREA_ID = t.$COL_TAREA_ID
                INNER JOIN $TABLE_CATEGORIAS c ON t.$COL_TAREA_CAT_ID = c.$COL_CAT_ID
                ORDER BY e.$COL_EVT_ID DESC
            """.trimIndent()
            val cursor = db.rawQuery(query, null)
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("evt_id"))
                    val tareaId = cursor.getInt(cursor.getColumnIndexOrThrow("evt_tarea_id"))
                    val tareaNombre = cursor.getString(cursor.getColumnIndexOrThrow("tarea_nombre"))
                    val catNombre = cursor.getString(cursor.getColumnIndexOrThrow("cat_nombre"))
                    val fechaInicio = cursor.getString(cursor.getColumnIndexOrThrow("fecha_inicio"))
                    val fechaFin = cursor.getString(cursor.getColumnIndexOrThrow("fecha_fin"))
                    val duracion = cursor.getLong(cursor.getColumnIndexOrThrow("duracion"))
                    val estado = cursor.getInt(cursor.getColumnIndexOrThrow("estado"))
                    lista.add(Evento(id, tareaId, tareaNombre, catNombre, fechaInicio, fechaFin, duracion, estado == 1))
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lista
    }
}
