package com.example.pruebav

import  android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import com.github.eltonvs.obd.command.NonNumericResponseException
import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdProtocols
import com.github.eltonvs.obd.command.ObdRawResponse
import com.github.eltonvs.obd.command.ObdResponse
import com.github.eltonvs.obd.command.RegexPatterns
import com.github.eltonvs.obd.command.RegexPatterns.BUS_INIT_PATTERN
import com.github.eltonvs.obd.command.RegexPatterns.STARTS_WITH_ALPHANUM_PATTERN
import com.github.eltonvs.obd.command.RegexPatterns.WHITESPACE_PATTERN
import com.github.eltonvs.obd.command.Switcher
import com.github.eltonvs.obd.command.control.DistanceMILOnCommand
import com.github.eltonvs.obd.command.control.TroubleCodesCommand
import com.github.eltonvs.obd.command.control.VINCommand
import com.github.eltonvs.obd.command.engine.LoadCommand
import com.github.eltonvs.obd.command.engine.RPMCommand
import com.github.eltonvs.obd.command.engine.RuntimeCommand
import com.github.eltonvs.obd.command.engine.ThrottlePositionCommand
import com.github.eltonvs.obd.command.fuel.FuelConsumptionRateCommand
import com.github.eltonvs.obd.command.fuel.FuelLevelCommand
import com.github.eltonvs.obd.command.pressure.BarometricPressureCommand
import com.github.eltonvs.obd.command.pressure.FuelPressureCommand
import com.github.eltonvs.obd.command.pressure.FuelRailPressureCommand
import com.github.eltonvs.obd.command.pressure.IntakeManifoldPressureCommand
import com.github.eltonvs.obd.command.temperature.AirIntakeTemperatureCommand
import com.github.eltonvs.obd.command.temperature.EngineCoolantTemperatureCommand
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import com.github.eltonvs.obd.command.at.DescribeProtocolCommand
import com.github.eltonvs.obd.command.at.ResetAdapterCommand
import com.github.eltonvs.obd.command.at.SelectProtocolCommand
import com.github.eltonvs.obd.command.at.SetEchoCommand
import com.github.eltonvs.obd.command.at.SetHeadersCommand
import com.github.eltonvs.obd.command.at.SetLineFeedCommand
import com.github.eltonvs.obd.command.at.SetSpacesCommand
import com.github.eltonvs.obd.command.at.SetTimeoutCommand
import com.github.eltonvs.obd.command.control.DTCNumberCommand
import com.github.eltonvs.obd.command.control.DistanceSinceCodesClearedCommand
import com.github.eltonvs.obd.command.control.ResetTroubleCodesCommand
import com.github.eltonvs.obd.command.control.TimeSinceCodesClearedCommand
import com.github.eltonvs.obd.command.egr.CommandedEgrCommand
import com.github.eltonvs.obd.command.egr.EgrErrorCommand
import com.github.eltonvs.obd.command.engine.AbsoluteLoadCommand
import com.github.eltonvs.obd.command.engine.MassAirFlowCommand
import com.github.eltonvs.obd.command.engine.RelativeThrottlePositionCommand
import com.github.eltonvs.obd.command.engine.SpeedCommand
import com.github.eltonvs.obd.command.fuel.EthanolLevelCommand
import com.github.eltonvs.obd.command.fuel.FuelAirEquivalenceRatioCommand
import com.github.eltonvs.obd.command.fuel.FuelTrimCommand
import com.github.eltonvs.obd.command.fuel.FuelTypeCommand
import com.github.eltonvs.obd.command.pressure.FuelRailGaugePressureCommand
import com.github.eltonvs.obd.command.removeAll
import com.github.eltonvs.obd.command.temperature.AmbientAirTemperatureCommand
import com.github.eltonvs.obd.command.temperature.OilTemperatureCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.TimeoutException
import kotlin.math.log

