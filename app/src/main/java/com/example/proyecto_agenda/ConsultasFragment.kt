package com.example.proyecto_agenda

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.database.Cursor
import android.location.Geocoder
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto_agenda.db.AppDatabase
import com.example.proyecto_agenda.db.Evento
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ConsultasFragment : Fragment() {

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

    private var activeDialogContactInput: EditText? = null
    private var activeDialogLocationInput: EditText? = null
    private var activeDialogMap: MapView? = null

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
                val direccion = "${place.name} - ${place.address}"
                activeDialogLocationInput?.setText(direccion)

                place.latLng?.let { latLng ->
                    activeDialogMap?.getMapAsync { map ->
                        map.clear()
                        map.addMarker(MarkerOptions().position(latLng).title("Nueva"))
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // CORRECCIÓN: API KEY REAL
        if (!Places.isInitialized()) Places.initialize(requireContext(), "AIzaSyA66uOipVB0QaCp2QTHVSAnGr5TqZkp9WY")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_consultas, container, false)

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
            mostrarDialogoEdicionCompleto(evento)
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
            when (tabIndex) {
                0 -> {
                    if(!etFechaInicio.text.isNullOrEmpty() && !etFechaFin.text.isNullOrEmpty()){
                        val dateInicio = sdf.parse(etFechaInicio.text.toString())
                        val dateFin = sdf.parse(etFechaFin.text.toString())
                        if(dateInicio != null && dateFin != null) {
                            inicio = dateInicio.time; cal.time = dateFin; cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); fin = cal.timeInMillis
                        }
                    }
                }
                1 -> {
                    val yearStr = etAnioOnly.text.toString()
                    val year = if(yearStr.isNotEmpty()) yearStr.toInt() else cal.get(Calendar.YEAR)
                    cal.set(year, 0, 1, 0, 0, 0); inicio = cal.timeInMillis; cal.set(year, 11, 31, 23, 59, 59); fin = cal.timeInMillis
                }
                2 -> {
                    if(!etDiaOnly.text.isNullOrEmpty()){
                        val dateDia = sdf.parse(etDiaOnly.text.toString())
                        if (dateDia != null) { cal.time = dateDia; cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); inicio = cal.timeInMillis; cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); fin = cal.timeInMillis }
                    }
                }
                3 -> {
                    val yearStr = etAnioMes.text.toString()
                    val year = if(yearStr.isNotEmpty()) yearStr.toInt() else cal.get(Calendar.YEAR)
                    val mesNombre = spinnerMes.text.toString()
                    val meses = arrayOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
                    val mesIndex = meses.indexOf(mesNombre).takeIf { it >= 0 } ?: 0
                    cal.set(year, mesIndex, 1, 0, 0, 0); inicio = cal.timeInMillis; val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH); cal.set(year, mesIndex, maxDay, 23, 59, 59); fin = cal.timeInMillis
                }
            }

            val chipId = chipGroupCat.checkedChipId
            var catFiltro: String? = null
            if (chipId != View.NO_ID) {
                val chip = view?.findViewById<Chip>(chipId)
                catFiltro = chip?.text.toString()
            }

            lifecycleScope.launch {
                val dao = AppDatabase.getDatabase(requireContext()).eventoDao()
                var eventos = if (inicio > 0) dao.getEventosEnRango(inicio, fin) else dao.getAll()
                if (catFiltro != null) eventos = eventos.filter { it.categoria == catFiltro }
                adapter.updateList(eventos)
                if (eventos.isEmpty()) Toast.makeText(context, "No se encontraron eventos", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) { Toast.makeText(context, "Error en filtros", Toast.LENGTH_SHORT).show() }
    }

    // --- COPIAR LA MISMA FUNCIÓN MOSTRAR DIALOGO EDICION COMPLETO DE HOMEFRAGMENT ---
    // (Incluye toda la lógica de mapa, scrollView, spinners, etc)
    private fun mostrarDialogoEdicionCompleto(evento: Evento) {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_editar_evento, null)
        builder.setView(view)
        val dialog = builder.create()

        val dialogScrollView = view.findViewById<ScrollView>(R.id.dialogScrollView)
        val mapView = view.findViewById<MapView>(R.id.mapViewDialog)

        val chipGroupCat = view.findViewById<ChipGroup>(R.id.chipGroupEditCategoria)
        val spinnerStatus = view.findViewById<AutoCompleteTextView>(R.id.spinnerDialogStatus)
        val etFecha = view.findViewById<TextInputEditText>(R.id.etEditFecha)
        val etHora = view.findViewById<TextInputEditText>(R.id.etEditHora)
        val etDesc = view.findViewById<TextInputEditText>(R.id.etEditDescripcion)
        val spinnerRecordatorio = view.findViewById<AutoCompleteTextView>(R.id.spinnerEditRecordatorio)
        val etContact = view.findViewById<TextInputEditText>(R.id.etDialogContacto)
        val etUbi = view.findViewById<TextInputEditText>(R.id.etDialogUbicacion)
        val btnGuardar = view.findViewById<Button>(R.id.btnDialogActualizar)
        val btnEliminar = view.findViewById<Button>(R.id.btnDialogEliminar)

        // MAPA
        activeDialogMap = mapView
        MapsInitializer.initialize(requireContext())
        mapView.onCreate(null)
        mapView.onResume()
        mapView.getMapAsync { map ->
            val lat = evento.latitud ?: 19.4326
            val lng = evento.longitud ?: -99.1332
            val pos = LatLng(lat, lng)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
            map.addMarker(MarkerOptions().position(pos).title("Actual"))

            map.setOnCameraMoveStartedListener { reason ->
                if (reason == com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    dialogScrollView.requestDisallowInterceptTouchEvent(true)
                }
            }
            map.setOnCameraIdleListener { dialogScrollView.requestDisallowInterceptTouchEvent(false) }

            map.setOnMapLongClickListener { latLng ->
                lifecycleScope.launch {
                    val geocoder = Geocoder(requireContext(), Locale.getDefault())
                    try {
                        val addresses = withContext(Dispatchers.IO) { geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) }
                        if (!addresses.isNullOrEmpty()) etUbi.setText(addresses[0].getAddressLine(0))
                    } catch (e: Exception) { }
                    map.clear()
                    map.addMarker(MarkerOptions().position(latLng).title("Nueva"))
                }
            }
        }

        // Llenar Datos
        etFecha.setText(evento.fecha)
        etHora.setText(evento.hora)
        etDesc.setText(evento.descripcion)
        etContact.setText(evento.contacto)
        etUbi.setText(evento.ubicacion)

        for (i in 0 until chipGroupCat.childCount) {
            val chip = chipGroupCat.getChildAt(i) as Chip
            if (chip.text.toString() == evento.categoria) { chip.isChecked = true; break }
        }

        val statusOpts = arrayOf("Pendiente", "Realizado", "Aplazado")
        val adapterStatus = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statusOpts)
        spinnerStatus.setAdapter(adapterStatus)
        spinnerStatus.setText(evento.status, false)
        spinnerStatus.setOnClickListener { spinnerStatus.showDropDown() }

        val recOpts = arrayOf("Sin recordatorio", "A la hora del evento", "10 minutos antes", "1 día antes")
        val adapterRec = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, recOpts)
        spinnerRecordatorio.setAdapter(adapterRec)
        spinnerRecordatorio.setText(evento.recordatorio, false)
        spinnerRecordatorio.setOnClickListener { spinnerRecordatorio.showDropDown() }

        // Listeners
        etFecha.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                etFecha.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        etHora.setOnClickListener {
            val c = Calendar.getInstance()
            val timeListener = TimePickerDialog.OnTimeSetListener { _, h, min ->
                etHora.setText(String.format("%02d:%02d", h, min))
            }
            TimePickerDialog(requireContext(), timeListener, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        etContact.setOnClickListener {
            activeDialogContactInput = etContact
            contactLauncher.launch(null)
        }

        etUbi.setOnClickListener {
            activeDialogLocationInput = etUbi
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(requireContext())
            placesLauncher.launch(intent)
        }

        btnGuardar.setOnClickListener {
            val f = etFecha.text.toString()
            val h = etHora.text.toString()
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val ts = sdf.parse("$f $h")?.time ?: evento.timestamp

            val chipCatId = chipGroupCat.checkedChipId
            val cat = if (chipCatId != View.NO_ID) view.findViewById<Chip>(chipCatId).text.toString() else evento.categoria

            val eventoActualizado = evento.copy(
                categoria = cat,
                fecha = f,
                hora = h,
                descripcion = etDesc.text.toString(),
                status = spinnerStatus.text.toString(),
                recordatorio = spinnerRecordatorio.text.toString(),
                contacto = etContact.text.toString(),
                ubicacion = etUbi.text.toString(),
                timestamp = ts
            )

            lifecycleScope.launch {
                AppDatabase.getDatabase(requireContext()).eventoDao().update(eventoActualizado)
                Toast.makeText(context, "Actualizado", Toast.LENGTH_SHORT).show()
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

    private fun setupTabs() { tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener { override fun onTabSelected(tab: TabLayout.Tab?) { layoutRango.visibility = View.GONE; layoutAnio.visibility = View.GONE; layoutDia.visibility = View.GONE; layoutMes.visibility = View.GONE; when (tab?.position) { 0 -> layoutRango.visibility = View.VISIBLE; 1 -> layoutAnio.visibility = View.VISIBLE; 2 -> layoutDia.visibility = View.VISIBLE; 3 -> layoutMes.visibility = View.VISIBLE } } override fun onTabUnselected(tab: TabLayout.Tab?) {} override fun onTabReselected(tab: TabLayout.Tab?) {} }) }
    private fun setupPickers() { val showCalendar = { editText: TextInputEditText -> val cal = Calendar.getInstance(); DatePickerDialog(requireContext(), { _, y, m, d -> val fecha = String.format("%02d/%02d/%04d", d, m + 1, y); editText.setText(fecha) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show() }; etFechaInicio.setOnClickListener { showCalendar(etFechaInicio) }; etFechaFin.setOnClickListener { showCalendar(etFechaFin) }; etDiaOnly.setOnClickListener { showCalendar(etDiaOnly) } }
    private fun setupMonthSpinner() { val meses = arrayOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"); val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, meses); spinnerMes.setAdapter(adapter); spinnerMes.setText("Enero", false); spinnerMes.setOnClickListener { spinnerMes.showDropDown() } }
}