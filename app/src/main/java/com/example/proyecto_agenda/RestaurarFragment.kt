package com.example.proyecto_agenda

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.proyecto_agenda.db.AppDatabase
import com.example.proyecto_agenda.db.Evento
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.Collections

class RestaurarFragment : Fragment() {

    private lateinit var driveServiceHelper: DriveServiceHelper
    private lateinit var btnRestaurar: MaterialButton
    private lateinit var progressBar: ProgressBar

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleSignInResult(result.data)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_restaurar, container, false)
        btnRestaurar = view.findViewById(R.id.btnRestaurar)
        progressBar = view.findViewById(R.id.progressBarRestaurar)

        btnRestaurar.setOnClickListener {
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

                realizarRestauracion()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error login: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun realizarRestauracion() {
        btnRestaurar.isEnabled = false
        btnRestaurar.text = "Descargando..."
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // 1. Descargar JSON desde Drive
                val jsonContent = driveServiceHelper.readFile()

                if (jsonContent.isNullOrEmpty()) {
                    Toast.makeText(context, "No se encontró archivo de respaldo", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // 2. Convertir JSON a Lista
                val listType = object : TypeToken<List<Evento>>() {}.type
                val eventosRestaurados: List<Evento> = Gson().fromJson(jsonContent, listType)

                // 3. Reemplazar Base de Datos
                val dao = AppDatabase.getDatabase(requireContext()).eventoDao()
                dao.deleteAll() // Borrar actuales
                dao.insertAll(eventosRestaurados) // Insertar respaldo

                Toast.makeText(context, "¡Datos restaurados con éxito!", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(context, "Error al restaurar: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
                btnRestaurar.isEnabled = true
                btnRestaurar.text = "Descargar y Restaurar"
            }
        }
    }
}