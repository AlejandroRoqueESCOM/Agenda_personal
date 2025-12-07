package com.example.proyecto_agenda

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RespaldoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_respaldo, container, false)

        val btnRespaldo = view.findViewById<MaterialButton>(R.id.btnRealizarRespaldo)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBarRespaldo)
        val tvUltimo = view.findViewById<TextView>(R.id.tvUltimoRespaldo)

        btnRespaldo.setOnClickListener {
            // Simulación de proceso de subida
            btnRespaldo.isEnabled = false
            btnRespaldo.text = "Subiendo archivos..."
            progressBar.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                progressBar.visibility = View.GONE
                btnRespaldo.isEnabled = true
                btnRespaldo.text = "Realizar Respaldo Ahora"

                val fechaHoy = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                tvUltimo.text = "Último respaldo: $fechaHoy"

                Toast.makeText(context, "Respaldo exitoso en Drive", Toast.LENGTH_SHORT).show()
            }, 3000) // 3 segundos de delay simulado
        }

        return view
    }
}