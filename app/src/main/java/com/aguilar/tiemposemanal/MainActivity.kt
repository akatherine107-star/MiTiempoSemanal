package com.aguilar.tiemposemanal

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.aguilar.tiemposemanal.adapter.CategoriaAdapter
import com.aguilar.tiemposemanal.adapter.EventoAdapter
import com.aguilar.tiemposemanal.adapter.TareaAdapter
import com.aguilar.tiemposemanal.db.DatabaseHelper
import com.aguilar.tiemposemanal.model.Categoria
import com.aguilar.tiemposemanal.model.Tarea
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var tabLayout: TabLayout
    private lateinit var containerEventos: View
    private lateinit var containerTareas: View
    private lateinit var containerCategorias: View

    private lateinit var rvEventos: RecyclerView
    private lateinit var rvTareas: RecyclerView
    private lateinit var rvCategorias: RecyclerView

    private lateinit var emptyEventos: View
    private lateinit var emptyTareas: View
    private lateinit var emptyCategorias: View

    private lateinit var fabAdd: ExtendedFloatingActionButton

    // Vistas de Eventos (Sprint 2)
    private lateinit var actvTareaBusqueda: AutoCompleteTextView
    private lateinit var chipCategoriaSeleccionada: Chip
    private lateinit var panelEventoEnCurso: View
    private lateinit var txtTareaEnCurso: TextView
    private lateinit var txtHoraInicioEnCurso: TextView
    private lateinit var btnIniciar: Button
    private lateinit var btnFinalizar: Button

    private lateinit var eventoAdapter: EventoAdapter
    private lateinit var tareaAdapter: TareaAdapter
    private lateinit var categoriaAdapter: CategoriaAdapter

    private var currentTab = 0 // 0: Eventos, 1: Tareas, 2: Categorías
    private var tareaSeleccionadaBusqueda: Tarea? = null

    private val dbDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val displayDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DatabaseHelper(this)

        initViews()
        setupTabs()
        setupRecyclerViews()
        setupBusquedaTareas()
        setupBotonesEvento()
        setupFab()

        cargarDatos()
    }

    override fun onResume() {
        super.onResume()
        actualizarEstadoEventoEnCurso()
    }

    private fun initViews() {
        tabLayout = findViewById(R.id.tabLayout)
        containerEventos = findViewById(R.id.containerEventos)
        containerTareas = findViewById(R.id.containerTareas)
        containerCategorias = findViewById(R.id.containerCategorias)

        rvEventos = findViewById(R.id.rvEventos)
        rvTareas = findViewById(R.id.rvTareas)
        rvCategorias = findViewById(R.id.rvCategorias)

        emptyEventos = findViewById(R.id.emptyEventos)
        emptyTareas = findViewById(R.id.emptyTareas)
        emptyCategorias = findViewById(R.id.emptyCategorias)

        fabAdd = findViewById(R.id.fabAdd)

        // Eventos views
        actvTareaBusqueda = findViewById(R.id.actvTareaBusqueda)
        chipCategoriaSeleccionada = findViewById(R.id.chipCategoriaSeleccionada)
        panelEventoEnCurso = findViewById(R.id.panelEventoEnCurso)
        txtTareaEnCurso = findViewById(R.id.txtTareaEnCurso)
        txtHoraInicioEnCurso = findViewById(R.id.txtHoraInicioEnCurso)
        btnIniciar = findViewById(R.id.btnIniciar)
        btnFinalizar = findViewById(R.id.btnFinalizar)

        val txtAppSubtitle = findViewById<TextView>(R.id.txtAppSubtitle)
        txtAppSubtitle.text = getString(R.string.app_subtitle)
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                actualizarVistaTab()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun actualizarVistaTab() {
        when (currentTab) {
            0 -> {
                containerEventos.visibility = View.VISIBLE
                containerTareas.visibility = View.GONE
                containerCategorias.visibility = View.GONE
                fabAdd.visibility = View.GONE
            }
            1 -> {
                containerEventos.visibility = View.GONE
                containerTareas.visibility = View.VISIBLE
                containerCategorias.visibility = View.GONE
                fabAdd.visibility = View.VISIBLE
                fabAdd.text = getString(R.string.title_nueva_tarea)
            }
            2 -> {
                containerEventos.visibility = View.GONE
                containerTareas.visibility = View.GONE
                containerCategorias.visibility = View.VISIBLE
                fabAdd.visibility = View.VISIBLE
                fabAdd.text = getString(R.string.title_nueva_categoria)
            }
        }
    }

    private fun setupRecyclerViews() {
        // RecyclerView Eventos
        rvEventos.layoutManager = LinearLayoutManager(this)
        eventoAdapter = EventoAdapter(emptyList())
        rvEventos.adapter = eventoAdapter

        // RecyclerView Tareas
        rvTareas.layoutManager = LinearLayoutManager(this)
        tareaAdapter = TareaAdapter(
            listaTareas = emptyList(),
            onEditClick = { tarea -> mostrarDialogoEditarTarea(tarea) },
            onDeleteClick = { tarea -> mostrarConfirmacionEliminarTarea(tarea) }
        )
        rvTareas.adapter = tareaAdapter

        // RecyclerView Categorías
        rvCategorias.layoutManager = LinearLayoutManager(this)
        categoriaAdapter = CategoriaAdapter(
            listaCategorias = emptyList(),
            getTaskCount = { catId -> dbHelper.obtenerCantidadTareasPorCategoria(catId) },
            onEditClick = { categoria -> mostrarDialogoEditarCategoria(categoria) },
            onDeleteClick = { categoria -> intentarEliminarCategoria(categoria) }
        )
        rvCategorias.adapter = categoriaAdapter
    }

    // --- BÚSQUEDA DE TAREAS (HU-03) ---

    private fun setupBusquedaTareas() {
        val adapterBusqueda = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, mutableListOf())
        actvTareaBusqueda.setAdapter(adapterBusqueda)

        var sugerenciasActuales = listOf<Tarea>()

        actvTareaBusqueda.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val texto = s?.toString()?.trim() ?: ""
                tareaSeleccionadaBusqueda = null

                if (texto.length >= 2) {
                    try {
                        sugerenciasActuales = dbHelper.buscarTareasPorNombre(texto)
                        val nombres = sugerenciasActuales.map { "${it.nombre} (${it.categoriaNombre})" }

                        adapterBusqueda.clear()
                        adapterBusqueda.addAll(nombres)
                        adapterBusqueda.notifyDataSetChanged()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    adapterBusqueda.clear()
                    adapterBusqueda.notifyDataSetChanged()
                    chipCategoriaSeleccionada.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        actvTareaBusqueda.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            try {
                val seleccion = parent.getItemAtPosition(position) as? String
                if (seleccion != null) {
                    val tarea = sugerenciasActuales.find { "${it.nombre} (${it.categoriaNombre})" == seleccion }
                    if (tarea != null) {
                        tareaSeleccionadaBusqueda = tarea
                        actvTareaBusqueda.setText(tarea.nombre, false) // false evita volver a filtrar el texto
                        actvTareaBusqueda.setSelection(tarea.nombre.length)
                        chipCategoriaSeleccionada.text = "Categoría: ${tarea.categoriaNombre}"
                        chipCategoriaSeleccionada.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- CONTROL DE EVENTOS (HU-04 Y HU-05) ---

    private fun setupBotonesEvento() {
        btnIniciar.setOnClickListener {
            try {
                val nombreTareaInput = actvTareaBusqueda.text.toString().trim()
                if (nombreTareaInput.isBlank()) {
                    Toast.makeText(this, "Escribe el nombre de una tarea para iniciar", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Verificar si hay un evento en curso
                if (dbHelper.obtenerEventoEnCurso() != null) {
                    Toast.makeText(this, "Ya hay un evento en curso. Debes finalizarlo primero.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                // Buscar si la tarea existe en BD
                val tareaExistente = tareaSeleccionadaBusqueda ?: dbHelper.obtenerTareaPorNombre(nombreTareaInput)

                if (tareaExistente != null) {
                    // La tarea existe, iniciar el evento directamente
                    iniciarEventoProcesado(tareaExistente)
                } else {
                    // HU-04: La tarea NO existe -> Pedir categoría para crearla
                    mostrarDialogoSeleccionarCategoriaParaNuevaTarea(nombreTareaInput)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Ocurrió un error al iniciar evento: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }

        btnFinalizar.setOnClickListener {
            try {
                finalizarEventoEnCurso()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Ocurrió un error al finalizar evento: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarDialogoSeleccionarCategoriaParaNuevaTarea(nombreTarea: String) {
        val categorias = dbHelper.obtenerTodasCategorias()
        if (categorias.isEmpty()) {
            Toast.makeText(this, "Primero debes crear al menos una categoría en el sistema", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tarea, null)
        val tilNombre = dialogView.findViewById<TextInputLayout>(R.id.tilNombreTarea)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreTarea)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerCategoria)

        etNombre.setText(nombreTarea)
        etNombre.isEnabled = false // Fijar el nombre escrito

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        val dialog = AlertDialog.Builder(this)
            .setTitle("Crear nueva tarea y empezar")
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val categoriaSeleccionada = spinner.selectedItem as? Categoria
            if (categoriaSeleccionada == null) {
                Toast.makeText(this, getString(R.string.error_no_category_selected), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val resultId = dbHelper.insertarTarea(nombreTarea, categoriaSeleccionada.id)
            if (resultId != -1L) {
                val nuevaTarea = Tarea(resultId.toInt(), nombreTarea, categoriaSeleccionada.id, categoriaSeleccionada.nombre)
                cargarTareas() // Actualizar lista de Tareas Sprint 1
                cargarCategorias()

                Toast.makeText(this, "Tarea creada: ${nuevaTarea.nombre}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

                // Iniciar evento con la nueva tarea
                iniciarEventoProcesado(nuevaTarea)
            } else {
                tilNombre.error = getString(R.string.error_task_exists)
            }
        }
    }

    private fun iniciarEventoProcesado(tarea: Tarea) {
        try {
            val ahora = LocalDateTime.now()
            val fechaInicioISO = ahora.format(dbDateFormatter)

            val result = dbHelper.iniciarEvento(tarea.id, fechaInicioISO)
            if (result != -1L) {
                Toast.makeText(this, "Evento iniciado: ${tarea.nombre}", Toast.LENGTH_SHORT).show()
                cargarEventos()
                actualizarEstadoEventoEnCurso()
            } else {
                Toast.makeText(this, "Error al iniciar evento", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al procesar inicio de evento: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun finalizarEventoEnCurso() {
        try {
            val eventoEnCurso = dbHelper.obtenerEventoEnCurso()
            if (eventoEnCurso == null) {
                actualizarEstadoEventoEnCurso()
                return
            }

            val fin = LocalDateTime.now()
            val fechaFinISO = fin.format(dbDateFormatter)

            val duracionSegundos = try {
                val inicio = LocalDateTime.parse(eventoEnCurso.fechaInicio, dbDateFormatter)
                Duration.between(inicio, fin).seconds
            } catch (e: Exception) {
                0L
            }

            val exito = dbHelper.finalizarEvento(eventoEnCurso.id, fechaFinISO, duracionSegundos)
            if (exito) {
                Toast.makeText(this, "Evento finalizado con éxito", Toast.LENGTH_SHORT).show()
                actvTareaBusqueda.setText("", false)
                chipCategoriaSeleccionada.visibility = View.GONE
                tareaSeleccionadaBusqueda = null

                cargarEventos()
                actualizarEstadoEventoEnCurso()
            } else {
                Toast.makeText(this, "Error al finalizar el evento", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al procesar fin de evento: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun actualizarEstadoEventoEnCurso() {
        try {
            val eventoActivo = dbHelper.obtenerEventoEnCurso()

            if (eventoActivo != null) {
                btnIniciar.isEnabled = false
                btnFinalizar.isEnabled = true

                panelEventoEnCurso.visibility = View.VISIBLE
                txtTareaEnCurso.text = "${eventoActivo.tareaNombre} (${eventoActivo.categoriaNombre})"

                val fechaFormateada = formatearFecha(eventoActivo.fechaInicio)
                txtHoraInicioEnCurso.text = "Inicio: $fechaFormateada"
            } else {
                btnIniciar.isEnabled = true
                btnFinalizar.isEnabled = false

                panelEventoEnCurso.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatearFecha(fechaIso: String): String {
        return try {
            val dt = LocalDateTime.parse(fechaIso, dbDateFormatter)
            dt.format(displayDateFormatter)
        } catch (e: Exception) {
            fechaIso
        }
    }

    // --- CARGA DE DATOS ---

    private fun cargarDatos() {
        cargarEventos()
        cargarTareas()
        cargarCategorias()
        actualizarEstadoEventoEnCurso()
    }

    private fun cargarEventos() {
        try {
            val eventos = dbHelper.obtenerTodosEventos()
            val eventosFormateados = eventos.map { evt ->
                evt.copy(
                    fechaInicio = formatearFecha(evt.fechaInicio),
                    fechaFin = if (evt.fechaFin != null) formatearFecha(evt.fechaFin) else null
                )
            }

            eventoAdapter.actualizarLista(eventosFormateados)
            if (eventosFormateados.isEmpty()) {
                emptyEventos.visibility = View.VISIBLE
                rvEventos.visibility = View.GONE
            } else {
                emptyEventos.visibility = View.GONE
                rvEventos.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cargarTareas() {
        try {
            val tareas = dbHelper.obtenerTodasTareas()
            tareaAdapter.actualizarLista(tareas)
            if (tareas.isEmpty()) {
                emptyTareas.visibility = View.VISIBLE
                rvTareas.visibility = View.GONE
            } else {
                emptyTareas.visibility = View.GONE
                rvTareas.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cargarCategorias() {
        try {
            val categorias = dbHelper.obtenerTodasCategorias()
            categoriaAdapter.actualizarLista(categorias)
            if (categorias.isEmpty()) {
                emptyCategorias.visibility = View.VISIBLE
                rvCategorias.visibility = View.GONE
            } else {
                emptyCategorias.visibility = View.GONE
                rvCategorias.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupFab() {
        fabAdd.setOnClickListener {
            if (currentTab == 1) {
                mostrarDialogoCrearTarea()
            } else if (currentTab == 2) {
                mostrarDialogoCrearCategoria()
            }
        }
    }

    // --- DIÁLOGOS SPRINT 1 ---

    private fun mostrarDialogoCrearCategoria() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_categoria, null)
        val tilNombre = dialogView.findViewById<TextInputLayout>(R.id.tilNombreCategoria)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreCategoria)

        etNombre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tilNombre.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.title_nueva_categoria)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            if (nombre.isBlank()) {
                tilNombre.error = getString(R.string.error_empty_cat_name)
                return@setOnClickListener
            }

            if (dbHelper.existeCategoria(nombre)) {
                tilNombre.error = getString(R.string.error_cat_exists)
                return@setOnClickListener
            }

            val result = dbHelper.insertarCategoria(nombre)
            if (result != -1L) {
                cargarCategorias()
                Toast.makeText(this, "Categoría creada con éxito", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                tilNombre.error = getString(R.string.error_cat_exists)
            }
        }
    }

    private fun mostrarDialogoEditarCategoria(categoria: Categoria) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_categoria, null)
        val tilNombre = dialogView.findViewById<TextInputLayout>(R.id.tilNombreCategoria)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreCategoria)
        etNombre.setText(categoria.nombre)

        etNombre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tilNombre.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.title_editar_categoria)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nuevoNombre = etNombre.text.toString().trim()
            if (nuevoNombre.isBlank()) {
                tilNombre.error = getString(R.string.error_empty_cat_name)
                return@setOnClickListener
            }

            if (dbHelper.existeCategoria(nuevoNombre, excludeId = categoria.id)) {
                tilNombre.error = getString(R.string.error_cat_exists)
                return@setOnClickListener
            }

            val exito = dbHelper.actualizarCategoria(categoria.id, nuevoNombre)
            if (exito) {
                cargarCategorias()
                cargarTareas()
                cargarEventos()
                Toast.makeText(this, "Categoría actualizada", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                tilNombre.error = getString(R.string.error_cat_exists)
            }
        }
    }

    private fun intentarEliminarCategoria(categoria: Categoria) {
        try {
            if (dbHelper.tieneTareasCategoria(categoria.id)) {
                AlertDialog.Builder(this)
                    .setTitle("No se puede eliminar")
                    .setMessage(getString(R.string.error_cat_has_tasks))
                    .setPositiveButton("Aceptar", null)
                    .show()
            } else {
                val mensaje = getString(R.string.confirm_delete_category, categoria.nombre)
                AlertDialog.Builder(this)
                    .setTitle(R.string.btn_eliminar)
                    .setMessage(mensaje)
                    .setPositiveButton(R.string.btn_eliminar) { _, _ ->
                        val exito = dbHelper.eliminarCategoria(categoria.id)
                        if (exito) {
                            cargarCategorias()
                            Toast.makeText(this, "Categoría eliminada", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, getString(R.string.error_cat_has_tasks), Toast.LENGTH_LONG).show()
                        }
                    }
                    .setNegativeButton(R.string.btn_cancelar, null)
                    .show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al eliminar categoría", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoCrearTarea() {
        val categorias = dbHelper.obtenerTodasCategorias()
        if (categorias.isEmpty()) {
            Toast.makeText(this, "Primero debes crear al menos una categoría", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tarea, null)
        val tilNombre = dialogView.findViewById<TextInputLayout>(R.id.tilNombreTarea)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreTarea)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerCategoria)

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        etNombre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tilNombre.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.title_nueva_tarea)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            if (nombre.isBlank()) {
                tilNombre.error = getString(R.string.error_empty_task_name)
                return@setOnClickListener
            }

            val categoriaSeleccionada = spinner.selectedItem as? Categoria
            if (categoriaSeleccionada == null) {
                Toast.makeText(this, getString(R.string.error_no_category_selected), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dbHelper.existeTarea(nombre)) {
                tilNombre.error = getString(R.string.error_task_exists)
                return@setOnClickListener
            }

            val result = dbHelper.insertarTarea(nombre, categoriaSeleccionada.id)
            if (result != -1L) {
                cargarTareas()
                cargarCategorias()
                Toast.makeText(this, "Tarea creada con éxito", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                tilNombre.error = getString(R.string.error_task_exists)
            }
        }
    }

    private fun mostrarDialogoEditarTarea(tarea: Tarea) {
        val categorias = dbHelper.obtenerTodasCategorias()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tarea, null)
        val tilNombre = dialogView.findViewById<TextInputLayout>(R.id.tilNombreTarea)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreTarea)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerCategoria)

        etNombre.setText(tarea.nombre)

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        val index = categorias.indexOfFirst { it.id == tarea.categoriaId }
        if (index >= 0) {
            spinner.setSelection(index)
        }

        etNombre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tilNombre.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.title_editar_tarea)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nuevoNombre = etNombre.text.toString().trim()
            if (nuevoNombre.isBlank()) {
                tilNombre.error = getString(R.string.error_empty_task_name)
                return@setOnClickListener
            }

            val categoriaSeleccionada = spinner.selectedItem as? Categoria
            if (categoriaSeleccionada == null) {
                Toast.makeText(this, getString(R.string.error_no_category_selected), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dbHelper.existeTarea(nuevoNombre, excludeId = tarea.id)) {
                tilNombre.error = getString(R.string.error_task_exists)
                return@setOnClickListener
            }

            val exito = dbHelper.actualizarTarea(tarea.id, nuevoNombre, categoriaSeleccionada.id)
            if (exito) {
                cargarTareas()
                cargarCategorias()
                cargarEventos()
                Toast.makeText(this, "Tarea actualizada", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                tilNombre.error = getString(R.string.error_task_exists)
            }
        }
    }

    private fun mostrarConfirmacionEliminarTarea(tarea: Tarea) {
        try {
            val mensaje = getString(R.string.confirm_delete_task, tarea.nombre)
            AlertDialog.Builder(this)
                .setTitle(R.string.btn_eliminar)
                .setMessage(mensaje)
                .setPositiveButton(R.string.btn_eliminar) { _, _ ->
                    val exito = dbHelper.eliminarTarea(tarea.id)
                    if (exito) {
                        cargarTareas()
                        cargarCategorias()
                        cargarEventos()
                        Toast.makeText(this, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "No se pudo eliminar la tarea", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(R.string.btn_cancelar, null)
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al eliminar tarea", Toast.LENGTH_SHORT).show()
        }
    }
}
