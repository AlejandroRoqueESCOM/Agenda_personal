package com.example.proyecto_agenda.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "eventos")
data class Evento(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoria: String,
    val fecha: String,
    val hora: String,
    val descripcion: String,
    val status: String,
    val ubicacion: String,
    val latitud: Double?,
    val longitud: Double?,
    val contacto: String,
    val timestamp: Long,
    val recordatorio: String // NUEVO CAMPO
)