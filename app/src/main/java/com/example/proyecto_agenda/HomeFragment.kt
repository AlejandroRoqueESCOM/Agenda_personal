package com.example.proyecto_agenda

import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
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
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var tvFecha: TextView

    // Variables para manejar la edición en el diálogo
    private var activeDialogContactInput: EditText? = null
    private var activeDialogLocationInput: EditText? = null

    // 1. Launcher para Contactos (Igual que en Agregar)
    private val contactLauncher = registerForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            val cursor: Cursor? = requireContext().contentResolver.query(
                uri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME), null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        // Actualizamos el EditText del diálogo activo
                        activeDialogContactInput?.setText(it.getString(nameIndex))
                    }
                }
            }
        }
    }

    // 2. Launcher para Google Places (Igual que en Agregar)
    private val placesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val intent = result.data
            if (intent != null) {
                val place = Autocomplete.getPlaceFromIntent(intent)
                // Actualizamos el EditText del diálogo activo
                activeDialogLocationInput?.setText("${place.name} - ${place.address}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicializar Places si no está listo
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyA66uOipVB0QaCp2QTHVSAnGr5TqZkp9WY")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        recycler = view.findViewById(R.id.recyclerHome)
        tvFecha = view.findViewById(R.id.tvFechaHoy)
        recycler.layoutManager = LinearLayoutManager(context)

        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault())
        tvFecha.text = "Hoy es ${sdf.format(Date())}"

        cargarEventos()
        return view
    }

    override fun onResume() {
        super.onResume()
        cargarEventos()
    }

    private fun cargarEventos() {
        lifecycleScope.launch {
            val eventos = AppDatabase.getDatabase(requireContext()).eventoDao().getAll()
            val adapter = EventoAdapter(eventos) { evento ->
                mostrarDetalleCompleto(evento)
            }
            recycler.adapter = adapter
        }
    }

    // --- NUEVO: MOSTRAR FICHA COMPLETA ---
    private fun mostrarDetalleCompleto(evento: Evento) {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_detalle_home, null)
        builder.setView(view)
        val dialog = builder.create()

        // Vincular vistas
        val chipCat = view.findViewById<Chip>(R.id.chipDetalleCategoria)
        val tvStatus = view.findViewById<TextView>(R.id.tvDetalleStatus)
        val tvTitulo = view.findViewById<TextView>(R.id.tvDetalleTitulo)
        val tvFecha = view.findViewById<TextView>(R.id.tvDetalleFechaHora)
        val tvUbi = view.findViewById<TextView>(R.id.tvDetalleUbicacion)
        val tvContact = view.findViewById<TextView>(R.id.tvDetalleContacto)
        val tvReminder = view.findViewById<TextView>(R.id.tvDetalleRecordatorio)
        val btnEditar = view.findViewById<Button>(R.id.btnDetalleEditar)
        val btnCerrar = view.findViewById<Button>(R.id.btnDetalleCerrar)

        // Llenar datos
        chipCat.text = evento.categoria
        tvStatus.text = evento.status
        tvTitulo.text = evento.descripcion
        tvFecha.text = "${evento.fecha} - ${evento.hora}"
        tvUbi.text = evento.ubicacion.ifEmpty { "Sin ubicación" }
        tvContact.text = evento.contacto.ifEmpty { "Sin contacto" }
        tvReminder.text = evento.recordatorio

        // Botón Editar: Cierra este detalle y abre el de edición funcional
        btnEditar.setOnClickListener {
            dialog.dismiss()
            mostrarDialogoEdicionFuncional(evento)
        }

        btnCerrar.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // --- NUEVO: DIÁLOGO DE EDICIÓN FUNCIONAL (Mapas y Contactos) ---
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

        // Asignar datos actuales
        etContacto.setText(evento.contacto)
        etUbicacion.setText(evento.ubicacion)

        // --- LÓGICA IMPORTANTE: Desactivar teclado y activar Launchers ---

        // 1. Contacto
        etContacto.focusable = View.NOT_FOCUSABLE
        etContacto.isFocusableInTouchMode = false
        etContacto.setOnClickListener {
            activeDialogContactInput = etContacto // Guardamos referencia
            contactLauncher.launch(null)
        }

        // 2. Ubicación
        etUbicacion.focusable = View.NOT_FOCUSABLE
        etUbicacion.isFocusableInTouchMode = false
        etUbicacion.setOnClickListener {
            activeDialogLocationInput = etUbicacion // Guardamos referencia
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(requireContext())
            placesLauncher.launch(intent)
        }

        // Spinner Status
        val statusOpciones = arrayOf("Pendiente", "Realizado", "Aplazado")
        val adapterStatus = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusOpciones)
        spinnerStatus.adapter = adapterStatus
        val spinnerPos = statusOpciones.indexOf(evento.status)
        if (spinnerPos >= 0) spinnerStatus.setSelection(spinnerPos)

        // Botones
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
                Toast.makeText(context, "Evento actualizado", Toast.LENGTH_SHORT).show()
                cargarEventos()
                dialog.dismiss()
            }
        }

        btnEliminar.setOnClickListener {
            lifecycleScope.launch {
                AppDatabase.getDatabase(requireContext()).eventoDao().delete(evento)
                Toast.makeText(context, "Eliminado", Toast.LENGTH_SHORT).show()
                cargarEventos()
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}