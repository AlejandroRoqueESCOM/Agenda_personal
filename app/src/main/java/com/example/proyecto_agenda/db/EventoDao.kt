package com.example.proyecto_agenda.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EventoDao {
    @Insert
    suspend fun insert(evento: Evento): Long

    @Update
    suspend fun update(evento: Evento)

    @Delete
    suspend fun delete(evento: Evento)

    // NUEVO: Para borrar múltiples eventos viejos de una sola vez
    @Delete
    suspend fun deleteList(eventos: List<Evento>)

    @Query("SELECT * FROM eventos ORDER BY timestamp ASC")
    suspend fun getAll(): List<Evento>

    @Query("SELECT * FROM eventos WHERE fecha = :fechaHoy ORDER BY timestamp ASC")
    suspend fun getEventosByDate(fechaHoy: String): List<Evento>

    // Filtros dinámicos para Consultas
    @Query("SELECT * FROM eventos WHERE timestamp BETWEEN :inicio AND :fin")
    suspend fun getEventosEnRango(inicio: Long, fin: Long): List<Evento>

    @Query("DELETE FROM eventos")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(eventos: List<Evento>)
}