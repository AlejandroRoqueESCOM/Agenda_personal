package com.example.proyecto_agenda

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.proyecto_agenda.db.AppDatabase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.gson.Gson
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class RespaldoFragment : Fragment() {

    private lateinit var driveServiceHelper: DriveServiceHelper
    private lateinit var progressBar: ProgressBar
    private lateinit var btnRespaldo: MaterialButton
    private lateinit var tvUltimo: TextView

    // Launcher para Login de Google
    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleSignInResult(result.data)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_respaldo, container, false)
        btnRespaldo = view.findViewById(R.id.btnRealizarRespaldo)
        progressBar = view.findViewById(R.id.progressBarRespaldo)
        tvUltimo = view.findViewById(R.id.tvUltimoRespaldo)

        btnRespaldo.setOnClickListener {
            requestSignIn()
        }
        return view
    }

    private fun requestSignIn() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        val client = GoogleSignIn.getClient(requireActivity(), signInOptions)
        loginLauncher.launch(client.signInIntent)
    }

    private fun handleSignInResult(result: Intent?) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
            .addOnSuccessListener { account ->
                // Inicializar Drive Helper
                val credential = GoogleAccountCredential.usingOAuth2(
                    requireContext(), Collections.singleton(DriveScopes.DRIVE_FILE)
                )
                credential.selectedAccount = account.account

                val googleDriveService = Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    GsonFactory(),
                    credential
                ).setApplicationName("Proyecto Agenda").build()

                driveServiceHelper = DriveServiceHelper(googleDriveService)

                // Proceder al respaldo
                realizarRespaldo()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error inicio sesión: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun realizarRespaldo() {
        btnRespaldo.isEnabled = false
        btnRespaldo.text = "Subiendo..."
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // 1. Obtener datos de Room
                val eventos = AppDatabase.getDatabase(requireContext()).eventoDao().getAll()

                // 2. Convertir a JSON
                val jsonContent = Gson().toJson(eventos)

                // 3. Subir a Drive
                driveServiceHelper.createFile(jsonContent)

                // 4. UI Success
                val fechaHoy = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                tvUltimo.text = "Último respaldo: $fechaHoy"
                Toast.makeText(context, "Respaldo exitoso en Drive", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
                btnRespaldo.isEnabled = true
                btnRespaldo.text = "Realizar Respaldo Ahora"
            }
        }
    }
}