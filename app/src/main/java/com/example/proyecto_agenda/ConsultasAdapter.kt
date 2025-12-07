package com.example.proyecto_agenda

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto_agenda.db.Evento

class ConsultasAdapter(
    private var eventos: List<Evento>,
    private val onClick: (Evento) -> Unit
) : RecyclerView.Adapter<ConsultasAdapter.TablaViewHolder>() {

    inner class TablaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tvFechaTabla)
        val tvHora: TextView = view.findViewById(R.id.tvHoraTabla)
        val tvCategoria: TextView = view.findViewById(R.id.tvCategoriaTabla)
        val tvStatus: TextView = view.findViewById(R.id.tvStatusTabla)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcionTabla)

        fun bind(evento: Evento) {
            tvFecha.text = evento.fecha
            tvHora.text = evento.hora
            tvCategoria.text = evento.categoria
            tvStatus.text = evento.status
            tvDescripcion.text = evento.descripcion

            // Colorear status para feedback visual
            val color = when(evento.status) {
                "Pendiente" -> android.R.color.holo_blue_dark
                "Realizado" -> android.R.color.holo_green_dark
                "Aplazado" -> android.R.color.holo_red_dark
                else -> android.R.color.black
            }
            tvStatus.setTextColor(ContextCompat.getColor(itemView.context, color))

            itemView.setOnClickListener { onClick(evento) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TablaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_consulta_tabla, parent, false)
        return TablaViewHolder(view)
    }

    override fun onBindViewHolder(holder: TablaViewHolder, position: Int) {
        holder.bind(eventos[position])
    }

    override fun getItemCount() = eventos.size

    fun updateList(newList: List<Evento>) {
        eventos = newList
        notifyDataSetChanged()
    }
}