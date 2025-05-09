package com.example.pruebav.database

import android.content.Context
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.pruebav.AppDatabase
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStreamReader

@Entity(
    tableName = "error_codigo",
    indices = [Index(value = ["codigoError"], unique = true)]
)
data class CodigosError(
    @PrimaryKey val codigoError: String,
    @ColumnInfo(name = "descripcion", defaultValue = "'Sin descripción'") val descripcion: String,
    @ColumnInfo(name = "categoria") val categoria: String, // NOT NULL porque no es nullable
    @ColumnInfo(name = "componente") val componente: String // NOT NULL porque no es nullable

)

@Dao
interface CodigosDao {
    @Query("SELECT * FROM error_codigo WHERE codigoError = :codigo LIMIT 1")
    suspend fun buscarCodigoPorPid(codigo: String): CodigosError?
}

fun cargarCodigosDesdeJson(context: Context): List<CodigosError> {
    val inputStream = context.assets.open("codigos_error.json")
    val reader = InputStreamReader(inputStream)
    val tipoLista = object : TypeToken<List<CodigosError>>() {}.type

    Log.d("JSON","codigos de error cargados correctamente")

    return Gson().fromJson(reader, tipoLista)
}







