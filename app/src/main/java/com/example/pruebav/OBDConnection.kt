package com.example.pruebav

import  android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
            delay(1000) // Pausa para estabilidad

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
                obdConnection!!.run(SetTimeoutCommand(200)) // en ms
                obdConnection!!.run(ResetAdapterCommand())
                delay(1000)
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
            val kmCodes = obdConnection!!.run(SafeObdCommand(DistanceSinceCodesClearedCommand())).formattedValue.takeIf { it.isNotBlank() } ?: "0"

            /*
            val numerCodes = obdConnection!!.run(SafeObdCommand(DTCNumberCommand())).formattedValue
            delay(300)

            val vinCommand = obdConnection!!.run(SafeObdCommand(VINCommand())).formattedValue
            delay(300)

            val kmCodes = obdConnection!!.run(SafeObdCommand(DistanceSinceCodesClearedCommand())).formattedValue
            delay(300)
            */

            //

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

        if (obdConnection == null) {
            Log.e("OBD", "Intento de lectura sin conexión activa1.")
            return datos
        }

        try {

            val troubleCodesCommand = SafeObdCommand(TroubleCodesCommand())
            val result = obdConnection!!.run(troubleCodesCommand)

            // Aquí asumimos que si formattedValue lanza excepción, no hay datos válidos.
            val listaErrores = if (result.formattedValue.isEmpty()) {
                null// No hay errores
            } else {
                result.formattedValue.trim().split(',', ' ').filter { it.isNotBlank() }
            }

            listaErrores?.let {
                datos["listaErrores"] = it.joinToString(separator = "\n")
            }
            Log.d("OBD:Lectura", "Codigos de error actualizados $datos")

        } catch (e: Exception) {
            Log.e("OBD:Lectura", "Probablemente sin errores: ${e.message}")
            e.printStackTrace() // Esto imprime la traza completa
            datos["listaErrores"] = "" // Manejo limpio en caso de error
        }
        return datos
    }

    suspend fun readAllParameters(): Map<String, String> {
        val datos = mutableMapOf<String, String>()

        if (obdConnection == null) {
            Log.e("OBD", "Intento de lectura sin conexión activa.")
            return datos
        }

        try {
            val rpm = obdConnection!!.run(SafeObdCommand(RPMCommand()))
            Log.d("rpm", ": ${rpm.formattedValue}")
            delay(1000)

            val speed = obdConnection!!.run(SafeObdCommand1(SpeedCommand()))
            Log.d("speed", ": ${speed.formattedValue}")
            delay(1000)

            val massAirFlow = try {
                obdConnection!!.run(SafeObdCommand1(MassAirFlowCommand()))
            } catch (e: Exception) {
                null
            }
            if (massAirFlow != null) {
                Log.d("massAirFlow", ": ${massAirFlow.formattedValue}")
            } else {
                Log.d("massAirFlow", ": No Data")
            }


            val runtime = obdConnection!!.run(SafeObdCommand1(RuntimeCommand()))
            Log.d("runtime", ": ${runtime.formattedValue}")
            delay(1000)


            val load = try {
                obdConnection!!.run(SafeObdCommand1(LoadCommand()))
            } catch (e: Exception) {
                null
            }
            Log.d("engineLoad", ": ${load?.formattedValue ?: "No Data"}")
            delay(1000)

            val absoluteLoad = try {
                obdConnection!!.run(SafeObdCommand1(AbsoluteLoadCommand()))
            } catch (e: Exception) {
                null
            }
            Log.d("engineAbsoluteLoad", ": ${absoluteLoad?.formattedValue ?: "No Data"}")
            delay(1000)

            val throttle = try {
                obdConnection!!.run(SafeObdCommand1(ThrottlePositionCommand()))
            } catch (e: Exception) {
                null
            }
            Log.d("throttlePosition", ": ${throttle?.formattedValue ?: "No Data"}")
            delay(1000)

            val relativeThrottle = try {
                obdConnection!!.run(SafeObdCommand1(RelativeThrottlePositionCommand()))
            } catch (e: Exception) {
                null
            }
            Log.d("relativeThrottlePosition", ": ${relativeThrottle?.formattedValue ?: "No Data"}")
            delay(1000)

            val fuelType = try {
                obdConnection!!.run(SafeObdCommand1(FuelTypeCommand()))
            } catch (e: Exception) {
                null
            }
            Log.d("fuelType", ": ${fuelType?.formattedValue ?: "No Data"}")
            delay(1000)

            val fuelLevel = try {
                obdConnection!!.run(SafeObdCommand1(FuelLevelCommand()))
            } catch (e: Exception) {
                null
            }
            Log.d("fuelLevel", ": ${fuelLevel?.formattedValue ?: "No Data"}")
            delay(1000)

            val barometricPressure = try {
                obdConnection!!.run(SafeObdCommand1(BarometricPressureCommand()))
            } catch (e: Exception) {
                null
            }
            Log.d("barometricPressure", ": ${barometricPressure?.formattedValue ?: "No Data"}")
            delay(1000)

            val intakeManifoldPressure = try {
                obdConnection!!.run(SafeObdCommand1(IntakeManifoldPressureCommand()))
            } catch (e: Exception) {
                null
            }
            Log.d("intakeManifoldPressure", ": ${intakeManifoldPressure?.formattedValue ?: "No Data"}")
            delay(1000)

            val fuelPressure = try {
                obdConnection!!.run(SafeObdCommand1(FuelPressureCommand()))
            } catch (e: Exception) {
                null
            }
            Log.d("fuelPressure", ": ${fuelPressure?.formattedValue ?: "No Data"}")
            delay(1000)

            val fuelRailPressure = try {
                obdConnection!!.run(SafeObdCommand1(FuelRailPressureCommand()))
            } catch (e: Exception) {
                null
            }
            Log.d("fuelRailPressure", ": ${fuelRailPressure?.formattedValue ?: "No Data"}")
            delay(1000)

            val fuelRailGaugePressure = try {
                obdConnection!!.run(SafeObdCommand1(FuelRailGaugePressureCommand()))
            } catch (e: Exception) {
                null
            }
            Log.d("fuelRailGaugePressure", ": ${fuelRailGaugePressure?.formattedValue ?: "No Data"}")
            delay(1000)

            val airIntakeTemperature = try {
                obdConnection!!.run(SafeObdCommand1(AirIntakeTemperatureCommand()))
            } catch (e: Exception) {
                null
            }
            Log.d("airIntakeTemperature", ": ${airIntakeTemperature?.formattedValue ?: "No Data"}")
            delay(1000)

            val ambientAirTemperature = try {
                obdConnection!!.run(SafeObdCommand1(AmbientAirTemperatureCommand()))
            } catch (e: Exception) {
                null
            }
            Log.d("ambientAirTemperature", ": ${ambientAirTemperature?.formattedValue ?: "No Data"}")
            delay(1000)

            val engineCoolantTemperature = try {
                obdConnection!!.run(SafeObdCommand1(EngineCoolantTemperatureCommand()))
            } catch (e: Exception) {
                null
            }
            Log.d("engineCoolantTemperature", ": ${engineCoolantTemperature?.formattedValue ?: "No Data"}")
            delay(1000)

            val oilTemperature = try {
                obdConnection!!.run(SafeObdCommand1(OilTemperatureCommand()))
            } catch (e: Exception) {
                null
            }
            Log.d("oilTemperature", ": ${oilTemperature?.formattedValue ?: "No Data"}")
            delay(1000)

            val commandedEgr = try {
                obdConnection!!.run(SafeObdCommand1(CommandedEgrCommand()))
            } catch (e: Exception) {
                null
            }
            Log.d("commandedEgr", ": ${commandedEgr?.formattedValue ?: "No Data"}")
            delay(1000)

            val egrError = try {
                obdConnection!!.run(SafeObdCommand1(EgrErrorCommand()))
            } catch (e: Exception) {
                null
            }
            Log.d("egrError", ": ${egrError?.formattedValue ?: "No Data"}")
            delay(1000)


            /*
            val fuelLevel = obdConnection!!.run(SafeObdCommand(FuelLevelCommand()))
            delay(1000)
            val coolantT = obdConnection!!.run(SafeObdCommand(EngineCoolantTemperatureCommand()))
            delay(1000)
            val intakeT = obdConnection!!.run(SafeObdCommand(AirIntakeTemperatureCommand()))
            delay(300)
            val load = obdConnection!!.run(SafeObdCommand(LoadCommand()))
            delay(300)
            val timeActive = obdConnection!!.run(SafeObdCommand(RuntimeCommand()))
            delay(300)
            val fuelConsume = obdConnection!!.run(SafeObdCommand(FuelConsumptionRateCommand()))
            delay(300)
            val barometricP = obdConnection!!.run(SafeObdCommand(BarometricPressureCommand()))
            delay(300)
            val railP = obdConnection!!.run(SafeObdCommand(FuelRailPressureCommand()))
            delay(300)
            val intakeP = obdConnection!!.run(SafeObdCommand(IntakeManifoldPressureCommand()))
            delay(300)
            val fuelP = obdConnection!!.run(SafeObdCommand(FuelPressureCommand()))
            delay(300)
            val throttlePosition = obdConnection!!.run(SafeObdCommand(ThrottlePositionCommand()))
            delay(300)*/


            datos["RPM"] = rpm.formattedValue
            datos["Nivel de combustible"] = fuelLevel!!.formattedValue
            datos["Tipo de combustible"] = fuelType!!.formattedValue

            /*
            datos["Nivel combustible"] = fuelLevel.formattedValue + fuelLevel.unit
            datos["Temperatura refrigerante"] = coolantT.formattedValue + coolantT.unit
            datos["Temperatura admision"] = intakeT.formattedValue+intakeT.unit
            datos["Carga motor"] = load.formattedValue+load.unit
            datos["Tiempo activo"] = timeActive.formattedValue+timeActive.unit
           // datos["Consumo combustible"] = fuelConsume.formattedValue+fuelConsume.unit
            datos["Presion exterior"] = barometricP.formattedValue+barometricP.unit
            datos["Presion rail"] = railP.formattedValue+railP.unit
            datos["Presion Admision"] = intakeP.formattedValue+intakeP.unit
            datos["Presion Combustible"] = fuelP.formattedValue+fuelP.unit
            datos["Posicion acelerador"] = throttlePosition.formattedValue+throttlePosition.unit*/


            Log.d("OBD:Lectura","Lectura de datos correctamente")


        } catch (e: NonNumericResponseException) {
            Log.e("OBD", "Respuesta no numérica: ${e.message}")

        } catch (e: Exception) {
            Log.e("OBD", "Error al leer parámetros: ${e.message}")
            e.printStackTrace()  // Esto también te da más detalles de la excepción
        }
        return datos
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

        Log.d(tag, "Filtered lines containing '49 02': $lines")

        // Extraer solo los bytes útiles de cada línea (ignorando cabeceras CAN y metadatos)
        val hexData = lines.flatMap { line ->
            val parts = line.split(Regex("\\s+"))
            Log.d(tag, "Parts of line: $parts")

            val startIndex = parts.indexOf("49")
            Log.d(tag, "Index of '49' in parts: $startIndex")

            if (startIndex >= 0 && parts.size > startIndex + 3) {
                val usefulData = parts.drop(startIndex + 3)
                Log.d(tag, "Useful data extracted from line: $usefulData")
                usefulData
            } else {
                Log.w(tag, "Line skipped due to unexpected format: $line")
                emptyList()
            }
        }

        // Convertir hex a texto ASCII
        val vin = hexData.joinToString("") {
            try {
                val char = Integer.parseInt(it, 16).toChar()
                Log.d(tag, "Hex '$it' -> Char '$char'")
                char.toString()
            } catch (e: NumberFormatException) {
                Log.e(tag, "Invalid hex byte: $it", e)
                ""
            }
        }.trim()

        Log.d(tag, "Parsed VIN: $vin")
        return vin
    }
}

