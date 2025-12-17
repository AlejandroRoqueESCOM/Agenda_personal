package com.example.proyecto_agenda

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto_agenda.db.Evento
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventoAdapter(
    private var eventos: List<Evento>,
    private val onClick: (Evento) -> Unit
) : RecyclerView.Adapter<EventoAdapter.EventoViewHolder>() {

    inner class EventoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Vistas de la tarjeta
        val cardView: MaterialCardView = view.findViewById(R.id.cardEvento)
        val chipCategoria: Chip = view.findViewById(R.id.chipCategoriaItem)
        val tvFechaHora: TextView = view.findViewById(R.id.tvFechaHoraItem)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcionItem)
        val tvStatus: TextView = view.findViewById(R.id.tvStatusItem)
        val tvContacto: TextView = view.findViewById(R.id.tvContactoItem)
        val imgContacto: ImageView = view.findViewById(R.id.imgContacto)

        // Vista NUEVA del encabezado
        val tvHeaderDate: TextView = view.findViewById(R.id.tvHeaderDate)

        fun bind(evento: Evento, position: Int) {
            // 1. Asignar datos
            chipCategoria.text = evento.categoria
            tvFechaHora.text = "${evento.fecha} - ${evento.hora}"
            tvDescripcion.text = evento.descripcion
            tvStatus.text = evento.status
            tvContacto.text = evento.contacto

            // 2. LÓGICA DE AGRUPACIÓN (ENCABEZADOS POR AÑO/DÍA)
            val sdfHeader = SimpleDateFormat("EEEE, d 'de' MMMM yyyy", Locale.getDefault())

            // Obtener fecha formateada del evento actual
            val fechaActualStr = try { sdfHeader.format(Date(evento.timestamp)) } catch (e: Exception) { evento.fecha }

            var mostrarHeader = false
            if (position == 0) {
                mostrarHeader = true // El primero de la lista siempre lleva título
            } else {
                // Comparamos con el evento anterior
                val eventoAnterior = eventos[position - 1]
                val fechaAnteriorStr = try { sdfHeader.format(Date(eventoAnterior.timestamp)) } catch (e: Exception) { eventoAnterior.fecha }

                // Si la fecha cambia, mostramos el encabezado nuevo
                if (fechaActualStr != fechaAnteriorStr) {
                    mostrarHeader = true
                }
            }

            if (mostrarHeader) {
                tvHeaderDate.visibility = View.VISIBLE
                // Capitalizar primera letra (ej: lunes -> Lunes)
                tvHeaderDate.text = fechaActualStr.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            } else {
                tvHeaderDate.visibility = View.GONE
            }

            // 3. LÓGICA DE COLORES (ANTIGUOS VS NUEVOS)
            val isPasado = evento.timestamp < System.currentTimeMillis()

            if (isPasado) {
                // ESTILO ANTIGUO (Gris/Deshabilitado)
                cardView.setCardBackgroundColor(Color.parseColor("#F5F5F5")) // Fondo gris claro
                cardView.strokeColor = Color.parseColor("#E0E0E0")

                // Textos en gris
                tvDescripcion.setTextColor(Color.GRAY)
                tvFechaHora.setTextColor(Color.GRAY)
                tvStatus.setTextColor(Color.GRAY)
                tvContacto.setTextColor(Color.GRAY)
                imgContacto.setColorFilter(Color.GRAY)

                // Chip en gris oscuro
                chipCategoria.setChipBackgroundColorResource(android.R.color.darker_gray)
            } else {
                // ESTILO ACTIVO (Normal)
                cardView.setCardBackgroundColor(Color.WHITE)
                cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.backgroundLight)

                tvDescripcion.setTextColor(Color.BLACK)
                tvFechaHora.setTextColor(Color.BLACK)
                tvContacto.setTextColor(Color.BLACK)
                imgContacto.setColorFilter(Color.BLACK)

                // Color dinámico del status
                val colorStatus = when(evento.status) {
                    "Realizado" -> android.R.color.holo_green_dark
                    "Aplazado" -> android.R.color.holo_red_dark
                    else -> R.color.secondaryColor
                }
                tvStatus.setTextColor(ContextCompat.getColor(itemView.context, colorStatus))

                // Color del chip original
                chipCategoria.setChipBackgroundColorResource(R.color.primaryColor)
            }

            itemView.setOnClickListener { onClick(evento) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evento, parent, false)
        return EventoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        holder.bind(eventos[position], position)
    }

    override fun getItemCount() = eventos.size

    fun updateList(newList: List<Evento>) {
        eventos = newList
        notifyDataSetChanged()
    }
}