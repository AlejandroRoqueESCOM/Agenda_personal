package com.example.proyecto_agenda

import android.app.AlarmManager
import android.app.AlertDialog
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
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var recyclerProximos: RecyclerView
    private lateinit var recyclerPasados: RecyclerView
    private lateinit var tvNoProximos: TextView
    private lateinit var tvFecha: TextView

    // Controles del Historial
    private lateinit var headerHistorial: LinearLayout
    private lateinit var iconExpand: ImageView
    private var isHistorialExpanded = true

    // Variables para el diálogo de edición activo
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

                // Mover el mapa del diálogo si existe
                place.latLng?.let { latLng ->
                    activeDialogMap?.getMapAsync { map ->
                        map.clear()
                        map.addMarker(MarkerOptions().position(latLng).title("Nueva selección"))
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Places.isInitialized()) Places.initialize(requireContext(), "AIzaSyA66uOipVB0QaCp2QTHVSAnGr5TqZkp9WY")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerProximos = view.findViewById(R.id.recyclerProximos)
        recyclerPasados = view.findViewById(R.id.recyclerPasados)
        tvNoProximos = view.findViewById(R.id.tvNoProximos)
        tvFecha = view.findViewById(R.id.tvFechaHoy)
        headerHistorial = view.findViewById(R.id.headerHistorial)
        iconExpand = view.findViewById(R.id.iconExpandHistorial)

        recyclerProximos.layoutManager = LinearLayoutManager(context)
        recyclerPasados.layoutManager = LinearLayoutManager(context)

        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault())
        tvFecha.text = "Hoy es ${sdf.format(Date())}"

        // LÓGICA DE EXPANDIR/OCULTAR HISTORIAL
        headerHistorial.setOnClickListener {
            isHistorialExpanded = !isHistorialExpanded
            recyclerPasados.visibility = if (isHistorialExpanded) View.VISIBLE else View.GONE
            // Animación simple de rotación para la flecha
            iconExpand.animate().rotation(if (isHistorialExpanded) 0f else 180f).start()
        }

        eliminarEventosViejos() // Borrado automático de eventos > 3 días
        cargarEventos()
        return view
    }

    override fun onResume() {
        super.onResume()
        cargarEventos()
    }

    // --- FUNCIÓN PARA BORRAR EVENTOS DE HACE MÁS DE 3 DÍAS ---
    private fun eliminarEventosViejos() {
        lifecycleScope.launch(Dispatchers.IO) {
            val tresDiasMillis = 3L * 24 * 60 * 60 * 1000
            val limiteFecha = System.currentTimeMillis() - tresDiasMillis

            val dao = AppDatabase.getDatabase(requireContext()).eventoDao()
            val todos = dao.getAll()

            // Filtramos eventos que ya pasaron hace más de 3 días
            val eventosViejos = todos.filter { it.timestamp < limiteFecha }

            if (eventosViejos.isNotEmpty()) {
                // Cancelamos sus alarmas por si acaso quedara alguna pendiente
                eventosViejos.forEach { cancelarNotificacion(it.id) }

                // Borramos de la BD
                dao.deleteList(eventosViejos)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Se eliminaron ${eventosViejos.size} eventos antiguos", Toast.LENGTH_SHORT).show()
                    cargarEventos() // Refrescamos la lista
                }
            }
        }
    }

    private fun cargarEventos() {
        lifecycleScope.launch {
            val todosEventos = AppDatabase.getDatabase(requireContext()).eventoDao().getAll()
            val now = System.currentTimeMillis()

            // Separar listas: Próximos (>= ahora) y Pasados (< ahora)
            val proximos = todosEventos.filter { it.timestamp >= now }.sortedBy { it.timestamp }
            val pasados = todosEventos.filter { it.timestamp < now }.sortedByDescending { it.timestamp }

            if (proximos.isEmpty()) {
                tvNoProximos.visibility = View.VISIBLE
                recyclerProximos.visibility = View.GONE
            } else {
                tvNoProximos.visibility = View.GONE
                recyclerProximos.visibility = View.VISIBLE
                recyclerProximos.adapter = EventoAdapter(proximos) { mostrarDetalleCompleto(it) }
            }

            recyclerPasados.adapter = EventoAdapter(pasados) { mostrarDetalleCompleto(it) }
        }
    }

    private fun mostrarDetalleCompleto(evento: Evento) {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_detalle_home, null)
        builder.setView(view)
        val dialog = builder.create()

        val btnEditar = view.findViewById<Button>(R.id.btnDetalleEditar)
        val btnCerrar = view.findViewById<Button>(R.id.btnDetalleCerrar)

        // Llenar vista de lectura
        view.findViewById<Chip>(R.id.chipDetalleCategoria).text = evento.categoria
        view.findViewById<TextView>(R.id.tvDetalleStatus).text = evento.status
        view.findViewById<TextView>(R.id.tvDetalleTitulo).text = evento.descripcion
        view.findViewById<TextView>(R.id.tvDetalleFechaHora).text = "${evento.fecha} - ${evento.hora}"
        view.findViewById<TextView>(R.id.tvDetalleUbicacion).text = evento.ubicacion
        view.findViewById<TextView>(R.id.tvDetalleContacto).text = evento.contacto
        view.findViewById<TextView>(R.id.tvDetalleRecordatorio).text = evento.recordatorio

        btnEditar.setOnClickListener {
            dialog.dismiss()
            mostrarDialogoEdicionCompleto(evento)
        }
        btnCerrar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

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

        // --- MAPA ---
        activeDialogMap = mapView
        MapsInitializer.initialize(requireContext())
        mapView.onCreate(null)
        mapView.onResume()
        mapView.getMapAsync { map ->
            val lat = evento.latitud ?: 19.4326
            val lng = evento.longitud ?: -99.1332
            val pos = LatLng(lat, lng)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
            map.addMarker(MarkerOptions().position(pos).title("Ubicación Actual"))

            // Solución Scroll
            map.setOnCameraMoveStartedListener { reason ->
                if (reason == com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    dialogScrollView.requestDisallowInterceptTouchEvent(true)
                }
            }
            map.setOnCameraIdleListener { dialogScrollView.requestDisallowInterceptTouchEvent(false) }

            // Click largo para cambiar ubicación
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

        // --- PRE-LLENADO DE DATOS ---
        etFecha.setText(evento.fecha)
        etHora.setText(evento.hora)
        etDesc.setText(evento.descripcion)
        etContact.setText(evento.contacto)
        etUbi.setText(evento.ubicacion)

        // Chips Categoría
        for (i in 0 until chipGroupCat.childCount) {
            val chip = chipGroupCat.getChildAt(i) as Chip
            if (chip.text.toString() == evento.categoria) { chip.isChecked = true; break }
        }

        // Spinner Status (FIX: Recargar adapter al click)
        val statusOpts = arrayOf("Pendiente", "Realizado", "Aplazado")
        val adapterStatus = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statusOpts)
        spinnerStatus.setAdapter(adapterStatus)
        spinnerStatus.setText(evento.status, false)
        spinnerStatus.setOnClickListener {
            spinnerStatus.setAdapter(adapterStatus)
            spinnerStatus.showDropDown()
        }

        // Spinner Recordatorio (FIX: Recargar adapter al click)
        val recOpts = arrayOf("Sin recordatorio", "A la hora del evento", "10 minutos antes", "1 día antes")
        val adapterRec = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, recOpts)
        spinnerRecordatorio.setAdapter(adapterRec)
        spinnerRecordatorio.setText(evento.recordatorio, false)
        spinnerRecordatorio.setOnClickListener {
            spinnerRecordatorio.setAdapter(adapterRec)
            spinnerRecordatorio.showDropDown()
        }

        // --- LISTENERS ---
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

        // --- GUARDAR CAMBIOS ---
        btnGuardar.setOnClickListener {
            val f = etFecha.text.toString()
            val h = etHora.text.toString()
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            // Recalcular timestamp
            val dateObj = sdf.parse("$f $h")
            val newTimestamp = dateObj?.time ?: evento.timestamp

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
                timestamp = newTimestamp // Guardamos la nueva hora para que se mueva en la lista
            )

            lifecycleScope.launch {
                // 1. Actualizar DB
                AppDatabase.getDatabase(requireContext()).eventoDao().update(eventoActualizado)

                // 2. REPROGRAMAR NOTIFICACIÓN (Si cambió la hora o el tipo de recordatorio)
                val opcionRec = spinnerRecordatorio.text.toString()
                reprogramarNotificacion(eventoActualizado, eventoActualizado.id, newTimestamp, opcionRec)

                Toast.makeText(context, "Evento actualizado y alarma reprogramada", Toast.LENGTH_SHORT).show()
                cargarEventos()
                dialog.dismiss()
            }
        }

        btnEliminar.setOnClickListener {
            lifecycleScope.launch {
                // 1. Cancelar notificación
                cancelarNotificacion(evento.id)
                // 2. Borrar de DB
                AppDatabase.getDatabase(requireContext()).eventoDao().delete(evento)
                cargarEventos()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    // --- FUNCIÓN PARA REPROGRAMAR ALARMA ---
    private fun reprogramarNotificacion(evento: Evento, id: Int, eventTime: Long, opcion: String) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), NotificationReceiver::class.java)

        // 1. Primero cancelamos la alarma vieja por si existe
        val pendingIntentCancel = PendingIntent.getBroadcast(
            requireContext(), id, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntentCancel != null) {
            alarmManager.cancel(pendingIntentCancel)
            pendingIntentCancel.cancel()
        }

        // 2. Calcular a qué hora debe sonar la nueva alarma
        val triggerTime = when (opcion) {
            "A la hora del evento" -> eventTime
            "10 minutos antes" -> eventTime - (10 * 60 * 1000)
            "1 día antes" -> eventTime - (24 * 60 * 60 * 1000)
            else -> null
        }

        // 3. Si hay fecha válida y es en el futuro, creamos la nueva alarma
        if (triggerTime != null && triggerTime > System.currentTimeMillis()) {
            intent.apply {
                putExtra("titulo", evento.categoria)
                putExtra("mensaje", evento.descripcion)
                putExtra("idEvento", id)
            }
            // FLAG_UPDATE_CURRENT es clave aquí
            val pendingIntentNew = PendingIntent.getBroadcast(
                requireContext(), id, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntentNew)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntentNew)
                }
                Log.d("ALARM_DEBUG", "Alarma reprogramada para: ${Date(triggerTime)}")
            } catch (e: SecurityException) {
                Log.e("ALARM_DEBUG", "Error permisos: ${e.message}")
            }
        }
    }

    // --- FUNCIÓN PARA CANCELAR ALARMA AL BORRAR ---
    private fun cancelarNotificacion(id: Int) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(), id, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}