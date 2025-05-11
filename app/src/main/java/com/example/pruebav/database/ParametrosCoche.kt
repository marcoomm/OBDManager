package com.example.pruebav.database

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.pruebav.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Entity(
    tableName = "parametros",
    indices = [Index(value = ["vin"], unique = true)]
)
data class ParametrosCoche(
    @PrimaryKey val vin: String,
    val parametros: List<Parametro>
)

data class Parametro(
    val nombre: String,
    val valor: String,
    val categoria: String
)

@Dao
interface ParametrosDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarParametros(parametros: ParametrosCoche)

    @Query("SELECT * FROM parametros WHERE vin = :vin")
    suspend fun obtenerParametros(vin: String): ParametrosCoche?

    @Query("DELETE FROM parametros WHERE vin = :vin")
    suspend fun borrarParametros(vin: String)
}


fun guardarParametros(context: Context, vin: String, parametros: List<Parametro>) {
    val entry = ParametrosCoche(vin, parametros)
    val db = AppDatabase.getDatabase(context)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            db.parametrosDao().insertarParametros(entry)

            val pa = db.parametrosDao().obtenerParametros(vin)
            Log.d("BBDD", pa.toString())

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
                Log.d("OBD", "Guardados correctamente")
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error al guardar los datos", Toast.LENGTH_LONG).show()
                Log.e("OBD", "Error al guardar", e)
            }
        }
    }
}



// Modificación aquí para obtener un solo ParametrosCoche en vez de una lista
suspend fun obtenerParametros(context: Context, vin: String, callback: (ParametrosCoche?) -> Unit) {
    val db = AppDatabase.getDatabase(context)
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val parametros = db.parametrosDao().obtenerParametros(vin)
            CoroutineScope(Dispatchers.Main).launch {
                callback(parametros) // Pasar el objeto ParametrosCoche obtenido al callback
            }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Error al obtener los datos: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

fun borrarParametros(context: Context, vin: String) {
    val db = AppDatabase.getDatabase(context)
    CoroutineScope(Dispatchers.IO).launch {
        try {
            db.parametrosDao().borrarParametros(vin)
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Datos borrados correctamente", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Error al borrar los datos: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
