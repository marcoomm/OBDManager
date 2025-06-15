package com.example.pruebav.database

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.withContext

@Entity(
    tableName = "errores_coche",
    indices = [
        Index(value = ["vin", "codigoError"], unique = true),
        Index(value = ["codigoError"])
    ]
)
data class ErroresCoche(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "codigoError") val codigoError: String,
    @ColumnInfo(name = "vin") val vin: String,
    @ColumnInfo(name = "descripcion") val descripcion: String?
)

@Dao
interface ErroresCocheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarErroresCoche(errores:List<ErroresCoche>)

    @Query("SELECT * FROM errores_coche WHERE vin = :vin")
    suspend fun obtenerErroresCoche(vin: String): List<ErroresCoche>?

    @Query("DELETE FROM errores_coche WHERE vin = :vin")
    suspend fun borrarErroresPorVin(vin: String)
}

fun guardarErrores(context: Context, vin: String, errores: List<ErroresCoche>) {
    val db = AppDatabase.getDatabase(context)
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val erroresAGuardar = errores.ifEmpty {
                listOf(
                    ErroresCoche(
                        codigoError = "NO_ERROR",
                        vin = vin,
                        descripcion = "No se detectaron errores en el análisis"
                    )
                )
            }

            db.erroresDao().insertarErroresCoche(erroresAGuardar)

            val pa = db.erroresDao().obtenerErroresCoche(vin)
            Log.d("BBDD", pa.toString())

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Errores guardados correctamente", Toast.LENGTH_SHORT).show()
                Log.d("OBD", "Guardados correctamente")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error al guardar los errores: ${e.message}", Toast.LENGTH_LONG).show()
                Log.d("OBD", "Error al guardar")
            }
        }
    }
}
fun borrarErrores(context: Context, vin: String) {
    val db = AppDatabase.getDatabase(context)
    CoroutineScope(Dispatchers.IO).launch {
        try {
            db.erroresDao().borrarErroresPorVin(vin)
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Errores borrados correctamente", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Error al borrar los errores: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}




