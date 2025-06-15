package com.example.pruebav.database


import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import com.example.pruebav.CocheMarcaModelo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Entity(
    tableName = "numero_vin",
    indices = [Index(value = ["vin"], unique = true)]
)
data class NumeroVin(
    @PrimaryKey val vin: String,

    @ColumnInfo(name = "marca", defaultValue = "'Desconocida'")
    val marca: String = "Desconocida",

    @ColumnInfo(name = "modelo", defaultValue = "'Desconocido'")
    val modelo: String = "Desconocido",

    @ColumnInfo(name = "caract")
    val caract: String? = null,

    @ColumnInfo(name = "anio_fabricacion")
    val anioFabricacion: Int? = null,

    @ColumnInfo(name = "numero_serie", defaultValue = "'-'")
    val numeroSerie: String = "-"
)
 {
    companion object {
        suspend fun decodeVinHttpClient(vin: String): NumeroVin? = withContext(Dispatchers.IO) {
            val apiUrl = "https://vpic.nhtsa.dot.gov/api/vehicles/decodevinvaluesextended/$vin?format=json"
            val wmiToMarca = mapOf(
                "WVW" to "Volkswagen",
                "WAU" to "Audi",
                "WBA" to "BMW",
                "WDB" to "Mercedes-Benz",
                "ZFA" to "Fiat",
                "ZFF" to "Ferrari",
                "ZAR" to "Alfa Romeo",
                "VF1" to "Renault",
                "VF3" to "Peugeot",
                "VSS" to "SEAT",
                "TMB" to "Skoda",
                "YS3" to "Saab",
                "YV1" to "Volvo",
                "SALL" to "Land Rover",
                "SAJ" to "Jaguar",
                "ZAM" to "Maserati",
                "ZHW" to "Lamborghini",
                "XLR" to "Dacia",
                "UU1" to "Dacia",
                "XL9" to "Spyker",
                "XMC" to "McLaren",
                "WME" to "Smart",
                "W0L" to "Opel",
                "VLU" to "Renault Trucks",
                "U5Y" to "Kia",
                "TMA" to "Hyundai",
                "SB1" to "Toyota",
                "VNK" to "Toyota",
                "TRU" to "Audi",
                "SHS" to "Honda",
                "SJN" to "Nissan"
            )

            fun obtenerMarcaDesdeWMI(vin: String): String {
                val wmi = vin.take(3).uppercase()
                return wmiToMarca[wmi] ?: "Desconocida"
            }

            try {
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val result = json.getJSONArray("Results").getJSONObject(0)

                val errorCode = result.optString("ErrorCode")
                val marcaFinal = obtenerMarcaDesdeWMI(vin)

                return@withContext if (errorCode != "0") {
                    NumeroVin(
                        vin = vin,
                        marca = marcaFinal,
                        modelo = "Desconocido",
                        caract = null,
                        anioFabricacion = null,
                        numeroSerie = "-"
                    )
                } else {
                    val fuelType = result.optString("FuelTypePrimary").takeIf { it.isNotBlank() }
                    val bodyClass = result.optString("BodyClass").takeIf { it.isNotBlank() }
                    val transmission = result.optString("TransmissionStyle").takeIf { it.isNotBlank() }

                    val caractList = listOfNotNull(fuelType, bodyClass, transmission)
                    val caract = if (caractList.isNotEmpty()) caractList.joinToString(" | ") else null

                    NumeroVin(
                        vin = vin,
                        marca = marcaFinal,
                        modelo = result.optString("Model").takeIf { it.isNotBlank() } ?: "Desconocido",
                        caract = caract,
                        anioFabricacion = result.optString("ModelYear").toIntOrNull(),
                        numeroSerie = result.optString("SerialNumber").takeIf { it.isNotBlank() } ?: "-"
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }
    }
}


@Dao
interface NumeroVinDao {

    @Insert
    suspend fun insertNumeroVin(numeroVin: NumeroVin)

    @Query("SELECT * FROM numero_vin WHERE vin = :vin")
    suspend fun getNumeroVin(vin: String): NumeroVin?

    @Query("SELECT vin FROM numero_vin")
    suspend fun getAllVins(): List<String>?

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT vin FROM numero_vin WHERE marca = :marca AND modelo = :modelo LIMIT 1")
    suspend fun getVinFromCoche(marca: String, modelo: String): String?

    @Query("SELECT marca, modelo FROM numero_vin")
    suspend fun getCoches(): List<CocheMarcaModelo>
}

//método de api de pago
/*
companion object {
        suspend fun decodeVinHttpClient(vin: String): NumeroVin? = withContext(Dispatchers.IO) {

            //val apiKey = "013f4cd0ad22"
            //val secretKey = "7e09a035e6"
            //val apiUrl2 = "https://api.vindecoder.eu/3.2/$apiKey/$secretKey/decode_vin/$vin.json\n"

            try {
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val data = json.getJSONArray("decode").getJSONObject(0)

                val marca = data.optString("make", "Desconocida")
                val modelo = data.optString("model", "Desconocido")
                val anioFabricacion = data.optString("year").toIntOrNull()
                val numeroSerie = data.optString("serial_number", "-")

                val fuel = data.optString("fuel_type")
                val body = data.optString("body_type")
                val gearbox = data.optString("gearbox")

                val caractList = listOfNotNull(fuel, body, gearbox)
                val caract = if (caractList.isNotEmpty()) caractList.joinToString(" | ") else null

                NumeroVin(
                    vin = vin,
                    marca = marca,
                    modelo = modelo,
                    caract = caract,
                    anioFabricacion = anioFabricacion,
                    numeroSerie = numeroSerie
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
 */
