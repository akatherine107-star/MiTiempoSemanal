package com.aguilar.tiemposemanal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.aguilar.tiemposemanal.adapter.CategoriaAdapter
import com.aguilar.tiemposemanal.adapter.TareaAdapter
import com.aguilar.tiemposemanal.db.DatabaseHelper
import com.aguilar.tiemposemanal.model.Categoria
import com.aguilar.tiemposemanal.model.Tarea

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var tabLayout: TabLayout
    private lateinit var containerTareas: View
    private lateinit var containerCategorias: View
    private lateinit var rvTareas: RecyclerView
    private lateinit var rvCategorias: RecyclerView
    private lateinit var emptyTareas: View
    private lateinit var emptyCategorias: View
    private lateinit var fabAdd: ExtendedFloatingActionButton

    private lateinit var tareaAdapter: TareaAdapter
    private lateinit var categoriaAdapter: CategoriaAdapter

    private var currentTab = 0 // 0: Tareas, 1: Categorías

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
        setupFab()

        cargarDatos()
    }

    private fun initViews() {
        tabLayout = findViewById(R.id.tabLayout)
        containerTareas = findViewById(R.id.containerTareas)
        containerCategorias = findViewById(R.id.containerCategorias)
        rvTareas = findViewById(R.id.rvTareas)
        rvCategorias = findViewById(R.id.rvCategorias)
        emptyTareas = findViewById(R.id.emptyTareas)
        emptyCategorias = findViewById(R.id.emptyCategorias)
        fabAdd = findViewById(R.id.fabAdd)

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
        if (currentTab == 0) {
            containerTareas.visibility = View.VISIBLE
            containerCategorias.visibility = View.GONE
            fabAdd.text = getString(R.string.title_nueva_tarea)
        } else {
            containerTareas.visibility = View.GONE
            containerCategorias.visibility = View.VISIBLE
            fabAdd.text = getString(R.string.title_nueva_categoria)
        }
    }

    private fun setupRecyclerViews() {
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

    private fun setupFab() {
        fabAdd.setOnClickListener {
            if (currentTab == 0) {
                mostrarDialogoCrearTarea()
            } else {
                mostrarDialogoCrearCategoria()
            }
        }
    }

    private fun cargarDatos() {
        cargarTareas()
        cargarCategorias()
    }

    private fun cargarTareas() {
        val tareas = dbHelper.obtenerTodasTareas()
        tareaAdapter.actualizarLista(tareas)
        if (tareas.isEmpty()) {
            emptyTareas.visibility = View.VISIBLE
            rvTareas.visibility = View.GONE
        } else {
            emptyTareas.visibility = View.GONE
            rvTareas.visibility = View.VISIBLE
        }
    }

    private fun cargarCategorias() {
        val categorias = dbHelper.obtenerTodasCategorias()
        categoriaAdapter.actualizarLista(categorias)
        if (categorias.isEmpty()) {
            emptyCategorias.visibility = View.VISIBLE
            rvCategorias.visibility = View.GONE
        } else {
            emptyCategorias.visibility = View.GONE
            rvCategorias.visibility = View.VISIBLE
        }
    }

    // --- DIÁLOGOS DE CATEGORÍA ---

    private fun mostrarDialogoCrearCategoria() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_categoria, null)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreCategoria)

        AlertDialog.Builder(this)
            .setTitle(R.string.title_nueva_categoria)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar) { _, _ ->
                val nombre = etNombre.text.toString().trim()
                if (nombre.isBlank()) {
                    Toast.makeText(this, getString(R.string.error_empty_cat_name), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (dbHelper.existeCategoria(nombre)) {
                    Toast.makeText(this, getString(R.string.error_cat_exists), Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                val result = dbHelper.insertarCategoria(nombre)
                if (result != -1L) {
                    cargarCategorias()
                    Toast.makeText(this, "Categoría creada con éxito", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.error_cat_exists), Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    private fun mostrarDialogoEditarCategoria(categoria: Categoria) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_categoria, null)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreCategoria)
        etNombre.setText(categoria.nombre)

        AlertDialog.Builder(this)
            .setTitle(R.string.title_editar_categoria)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar) { _, _ ->
                val nuevoNombre = etNombre.text.toString().trim()
                if (nuevoNombre.isBlank()) {
                    Toast.makeText(this, getString(R.string.error_empty_cat_name), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (dbHelper.existeCategoria(nuevoNombre, excludeId = categoria.id)) {
                    Toast.makeText(this, getString(R.string.error_cat_exists), Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                val exito = dbHelper.actualizarCategoria(categoria.id, nuevoNombre)
                if (exito) {
                    cargarCategorias()
                    cargarTareas() // Actualizar vista de tareas si cambió la categoría
                    Toast.makeText(this, "Categoría actualizada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.error_cat_exists), Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    private fun intentarEliminarCategoria(categoria: Categoria) {
        if (dbHelper.tieneTareasCategoria(categoria.id)) {
            AlertDialog.Builder(this)
                .setTitle("No se puede eliminar")
                .setMessage(getString(R.string.error_cat_has_tasks))
                .setPositiveButton("Aceptar", null)
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle(R.string.btn_eliminar)
                .setMessage(getString(R.string.confirm_delete_category, categoria.nombre))
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
    }

    // --- DIÁLOGOS DE TAREA ---

    private fun mostrarDialogoCrearTarea() {
        val categorias = dbHelper.obtenerTodasCategorias()
        if (categorias.isEmpty()) {
            Toast.makeText(this, "Primero debes crear al menos una categoría", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tarea, null)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreTarea)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerCategoria)

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        AlertDialog.Builder(this)
            .setTitle(R.string.title_nueva_tarea)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar) { _, _ ->
                val nombre = etNombre.text.toString().trim()
                if (nombre.isBlank()) {
                    Toast.makeText(this, getString(R.string.error_empty_task_name), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val categoriaSeleccionada = spinner.selectedItem as? Categoria
                if (categoriaSeleccionada == null) {
                    Toast.makeText(this, getString(R.string.error_no_category_selected), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (dbHelper.existeTarea(nombre)) {
                    Toast.makeText(this, getString(R.string.error_task_exists), Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                val result = dbHelper.insertarTarea(nombre, categoriaSeleccionada.id)
                if (result != -1L) {
                    cargarTareas()
                    cargarCategorias() // Actualizar contador en categorías
                    Toast.makeText(this, "Tarea creada con éxito", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.error_task_exists), Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    private fun mostrarDialogoEditarTarea(tarea: Tarea) {
        val categorias = dbHelper.obtenerTodasCategorias()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tarea, null)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreTarea)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerCategoria)

        etNombre.setText(tarea.nombre)

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        // Seleccionar la categoría actual
        val index = categorias.indexOfFirst { it.id == tarea.categoriaId }
        if (index >= 0) {
            spinner.setSelection(index)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.title_editar_tarea)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar) { _, _ ->
                val nuevoNombre = etNombre.text.toString().trim()
                if (nuevoNombre.isBlank()) {
                    Toast.makeText(this, getString(R.string.error_empty_task_name), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val categoriaSeleccionada = spinner.selectedItem as? Categoria
                if (categoriaSeleccionada == null) {
                    Toast.makeText(this, getString(R.string.error_no_category_selected), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (dbHelper.existeTarea(nuevoNombre, excludeId = tarea.id)) {
                    Toast.makeText(this, getString(R.string.error_task_exists), Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                val exito = dbHelper.actualizarTarea(tarea.id, nuevoNombre, categoriaSeleccionada.id)
                if (exito) {
                    cargarTareas()
                    cargarCategorias()
                    Toast.makeText(this, "Tarea actualizada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.error_task_exists), Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    private fun mostrarConfirmacionEliminarTarea(tarea: Tarea) {
        AlertDialog.Builder(this)
            .setTitle(R.string.btn_eliminar)
            .setMessage(getString(R.string.confirm_delete_task, tarea.nombre))
            .setPositiveButton(R.string.btn_eliminar) { _, _ ->
                val exito = dbHelper.eliminarTarea(tarea.id)
                if (exito) {
                    cargarTareas()
                    cargarCategorias()
                    Toast.makeText(this, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }
}
