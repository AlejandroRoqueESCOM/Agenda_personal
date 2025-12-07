package com.example.proyecto_agenda

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class RestaurarFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_restaurar, container, false)

        val btnRestaurar = view.findViewById<MaterialButton>(R.id.btnRestaurar)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBarRestaurar)

        btnRestaurar.setOnClickListener {
            btnRestaurar.isEnabled = false
            btnRestaurar.text = "Descargando..."
            progressBar.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                progressBar.visibility = View.GONE
                btnRestaurar.isEnabled = true
                btnRestaurar.text = "Descargar y Restaurar"

                Toast.makeText(context, "Datos restaurados correctamente", Toast.LENGTH_LONG).show()
                // Aquí idealmente navegaríamos de regreso a Inicio o recargaríamos la BD
            }, 3000)
        }
        return view
    }
}