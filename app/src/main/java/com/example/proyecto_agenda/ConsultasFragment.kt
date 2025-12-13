package com.example.proyecto_agenda

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto_agenda.db.AppDatabase
import com.example.proyecto_agenda.db.Evento
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ConsultasFragment : Fragment() {

    // ... (Tus variables de Vistas igual que antes) ...
    private lateinit var tabLayout: TabLayout
    private lateinit var layoutRango: LinearLayout
    private lateinit var layoutAnio: LinearLayout
    private lateinit var layoutDia: LinearLayout
    private lateinit var layoutMes: LinearLayout

    private lateinit var etFechaInicio: TextInputEditText
    private lateinit var etFechaFin: TextInputEditText
    private lateinit var etAnioOnly: TextInputEditText
    private lateinit var etDiaOnly: TextInputEditText
    private lateinit var etAnioMes: TextInputEditText
    private lateinit var spinnerMes: AutoCompleteTextView
    private lateinit var chipGroupCat: ChipGroup
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ConsultasAdapter

    // --- VARIABLES PARA EDICIÓN FUNCIONAL ---
    private var activeDialogContactInput: EditText? = null
    private var activeDialogLocationInput: EditText? = null

    private val contactLauncher = registerForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            val cursor: Cursor? = requireContext().contentResolver.query(
                uri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME), null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (nameIndex >= 0) activeDialogContactInput?.setText(it.getString(nameIndex))
                }
            }
        }
    }

    private val placesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val intent = result.data
            if (intent != null) {
                val place = Autocomplete.getPlaceFromIntent(intent)
                activeDialogLocationInput?.setText("${place.name} - ${place.address}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyA66uOipVB0QaCp2QTHVSAnGr5TqZkp9WY")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_consultas, container, false)

        // Vincular Vistas
        tabLayout = view.findViewById(R.id.tabLayoutFiltros)
        layoutRango = view.findViewById(R.id.layoutRango)
        layoutAnio = view.findViewById(R.id.layoutAnio)
        layoutDia = view.findViewById(R.id.layoutDia)
        layoutMes = view.findViewById(R.id.layoutMes)
        etFechaInicio = view.findViewById(R.id.etFechaInicio)
        etFechaFin = view.findViewById(R.id.etFechaFin)
        etAnioOnly = view.findViewById(R.id.etAnioOnly)
        etDiaOnly = view.findViewById(R.id.etDiaOnly)
        etAnioMes = view.findViewById(R.id.etAnioMes)
        spinnerMes = view.findViewById(R.id.spinnerMes)
        chipGroupCat = view.findViewById(R.id.chipGroupFiltroCat)
        recycler = view.findViewById(R.id.recyclerConsultas)

        recycler.layoutManager = LinearLayoutManager(context)
        adapter = ConsultasAdapter(emptyList()) { evento ->
            mostrarDialogoEdicionFuncional(evento) // Abre el diálogo con mapas/contactos
        }
        recycler.adapter = adapter

        setupTabs()
        setupPickers()
        setupMonthSpinner()

        view.findViewById<Button>(R.id.btnConsultar).setOnClickListener { realizarConsulta() }
        realizarConsultaInicial()

        return view
    }

    private fun realizarConsultaInicial() {
        lifecycleScope.launch {
            val eventos = AppDatabase.getDatabase(requireContext()).eventoDao().getAll()
            adapter.updateList(eventos)
        }
    }

    private fun realizarConsulta() {
        val tabIndex = tabLayout.selectedTabPosition
        var inicio: Long = 0
        var fin: Long = 0

        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        try {
            // Lógica para definir el rango de tiempo [cite: 42-46]
            when (tabIndex) {
                0 -> { // RANGO
                    if(!etFechaInicio.text.isNullOrEmpty() && !etFechaFin.text.isNullOrEmpty()){
                        val dateInicio = sdf.parse(etFechaInicio.text.toString())
                        val dateFin = sdf.parse(etFechaFin.text.toString())
                        if(dateInicio != null && dateFin != null) {
                            inicio = dateInicio.time
                            cal.time = dateFin
                            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
                            fin = cal.timeInMillis
                        }
                    }
                }
                1 -> { // AÑO
                    val yearStr = etAnioOnly.text.toString()
                    val year = if(yearStr.isNotEmpty()) yearStr.toInt() else cal.get(Calendar.YEAR)
                    cal.set(year, 0, 1, 0, 0, 0)
                    inicio = cal.timeInMillis
                    cal.set(year, 11, 31, 23, 59, 59)
                    fin = cal.timeInMillis
                }
                2 -> { // DÍA
                    if(!etDiaOnly.text.isNullOrEmpty()){
                        val dateDia = sdf.parse(etDiaOnly.text.toString())
                        if (dateDia != null) {
                            cal.time = dateDia
                            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                            inicio = cal.timeInMillis
                            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
                            fin = cal.timeInMillis
                        }
                    }
                }
                3 -> { // MES
                    val yearStr = etAnioMes.text.toString()
                    val year = if(yearStr.isNotEmpty()) yearStr.toInt() else cal.get(Calendar.YEAR)
                    val mesNombre = spinnerMes.text.toString()
                    val meses = arrayOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
                    val mesIndex = meses.indexOf(mesNombre).takeIf { it >= 0 } ?: 0

                    cal.set(year, mesIndex, 1, 0, 0, 0)
                    inicio = cal.timeInMillis
                    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(year, mesIndex, maxDay, 23, 59, 59)
                    fin = cal.timeInMillis
                }
            }

            // Obtener categoría seleccionada (si hay alguna) [cite: 47]
            val chipId = chipGroupCat.checkedChipId
            var catFiltro: String? = null
            if (chipId != View.NO_ID) {
                val chip = view?.findViewById<Chip>(chipId)
                catFiltro = chip?.text.toString()
            }

            lifecycleScope.launch {
                val dao = AppDatabase.getDatabase(requireContext()).eventoDao()

                // 1. Obtener eventos por fecha
                var eventos = if (inicio > 0) {
                    dao.getEventosEnRango(inicio, fin)
                } else {
                    dao.getAll() // Si no hay fechas válidas, trae todo (o muestra error)
                }

                // 2. Filtrar por Categoría en memoria
                if (catFiltro != null) {
                    eventos = eventos.filter { it.categoria == catFiltro }
                }

                // 3. Actualizar la Tabla
                adapter.updateList(eventos)

                if (eventos.isEmpty()) {
                    Toast.makeText(context, "No se encontraron eventos", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            Toast.makeText(context, "Error en filtros: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    // Configuración de UI (Calendarios y Spinners)
    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                layoutRango.visibility = View.GONE
                layoutAnio.visibility = View.GONE
                layoutDia.visibility = View.GONE
                layoutMes.visibility = View.GONE

                when (tab?.position) {
                    0 -> layoutRango.visibility = View.VISIBLE
                    1 -> layoutAnio.visibility = View.VISIBLE
                    2 -> layoutDia.visibility = View.VISIBLE
                    3 -> layoutMes.visibility = View.VISIBLE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupPickers() {
        val showCalendar = { editText: TextInputEditText ->
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                val fecha = String.format("%02d/%02d/%04d", d, m + 1, y)
                editText.setText(fecha)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
        etFechaInicio.setOnClickListener { showCalendar(etFechaInicio) }
        etFechaFin.setOnClickListener { showCalendar(etFechaFin) }
        etDiaOnly.setOnClickListener { showCalendar(etDiaOnly) }
    }

    private fun setupMonthSpinner() {
        val meses = arrayOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, meses)
        spinnerMes.setAdapter(adapter)
        spinnerMes.setText("Enero", false)
        spinnerMes.setOnClickListener { spinnerMes.showDropDown() }
    }


    // --- DIÁLOGO DE EDICIÓN FUNCIONAL (IGUAL QUE EN HOME) ---
    @RequiresApi(Build.VERSION_CODES.O)
    private fun mostrarDialogoEdicionFuncional(evento: Evento) {
        val builder = AlertDialog.Builder(requireContext())
        val viewDialog = layoutInflater.inflate(R.layout.dialog_editar_evento, null)
        builder.setView(viewDialog)
        val dialog = builder.create()

        val spinnerStatus = viewDialog.findViewById<Spinner>(R.id.spinnerDialogStatus)
        val etContacto = viewDialog.findViewById<EditText>(R.id.etDialogContacto)
        val etUbicacion = viewDialog.findViewById<EditText>(R.id.etDialogUbicacion)
        val btnActualizar = viewDialog.findViewById<Button>(R.id.btnDialogActualizar)
        val btnEliminar = viewDialog.findViewById<Button>(R.id.btnDialogEliminar)

        etContacto.setText(evento.contacto)
        etUbicacion.setText(evento.ubicacion)

        // CONFIGURAR CLICKS PARA CONTACTOS Y MAPA
        etContacto.focusable = View.NOT_FOCUSABLE
        etContacto.isFocusableInTouchMode = false
        etContacto.setOnClickListener {
            activeDialogContactInput = etContacto
            contactLauncher.launch(null)
        }

        etUbicacion.focusable = View.NOT_FOCUSABLE
        etUbicacion.isFocusableInTouchMode = false
        etUbicacion.setOnClickListener {
            activeDialogLocationInput = etUbicacion
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(requireContext())
            placesLauncher.launch(intent)
        }

        val statusOpciones = arrayOf("Pendiente", "Realizado", "Aplazado")
        val adapterStatus = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusOpciones)
        spinnerStatus.adapter = adapterStatus
        val spinnerPos = statusOpciones.indexOf(evento.status)
        if (spinnerPos >= 0) spinnerStatus.setSelection(spinnerPos)

        btnActualizar.setOnClickListener {
            val nuevoStatus = spinnerStatus.selectedItem.toString()
            val nuevoContacto = etContacto.text.toString()
            val nuevaUbi = etUbicacion.text.toString()

            val eventoActualizado = evento.copy(
                status = nuevoStatus,
                contacto = nuevoContacto,
                ubicacion = nuevaUbi
            )

            lifecycleScope.launch {
                AppDatabase.getDatabase(requireContext()).eventoDao().update(eventoActualizado)
                Toast.makeText(context, "Evento Actualizado", Toast.LENGTH_SHORT).show()
                realizarConsulta()
                dialog.dismiss()
            }
        }

        btnEliminar.setOnClickListener {
            lifecycleScope.launch {
                AppDatabase.getDatabase(requireContext()).eventoDao().delete(evento)
                Toast.makeText(context, "Eliminado", Toast.LENGTH_SHORT).show()
                realizarConsulta()
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}