class OBDConnection(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var sockeT: BluetoothSocket?=null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var obdConnection: ObdDeviceConnection? = null
    private var isConnecting = false


    suspend fun connectToOBD(device:BluetoothDevice) = withContext(Dispatchers.IO) {
        if (isConnecting) {
            Log.d("OBD", "Ya hay una conexión en curso, esperando a que finalice.")
            return@withContext
        }
        isConnecting = true
        disconnectOBD() // Cerrar conexiones previas
        try {
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                Log.e("OBD", "Bluetooth no disponible o desactivado")
                return@withContext
            }
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e("OBD", "Permiso BLUETOOTH_CONNECT no concedido")
                return@withContext
            }

            sockeT = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            delay(1000)

            try {
                sockeT!!.connect()
            } catch (e: IOException) {
                Log.e("OBD", "Error al conectar socket: ${e.message}", e)
                disconnectOBD()
                return@withContext
            }

            inputStream = sockeT?.inputStream
            outputStream = sockeT?.outputStream
            obdConnection = ObdDeviceConnection(inputStream!!, outputStream!!)
            Log.d("OBD", "Conectado a OBD-II exitosamente")


            /*
            val buffer = ByteArray(1024)
            while ((inputStream?.available() ?: 0) > 0) {
                inputStream?.read(buffer)
            }*/


            val describeProtocol = DescribeProtocolCommand()
            val response = obdConnection!!.run(describeProtocol)
            Log.d("OBD-Protocol", "Protocolo actual: ${describeProtocol.format(response)}")

            try{
                obdConnection!!.run(SetTimeoutCommand(50)) // en ms
                obdConnection!!.run(ResetAdapterCommand())
                delay(200)
                obdConnection!!.run(SetEchoCommand(Switcher.OFF))
                delay(300)
                obdConnection!!.run(SetLineFeedCommand(Switcher.OFF))
                delay(300)
                obdConnection!!.run(SelectProtocolCommand(ObdProtocols.ISO_15765_4_CAN))
                delay(1000)

                //obdConnection!!.run(SetSpacesCommand(Switcher.OFF))
                //obdConnection!!.run(SetHeadersCommand(Switcher.OFF))

                Log.d("OBDCommand","Comandos AT ejecutados correctamente")

            }catch(e: TimeoutException) {
                Log.e("OBDCommand", "Se excedió el tiempo de espera.")
            } catch(e: IOException) {
                Log.e("OBDCommand", "Error de comunicación con el adaptador OBD.")
            } catch(e: Exception) {
                Log.e("OBDCommand", "Error inesperado: ${e.message ?: "Sin mensaje"}", e)
            }

            Log.d("OBD:Conexión","Conectado a la ECU")
            withContext(Dispatchers.Main) {
                OBDManager.viewModel?.setECUReady(true) // <-- Necesitas pasar el ViewModel a OBDManager
            }

        } catch (e: Exception) {
            Log.e("OBD", "Error al conectar al OBD-II: ${e.message}", e)
            disconnectOBD()
        } finally {
            isConnecting = false
        }
    }

    fun disconnectOBD() {
        try {
            inputStream?.close()
            outputStream?.close()
            sockeT?.close()
            Log.d("Conexion", "Desconectado correctamente")
        } catch (e: Exception) {
            Log.e("Conexion", "Error al cerrar la conexión: ${e.message}", e)
        } finally {
            obdConnection = null
            inputStream = null
            outputStream = null
            sockeT = null
        }
    }

    //funciones de lectura

    suspend fun readFirst(): Map<String,String>{
        val datos = mutableMapOf<String,String>()

        if (obdConnection == null) {
            Log.e("OBD", "Intento de lectura sin conexión activa.")
            return datos
        }

        try{

            val numerCodes = obdConnection!!.run(SafeObdCommand(DTCNumberCommand())).formattedValue.takeIf { it.isNotBlank() } ?: "0"
            delay(300)
            val vinCommand = obdConnection!!.run(SafeObdCommand(VINCommand1())).formattedValue.takeIf { it.isNotBlank() } ?: "No disponible"
            delay(300)

            val distanceCommand =obdConnection!!.run(SafeObdCommand(DistanceSinceCodesClearedCommand()))
            val rawKm = distanceCommand.formattedValue
            val kmCodes = if (rawKm == "65535Km") {
                "Disponible en cuadro"
            } else {
                "$rawKm km"
            }

            /*

            val fuelAirEquivalence1 = obdConnection!!.run(SafeObdCommand(FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_1))).formattedValue
            delay(300)
            val fuelAirEquivalence2 = obdConnection!!.run(SafeObdCommand(FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_2))).formattedValue
            delay(300)
            val fuelAirEquivalence3 = obdConnection!!.run(SafeObdCommand(FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_3))).formattedValue
            delay(300)
            val fuelAirEquivalence4 = obdConnection!!.run(SafeObdCommand(FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_4))).formattedValue
            delay(300)
            val fuelAirEquivalence5 = obdConnection!!.run(SafeObdCommand(FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_5))).formattedValue
            delay(300)
            val fuelAirEquivalence6 = obdConnection!!.run(SafeObdCommand(FuelAirEquivalenceRatioCommand(FuelAirEquivalenceRatioCommand.OxygenSensor.OXYGEN_SENSOR_6))).formattedValue
            delay(300)

            val mezcla = listOf(
                fuelAirEquivalence1,
                fuelAirEquivalence2,
                fuelAirEquivalence3,
                fuelAirEquivalence4,
                fuelAirEquivalence5,
                fuelAirEquivalence6
            ).map { it.toDoubleOrNull() ?: 1.0 }.average()

            */

            datos["numerCodes"] = numerCodes
            datos["vin"] = vinCommand
            datos["km"] = kmCodes
            //datos["mezcla"] = mezcla.toString()


        }catch(e:Exception){
            Log.e("OBD:Lectura","Fallo lectura main: ${e.message}")
        }
        return datos
    }

    suspend fun readCodes(): Map<String, String> {
        val datos = mutableMapOf<String, String>()

        try {

            val troubleCodesCommand = SafeObdCommand(TroubleCodesCommand())
            val result = obdConnection!!.run(troubleCodesCommand)

            val listaErrores = if (result.formattedValue.isEmpty()) {
                null
                //Log.d("OBD:Lectura","no hay errores")
            } else {
                result.formattedValue.trim().split(',', ' ').filter { it.isNotBlank() }
            }

            listaErrores?.let {
                datos["listaErrores"] = it.joinToString(separator = "\n")
            }
            Log.d("OBD:Lectura", "Codigos de error leidos $datos")

        } catch (e: Exception) {
            Log.e("OBD:Lectura", "Probablemente sin errores: ${e.message}")
            e.printStackTrace()
            datos["listaErrores"] = ""
        }
        return datos
    }

    suspend fun deleteCodes(): Boolean {
        return try {
            val delete = obdConnection?.run(SafeObdCommand1(ResetTroubleCodesCommand()))
            delete != null
        } catch (e: Exception) {
            Log.d("OBD:Delete", "Error al borrar los códigos: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun readAllParameters(): Map<String, String> {
        val datos = mutableMapOf<String, String>()

        try {
            try {
                val rpm = obdConnection!!.run(SafeObdCommand1(RPMCommand()), delayTime = 100)
                datos["RPM"] = rpm.formattedValue
            } catch (e: Exception) {
                datos["RPM"] = "No Data"
            }
            Log.d("rpm", ": ${datos["RPM"]}")

            try {
                val speed = obdConnection!!.run(SafeObdCommand1(SpeedCommand()))
                datos["Speed"] = speed.formattedValue
            } catch (e: Exception) {
                datos["Speed"] = "No Data"
            }
            Log.d("speed", ": ${datos["Speed"]}")
            delay(150)

            /*
            val massAirFlow = try {
                obdConnection!!.run(SafeObdCommand1(MassAirFlowCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Mass Air Flow"] = massAirFlow?.formattedValue ?: "No Data"
            Log.d("massAirFlow", ": ${datos["Mass Air Flow"]}")
            delay(150)
            try {
                val runtime = obdConnection!!.run(SafeObdCommand1(RuntimeCommand()))
                datos["Runtime"] = runtime.formattedValue
            } catch (e: Exception) {
                datos["Runtime"] = "No Data"
            }
            Log.d("runtime", ": ${datos["Runtime"]}")
            delay(150)*/

            val load = try {
                obdConnection!!.run(SafeObdCommand1(LoadCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Engine Load"] = load?.formattedValue ?: "No Data"
            Log.d("engineLoad", ": ${datos["Engine Load"]}")
            delay(150)

            /*
            val absoluteLoad = try {
                obdConnection!!.run(SafeObdCommand1(AbsoluteLoadCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Absolute Load"] = absoluteLoad?.formattedValue ?: "No Data"
            Log.d("engineAbsoluteLoad", ": ${datos["Absolute Load"]}")
            delay(150)*/

            val throttle = try {
                obdConnection!!.run(SafeObdCommand1(ThrottlePositionCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Throttle Position"] = throttle?.formattedValue ?: "No Data"
            Log.d("throttlePosition", ": ${datos["Throttle Position"]}")
            delay(150)

            /*
            val relativeThrottle = try {
                obdConnection!!.run(SafeObdCommand1(RelativeThrottlePositionCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Relative Throttle Position"] = relativeThrottle?.formattedValue ?: "No Data"
            Log.d("relativeThrottlePosition", ": ${datos["Relative Throttle Position"]}")
            delay(150)*/

            val fuelType = try {
                obdConnection!!.run(SafeObdCommand1(FuelTypeCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Fuel Type"] = fuelType?.formattedValue ?: "No Data"
            Log.d("fuelType", ": ${datos["Fuel Type"]}")
            delay(150)

            val fuelLevel = try {
                obdConnection!!.run(SafeObdCommand1(FuelLevelCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Fuel Level"] = fuelLevel?.formattedValue ?: "No Data"
            Log.d("fuelLevel", ": ${datos["Fuel Level"]}")
            delay(150)

            /*
            val barometricPressure = try {
                obdConnection!!.run(SafeObdCommand1(BarometricPressureCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Barometric Pressure"] = barometricPressure?.formattedValue ?: "No Data"
            Log.d("barometricPressure", ": ${datos["Barometric Pressure"]}")
            delay(150)*/

            val intakeManifoldPressure = try {
                obdConnection!!.run(SafeObdCommand1(IntakeManifoldPressureCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Intake Manifold Pressure"] = intakeManifoldPressure?.formattedValue ?: "No Data"
            Log.d("intakeManifoldPressure", ": ${datos["Intake Manifold Pressure"]}")
            delay(150)

            val fuelPressure = try {
                obdConnection!!.run(SafeObdCommand1(FuelPressureCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Fuel Pressure"] = fuelPressure?.formattedValue ?: "No Data"
            Log.d("fuelPressure", ": ${datos["Fuel Pressure"]}")
            delay(150)

            val fuelRailPressure = try {
                obdConnection!!.run(SafeObdCommand1(FuelRailPressureCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Fuel Rail Pressure"] = fuelRailPressure?.formattedValue ?: "No Data"
            Log.d("fuelRailPressure", ": ${datos["Fuel Rail Pressure"]}")
            delay(150)

            val fueltrim = try{
                obdConnection!!.run(SafeObdCommand1(FuelTrimCommand(FuelTrimCommand.FuelTrimBank.LONG_TERM_BANK_1)))
                obdConnection!!.run(SafeObdCommand1(FuelTrimCommand(FuelTrimCommand.FuelTrimBank.LONG_TERM_BANK_2)))
                obdConnection!!.run(SafeObdCommand1(FuelTrimCommand(FuelTrimCommand.FuelTrimBank.SHORT_TERM_BANK_1)))
                obdConnection!!.run(SafeObdCommand1(FuelTrimCommand(FuelTrimCommand.FuelTrimBank.SHORT_TERM_BANK_2)))
            }  catch(e:Exception){
                null
            }
            Log.d("fuelTrim", fueltrim!!.formattedValue)

            /*
            val fuelRailGaugePressure = try {
                obdConnection!!.run(SafeObdCommand1(FuelRailGaugePressureCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Fuel Rail Gauge Pressure"] = fuelRailGaugePressure?.formattedValue ?: "No Data"
            Log.d("fuelRailGaugePressure", ": ${datos["Fuel Rail Gauge Pressure"]}")
            delay(150)*/

            val airIntakeTemperature = try {
                obdConnection!!.run(SafeObdCommand1(AirIntakeTemperatureCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Air Intake Temperature"] = airIntakeTemperature?.formattedValue ?: "No Data"
            Log.d("airIntakeTemperature", ": ${datos["Air Intake Temperature"]}")
            delay(150)

            val ambientAirTemperature = try {
                obdConnection!!.run(SafeObdCommand1(AmbientAirTemperatureCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Ambient Air Temperature"] = ambientAirTemperature?.formattedValue ?: "No Data"
            Log.d("ambientAirTemperature", ": ${datos["Ambient Air Temperature"]}")
            delay(150)

            val engineCoolantTemperature = try {
                obdConnection!!.run(SafeObdCommand1(EngineCoolantTemperatureCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Engine Coolant Temperature"] = engineCoolantTemperature?.formattedValue ?: "No Data"
            Log.d("engineCoolantTemperature", ": ${datos["Engine Coolant Temperature"]}")
            delay(150)

            val oilTemperature = try {
                obdConnection!!.run(SafeObdCommand1(OilTemperatureCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Oil Temperature"] = oilTemperature?.formattedValue ?: "No Data"
            Log.d("oilTemperature", ": ${datos["Oil Temperature"]}")
            delay(150)


        } catch (e: NonNumericResponseException) {
            Log.e("OBD", "Respuesta no numérica: ${e.message}")
        } catch (e: Exception) {
            Log.e("OBD", "Error al leer parámetros: ${e.message}")
            e.printStackTrace()
        }

        return datos
    }

    suspend fun readBasicParameters(): Map<String, String> = coroutineScope {
        val obdConn = obdConnection ?: return@coroutineScope emptyMap<String, String>()

        val rpmDeferred = async {
            try {
                "RPM" to obdConn.run(SafeObdCommand(RPMCommand())).formattedValue
            } catch (e: Exception) {
                "RPM" to "Error"
            }
        }

        val speedDeferred = async {
            try {
                "Speed" to obdConn.run(SafeObdCommand(SpeedCommand())).formattedValue
            } catch (e: Exception) {
                "Speed" to "Error"
            }
        }

        val tempDeferred = async {
            try {
                "Engine Coolant Temperature" to obdConn.run(SafeObdCommand(EngineCoolantTemperatureCommand())).formattedValue
            } catch (e: Exception) {
                "Engine Coolant Temperature" to "Error"
            }
        }

        listOf(
            rpmDeferred.await(),
            speedDeferred.await(),
            tempDeferred.await()
        ).toMap()
    }

    suspend fun readAllFastParameters(): Map<String, String> = coroutineScope {
        val obdConn = obdConnection ?: return@coroutineScope emptyMap()

        val comandos: List<Pair<String, ObdCommand>> = listOf(
            "RPM" to RPMCommand(),
            "Speed" to SpeedCommand(),
            "Engine Load" to LoadCommand(),
            "Throttle Position" to ThrottlePositionCommand(),
            "Fuel Type" to FuelTypeCommand(),
            "Fuel Level" to FuelLevelCommand(),
            "Intake Manifold Pressure" to IntakeManifoldPressureCommand(),
            "Fuel Pressure" to FuelPressureCommand(),
            "Fuel Rail Pressure" to FuelRailPressureCommand(),
            "Air Intake Temperature" to AirIntakeTemperatureCommand(),
            "Ambient Air Temperature" to AmbientAirTemperatureCommand(),
            "Engine Coolant Temperature" to EngineCoolantTemperatureCommand(),
            "Oil Temperature" to OilTemperatureCommand()
        )

        val deferreds = comandos.map { (nombre, cmd) ->
            async {
                val resultado = try {
                    val res = obdConn.run(SafeObdCommand1(cmd))
                    nombre to res.formattedValue
                } catch (e: Exception) {
                    nombre to "No Data"
                }
                Log.d("OBD", "${resultado.first}: ${resultado.second}")
                resultado
            }
        }

        deferreds.awaitAll().toMap()
    }

}

//clases

class SafeObdCommand(
    private val baseCommand: ObdCommand
) : ObdCommand() {
    override val tag = baseCommand.tag
    override val name = baseCommand.name
    override val mode = baseCommand.mode
    override val pid = baseCommand.pid
    override val defaultUnit = baseCommand.defaultUnit
    override val skipDigitCheck = baseCommand.skipDigitCheck

    override val handler: (ObdRawResponse) -> String = {
        val cleanedValue = it.value.replace(Regex("[^A-Fa-f0-9 ]"), "").trim()
        baseCommand.handler(it.copy(value = cleanedValue))
    }
}

class SafeObdCommand1(
    private val baseCommand: ObdCommand
) : ObdCommand() {

    override val tag = baseCommand.tag
    override val name = baseCommand.name
    override val mode = baseCommand.mode
    override val pid = baseCommand.pid
    override val defaultUnit = baseCommand.defaultUnit
    override val skipDigitCheck = baseCommand.skipDigitCheck

    override val handler: (ObdRawResponse) -> String = { response ->
        val rawValue = response.value.trim().uppercase()

        if (rawValue.contains("NO DATA") || rawValue.contains("?") || rawValue.isBlank()) {
            "No Data"
        } else {
            try {
                val cleanedValue = rawValue.replace(Regex("[^A-F0-9 ]"), "").trim()
                baseCommand.handler(response.copy(value = cleanedValue))
            } catch (e: Exception) {
                "Error"
            }
        }
    }
}

class VINCommand1 : ObdCommand() {
    override val tag = "VIN"
    override val name = "Vehicle Identification Number (VIN)"
    override val mode = "09"
    override val pid = "02"

    override val defaultUnit = ""
    override val handler = { it: ObdRawResponse -> parseVIN(it.value) }

    private fun parseVIN(rawValue: String): String {
        Log.d(tag, "Raw VIN response:\n$rawValue")

        // Limpiar saltos de línea, espacios y caracteres innecesarios
        val lines = rawValue.lines()
            .map { it.trim() }
            .filter { it.contains("49 02") }

        //Log.d(tag, "Filtered lines containing '49 02': $lines")

        // Extraer solo los bytes útiles de cada línea (ignorando cabeceras CAN y metadatos)
        val hexData = lines.flatMap { line ->
            val parts = line.split(Regex("\\s+"))
            //Log.d(tag, "Parts of line: $parts")

            val startIndex = parts.indexOf("49")
            //Log.d(tag, "Index of '49' in parts: $startIndex")

            if (startIndex >= 0 && parts.size > startIndex + 3) {
                val usefulData = parts.drop(startIndex + 3)
                //Log.d(tag, "Useful data extracted from line: $usefulData")
                usefulData
            } else {
                //Log.w(tag, "Line skipped due to unexpected format: $line")
                emptyList()
            }
        }

        // Convertir hex a texto ASCII
        val vin = hexData.joinToString("") {
            try {
                val char = Integer.parseInt(it, 16).toChar()
                //Log.d(tag, "Hex '$it' -> Char '$char'")
                char.toString()
            } catch (e: NumberFormatException) {
                //Log.e(tag, "Invalid hex byte: $it", e)
                ""
            }
        }.trim()

        Log.d(tag, "Parsed VIN: $vin")
        return vin
    }
}

class TurboRpmCommand : ObdCommand() {
    override val tag = "TURBO_RPM"
    override val name = "Turbocharger RPM"
    override val mode = "01"
    override val pid = "74"

    override val defaultUnit = "RPM"

    override val handler = { it: ObdRawResponse ->
        val bytes = it.bufferedValue
        if (bytes.size >= 3) {
            val A = bytes[1] and 0xFF
            val B = bytes[2] and 0xFF
            val rpm = (A * 256) + B
            rpm.toString()
        } else {
            "N/D"
        }
    }
}
class TurboTemperatureCommand : ObdCommand() {
    override val tag = "TURBO_TEMPERATURE"
    override val name = "Turbocharger Temperature"
    override val mode = "01"
    override val pid = "75"

    override val defaultUnit = "°C"

    override val handler = { it: ObdRawResponse ->
        val bytes = it.bufferedValue
        if (bytes.size >= 2) {
            val A = bytes[1] and 0xFF
            val temp = A - 40
            temp.toString()
        } else {
            "N/D"
        }
    }
}



