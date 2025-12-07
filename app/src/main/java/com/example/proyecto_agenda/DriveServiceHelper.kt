package com.example.proyecto_agenda

import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Collections

class DriveServiceHelper(private val mDriveService: Drive) {

    // Crear o sobrescribir archivo de respaldo
    suspend fun createFile(contentJson: String): String? = withContext(Dispatchers.IO) {
        val metadata = File()
            .setParents(Collections.singletonList("root")) // Guardar en raíz
            .setMimeType("application/json")
            .setName("agenda_backup.json")

        val contentStream = ByteArrayContent.fromString("application/json", contentJson)

        // 1. Buscar si ya existe para sobrescribirlo
        val fileId = searchFileId()

        if (fileId != null) {
            // Actualizar existente
            mDriveService.files().update(fileId, null, contentStream).execute()
            return@withContext fileId
        } else {
            // Crear nuevo
            val googleFile = mDriveService.files().create(metadata, contentStream).execute()
            return@withContext googleFile.id
        }
    }

    // Leer archivo de respaldo
    suspend fun readFile(): String? = withContext(Dispatchers.IO) {
        val fileId = searchFileId() ?: return@withContext null

        val inputStream = mDriveService.files().get(fileId).executeMediaAsInputStream()
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            stringBuilder.append(line)
        }
        return@withContext stringBuilder.toString()
    }

    // Buscar el ID del archivo por nombre
    private fun searchFileId(): String? {
        val page = mDriveService.files().list()
            .setQ("name = 'agenda_backup.json' and trashed = false")
            .setSpaces("drive")
            .execute()

        return if (page.files.isNotEmpty()) page.files[0].id else null
    }
}