/*
class CustomCommand : ObdCommand() {
    // Required
    override val tag = "CUSTOM_COMMAND"
    override val name = "Custom Command"
    override val mode = "01"
    override val pid = "FF"

    // Optional
    override val defaultUnit = ""
    override val handler = { it: ObdRawResponse -> "Calculations to parse value from ${it.processedValue}" }
}


class VinRawCommand : ObdCommand() {
    override val tag = "VIN_COMMAND"
    override val name = "VIN Command"
    override val mode = "09" // Comando para obtener el VIN
    override val pid = "02" // PID para obtener el VIN

    override val handler = { rawResponse: ObdRawResponse ->
        // Convertir la respuesta cruda a String y depurar
        val vinRawString = rawResponse.processedValue.toString()

        Log.d("VIN_RAW", "Raw VIN response: $vinRawString")

        // Filtrar solo caracteres alfanuméricos y tomar solo los 17 primeros caracteres
        vinRawString.filter { it.isLetterOrDigit() }.take(17)
    }
}
class VINCommand1 : ObdCommand() {
    override val tag = "VIN"
    override val name = "Vehicle Identification Number (VIN)"
    override val mode = "09"
    override val pid = "02"

    override val defaultUnit = ""
    override val handler = { it: ObdRawResponse ->
        // Paso 1: Eliminar caracteres no deseados
        val cleanedValue = removeAll(it.value, WHITESPACE_PATTERN, BUS_INIT_PATTERN)

        // Paso 2: Llamar a parseVIN para convertir a formato legible
        parseVIN(cleanedValue)
    }

    // Función para procesar y extraer el VIN
    private fun parseVIN(rawValue: String): String {
        val workingData =
            if (rawValue.contains(":")) {
                // CAN(ISO-15765) protocol.
                // 9 es el inicio de la secuencia con los bytes de información a seguir
                val value = rawValue.replace(".:".toRegex(), "").substring(9)
                if (STARTS_WITH_ALPHANUM_PATTERN.matcher(convertHexToString(value)).find()) {
                    rawValue.replace("0:49", "").replace(".:".toRegex(), "")
                } else {
                    value
                }
            } else {
                // Protocolos ISO9141-2, KWP2000 Fast y KWP2000 5Kbps (ISO15031)
                rawValue.replace("49020.".toRegex(), "")
            }

        // Convertir de hexadecimal a cadena
        return convertHexToString(workingData).replace("[\u0000-\u001f]".toRegex(), "")
    }

    // Función para convertir de hexadecimal a texto ASCII
    private fun convertHexToString(hex: String): String {
        val stringBuilder = StringBuilder()
        for (i in hex.indices step 2) {
            val hexPair = hex.substring(i, i + 2) // Obtener cada par de caracteres hexadecimales
            val decimal = Integer.parseInt(hexPair, 16) // Convertir el par hexadecimal a decimal
            stringBuilder.append(decimal.toChar()) // Convertir el valor decimal a un carácter
        }
        return stringBuilder.toString() // Retornar la cadena resultante
    }
}
 */