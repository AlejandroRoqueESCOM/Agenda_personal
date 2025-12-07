package com.example.proyecto_agenda

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto_agenda.db.Evento
import com.google.android.material.chip.Chip

class EventoAdapter(
    private var eventos: List<Evento>,
    private val onClick: (Evento) -> Unit
) : RecyclerView.Adapter<EventoAdapter.EventoViewHolder>() {

    inner class EventoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val chipCategoria: Chip = view.findViewById(R.id.chipCategoriaItem)
        val tvFechaHora: TextView = view.findViewById(R.id.tvFechaHoraItem)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcionItem)
        val tvStatus: TextView = view.findViewById(R.id.tvStatusItem)
        val tvContacto: TextView = view.findViewById(R.id.tvContactoItem)

        fun bind(evento: Evento) {
            chipCategoria.text = evento.categoria
            tvFechaHora.text = "${evento.fecha} - ${evento.hora}"
            tvDescripcion.text = evento.descripcion
            tvStatus.text = evento.status
            tvContacto.text = evento.contacto

            // Configurar click
            itemView.setOnClickListener { onClick(evento) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evento, parent, false)
        return EventoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        holder.bind(eventos[position])
    }

    override fun getItemCount() = eventos.size

    fun updateList(newList: List<Evento>) {
        eventos = newList
        notifyDataSetChanged()
    }
}