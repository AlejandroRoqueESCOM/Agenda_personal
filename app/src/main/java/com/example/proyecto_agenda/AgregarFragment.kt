package com.example.proyecto_agenda

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.location.Geocoder
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.proyecto_agenda.db.AppDatabase
import com.example.proyecto_agenda.db.Evento
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AgregarFragment : Fragment(), OnMapReadyCallback {

    private lateinit var etFecha: TextInputEditText
    private lateinit var etHora: TextInputEditText
    private lateinit var etContacto: TextInputEditText
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var etUbicacion: TextInputEditText
    private lateinit var spinnerStatus: AutoCompleteTextView
    private lateinit var spinnerRecordatorio: AutoCompleteTextView
    private lateinit var chipGroup: ChipGroup
    private lateinit var btnGuardar: MaterialButton
    private lateinit var scrollView: androidx.core.widget.NestedScrollView

    private var googleMap: GoogleMap? = null
    private var latitudSeleccionada: Double? = null
    private var longitudSeleccionada: Double? = null

    private val startAutocomplete = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val intent = result.data
            if (intent != null) {
                val place = Autocomplete.getPlaceFromIntent(intent)
                etUbicacion.setText("${place.name} - ${place.address}")
                place.latLng?.let { actualizarMapa(it, place.name ?: "Ubicación") }
            }
        }
    }

    private val contactLauncher = registerForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            val cursor: Cursor? = requireContext().contentResolver.query(
                uri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME), null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (nameIndex >= 0) etContacto.setText(it.getString(nameIndex))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyA66uOipVB0QaCp2QTHVSAnGr5TqZkp9WY")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_agregar, container, false)

        etFecha = view.findViewById(R.id.etFecha)
        etHora = view.findViewById(R.id.etHora)
        etContacto = view.findViewById(R.id.etContacto)
        etDescripcion = view.findViewById(R.id.etDescripcion)
        etUbicacion = view.findViewById(R.id.etUbicacion)
        spinnerStatus = view.findViewById(R.id.autoCompleteStatus)
        spinnerRecordatorio = view.findViewById(R.id.spinnerRecordatorio)
        chipGroup = view.findViewById(R.id.chipGroupCategoria)
        btnGuardar = view.findViewById(R.id.btnGuardar)
        scrollView = view.findViewById(R.id.mainScrollView)

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupPickers()
        setupStatusDropdown()
        setupRecordatorioDropdown()

        btnGuardar.setOnClickListener { guardarEvento() }

        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val cdmx = LatLng(19.4326, -99.1332)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(cdmx, 10f))

        googleMap?.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                scrollView.requestDisallowInterceptTouchEvent(true)
            }
        }
        googleMap?.setOnCameraIdleListener {
            scrollView.requestDisallowInterceptTouchEvent(false)
        }

        // CORRECCIÓN: Geocodificación (Obtener nombre del lugar)
        googleMap?.setOnMapLongClickListener { latLng ->
            lifecycleScope.launch {
                val direccion = obtenerDireccionDesdeCoordenadas(latLng)
                etUbicacion.setText(direccion)
                actualizarMapa(latLng, direccion)
            }
        }
    }

    private suspend fun obtenerDireccionDesdeCoordenadas(latLng: LatLng): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                return@withContext addresses[0].getAddressLine(0) ?: "Ubicación en mapa"
            }
        } catch (e: Exception) {
            Log.e("Geocoder", "Error: ${e.message}")
        }
        return@withContext "Lat: ${latLng.latitude}, Lon: ${latLng.longitude}"
    }

    private fun actualizarMapa(latLng: LatLng, title: String) {
        latitudSeleccionada = latLng.latitude
        longitudSeleccionada = latLng.longitude
        googleMap?.clear()
        googleMap?.addMarker(MarkerOptions().position(latLng).title(title))
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    private fun setupPickers() {
        etFecha.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                etFecha.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        etHora.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, h, min ->
                etHora.setText(String.format("%02d:%02d", h, min))
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        etContacto.setOnClickListener { contactLauncher.launch(null) }

        etUbicacion.setOnClickListener {
            // AQUÍ ESTABA EL ERROR: Necesitamos pedir LAT_LNG para que el mapa se mueva
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(requireContext())
            startAutocomplete.launch(intent)
        }
    }

    private fun setupStatusDropdown() {
        // CORRECCIÓN SPINNER: Usar una lista simple y recargar el adaptador al hacer clic
        val opciones = arrayOf("Pendiente", "Realizado", "Aplazado")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, opciones)

        spinnerStatus.setAdapter(adapter)
        spinnerStatus.setText("Pendiente", false) // false es clave para no filtrar

        spinnerStatus.setOnClickListener {
            spinnerStatus.setAdapter(adapter) // Recargar para mostrar todos
            spinnerStatus.showDropDown()
        }
    }

    private fun setupRecordatorioDropdown() {
        val opciones = arrayOf("Sin recordatorio", "A la hora del evento", "10 minutos antes", "1 día antes")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, opciones)
        spinnerRecordatorio.setAdapter(adapter)
        spinnerRecordatorio.setText("A la hora del evento", false)
        spinnerRecordatorio.setOnClickListener {
            spinnerRecordatorio.setAdapter(adapter)
            spinnerRecordatorio.showDropDown()
        }
    }

    private fun guardarEvento() {
        if (etFecha.text.isNullOrEmpty() || etHora.text.isNullOrEmpty() || etDescripcion.text.isNullOrEmpty()) {
            Toast.makeText(context, "Completa fecha, hora y descripción", Toast.LENGTH_SHORT).show()
            return
        }

        val chipId = chipGroup.checkedChipId
        val categoria = if (chipId != View.NO_ID) view?.findViewById<Chip>(chipId)?.text.toString() else "Otro"

        val fechaStr = etFecha.text.toString()
        val horaStr = etHora.text.toString()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = sdf.parse("$fechaStr $horaStr")
        val timestamp = date?.time ?: System.currentTimeMillis()

        val opcionRecordatorio = spinnerRecordatorio.text.toString()
        val tiempoNotificacion = calcularTiempoNotificacion(timestamp, opcionRecordatorio)

        val nuevoEvento = Evento(
            categoria = categoria,
            fecha = fechaStr,
            hora = horaStr,
            descripcion = etDescripcion.text.toString(),
            status = spinnerStatus.text.toString(),
            ubicacion = etUbicacion.text.toString(),
            latitud = latitudSeleccionada,
            longitud = longitudSeleccionada,
            contacto = etContacto.text.toString(),
            timestamp = timestamp,
            recordatorio = opcionRecordatorio
        )

        lifecycleScope.launch {
            val id = AppDatabase.getDatabase(requireContext()).eventoDao().insert(nuevoEvento)
            if (tiempoNotificacion != null && tiempoNotificacion > System.currentTimeMillis()) {
                programarNotificacion(nuevoEvento, id.toInt(), tiempoNotificacion)
            }
            Toast.makeText(context, "Evento guardado!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.nav_home)
        }
    }

    private fun calcularTiempoNotificacion(eventTime: Long, opcion: String): Long? {
        return when (opcion) {
            "A la hora del evento" -> eventTime
            "10 minutos antes" -> eventTime - (10 * 60 * 1000)
            "1 día antes" -> eventTime - (24 * 60 * 60 * 1000)
            else -> null
        }
    }

    private fun programarNotificacion(evento: Evento, id: Int, timeInMillis: Long) {
        val intent = Intent(requireContext(), NotificationReceiver::class.java).apply {
            putExtra("titulo", evento.categoria)
            putExtra("mensaje", evento.descripcion)
            putExtra("idEvento", id)
            putExtra("timestamp", timeInMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(), id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        } catch (e: SecurityException) {
            Log.e("Alarma", "Falta permiso de alarma exacta")
        }
    }
}