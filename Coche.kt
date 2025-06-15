package com.example.pruebav.database

import android.content.Context
import android.widget.Toast
import androidx.room.ColumnInfo
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
import kotlinx.coroutines.launch

@Entity(
    tableName = "coche",
    indices = [Index(value = ["vin"], unique = true)]
)
data class Coche(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "vin") val vin: String,
    @ColumnInfo(name = "nombre") val nombre: String?,
    @ColumnInfo(name = "fecha_conexion") val fechaConexion: String
)
@Dao
interface CocheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarCoche(coche: Coche)

    @Query("SELECT * FROM coche")
    suspend fun obtenerTodosLosCoches(): List<Coche>
}

fun guardarCoche(context: Context, vin: String, nombre: String?, fecha: Long = System.currentTimeMillis()) {
    val coche = Coche(vin = vin, nombre = nombre, fechaConexion = fecha.toString())
    val db = AppDatabase.getDatabase(context)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            db.cocheDao().insertarCoche(coche)
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Coche guardado correctamente", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Error al guardar el coche: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

fun obtenerTodosLosCoches(context: Context, callback: (List<Coche>) -> Unit) {
    val db = AppDatabase.getDatabase(context)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val coches = db.cocheDao().obtenerTodosLosCoches()
            CoroutineScope(Dispatchers.Main).launch {
                callback(coches)
            }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Error al obtener los coches: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}