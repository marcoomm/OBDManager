package com.example.pruebav

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.github.eltonvs.obd.command.NonNumericResponseException
import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdProtocols
import com.github.eltonvs.obd.command.ObdRawResponse
import com.github.eltonvs.obd.command.Switcher
import com.github.eltonvs.obd.command.at.DescribeProtocolCommand
import com.github.eltonvs.obd.command.at.ResetAdapterCommand
import com.github.eltonvs.obd.command.at.SelectProtocolCommand
import com.github.eltonvs.obd.command.at.SetEchoCommand
import com.github.eltonvs.obd.command.at.SetLineFeedCommand
import com.github.eltonvs.obd.command.at.SetTimeoutCommand
import com.github.eltonvs.obd.command.control.DTCNumberCommand
import com.github.eltonvs.obd.command.control.DistanceSinceCodesClearedCommand
import com.github.eltonvs.obd.command.control.ResetTroubleCodesCommand
import com.github.eltonvs.obd.command.control.TroubleCodesCommand
import com.github.eltonvs.obd.command.engine.LoadCommand
import com.github.eltonvs.obd.command.engine.RPMCommand
import com.github.eltonvs.obd.command.engine.SpeedCommand
import com.github.eltonvs.obd.command.engine.ThrottlePositionCommand
import com.github.eltonvs.obd.command.fuel.FuelLevelCommand
import com.github.eltonvs.obd.command.fuel.FuelTypeCommand
import com.github.eltonvs.obd.command.pressure.FuelPressureCommand
import com.github.eltonvs.obd.command.pressure.FuelRailPressureCommand
import com.github.eltonvs.obd.command.pressure.IntakeManifoldPressureCommand
import com.github.eltonvs.obd.command.temperature.AirIntakeTemperatureCommand
import com.github.eltonvs.obd.command.temperature.AmbientAirTemperatureCommand
import com.github.eltonvs.obd.command.temperature.EngineCoolantTemperatureCommand
import com.github.eltonvs.obd.command.temperature.OilTemperatureCommand
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.TimeoutException

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
        disconnectOBD()
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


            val describeProtocol = DescribeProtocolCommand()
            val response = obdConnection!!.run(describeProtocol)
            Log.d("OBD-Protocol", "Protocolo actual: ${describeProtocol.format(response)}")

            try{
                obdConnection!!.run(SetTimeoutCommand(50))
                obdConnection!!.run(ResetAdapterCommand())
                delay(200)
                obdConnection!!.run(SetEchoCommand(Switcher.OFF))
                delay(300)
                obdConnection!!.run(SetLineFeedCommand(Switcher.OFF))
                delay(300)
                obdConnection!!.run(SelectProtocolCommand(ObdProtocols.ISO_15765_4_CAN))
                delay(300)

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
                OBDManager.viewModel?.setECUReady(true)
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

            datos["numerCodes"] = numerCodes
            datos["vin"] = vinCommand
            datos["km"] = kmCodes

        }catch(e:Exception){
            Log.e("OBD:Lectura","Fallo lectura main: ${e.message}")
        }
        return datos
    }

    suspend fun readCodes(): Map<String, String> {
        val datos = mutableMapOf<String, String>()

        try{
            val troubleCodesCommand = SafeObdCommand(TroubleCodesCommand())
            val result = obdConnection!!.run(troubleCodesCommand)

            val listaErrores = if (result.formattedValue.isEmpty()) {
                null
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
            val delete = obdConnection?.run(SafeObdCommand(ResetTroubleCodesCommand()))
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

            try {
                val speed = obdConnection!!.run(SafeObdCommand1(SpeedCommand()))
                datos["Speed"] = speed.formattedValue
            } catch (e: Exception) {
                datos["Speed"] = "No Data"
            }
            delay(150)

            val load = try {
                obdConnection!!.run(SafeObdCommand1(LoadCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Engine Load"] = load?.formattedValue ?: "No Data"
            delay(150)

            val throttle = try {
                obdConnection!!.run(SafeObdCommand1(ThrottlePositionCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Throttle Position"] = throttle?.formattedValue ?: "No Data"
            delay(150)

            val fuelType = try {
                obdConnection!!.run(SafeObdCommand1(FuelTypeCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Fuel Type"] = fuelType?.formattedValue ?: "No Data"
            delay(150)

            val fuelLevel = try {
                obdConnection!!.run(SafeObdCommand1(FuelLevelCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Fuel Level"] = fuelLevel?.formattedValue ?: "No Data"
            delay(150)

            val intakeManifoldPressure = try {
                obdConnection!!.run(SafeObdCommand1(IntakeManifoldPressureCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Intake Manifold Pressure"] = intakeManifoldPressure?.formattedValue ?: "No Data"
            delay(150)

            val fuelPressure = try {
                obdConnection!!.run(SafeObdCommand1(FuelPressureCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Fuel Pressure"] = fuelPressure?.formattedValue ?: "No Data"
            delay(150)

            val fuelRailPressure = try {
                obdConnection!!.run(SafeObdCommand1(FuelRailPressureCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Fuel Rail Pressure"] = fuelRailPressure?.formattedValue ?: "No Data"
            delay(150)

            val airIntakeTemperature = try {
                obdConnection!!.run(SafeObdCommand1(AirIntakeTemperatureCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Air Intake Temperature"] = airIntakeTemperature?.formattedValue ?: "No Data"
            delay(150)

            val ambientAirTemperature = try {
                obdConnection!!.run(SafeObdCommand1(AmbientAirTemperatureCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Ambient Air Temperature"] = ambientAirTemperature?.formattedValue ?: "No Data"
            delay(150)

            val engineCoolantTemperature = try {
                obdConnection!!.run(SafeObdCommand1(EngineCoolantTemperatureCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Engine Coolant Temperature"] = engineCoolantTemperature?.formattedValue ?: "No Data"
            delay(150)

            val oilTemperature = try {
                obdConnection!!.run(SafeObdCommand1(OilTemperatureCommand()))
            } catch (e: Exception) {
                null
            }
            datos["Oil Temperature"] = oilTemperature?.formattedValue ?: "No Data"
            delay(150)


        } catch (e: NonNumericResponseException) {
            Log.e("OBD", "Respuesta no numérica: ${e.message}")
        } catch (e: Exception) {
            Log.e("OBD", "Error al leer parámetros: ${e.message}")
            e.printStackTrace()
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

        val lines = rawValue.lines()
            .map { it.trim() }
            .filter { it.contains("49 02") }

        val hexData = lines.flatMap { line ->
            val parts = line.split(Regex("\\s+"))

            val startIndex = parts.indexOf("49")

            if (startIndex >= 0 && parts.size > startIndex + 3) {
                val usefulData = parts.drop(startIndex + 3)
                usefulData
            } else {
                emptyList()
            }
        }
        val vin = hexData.joinToString("") {
            try {
                val char = Integer.parseInt(it, 16).toChar()
                char.toString()
            } catch (e: NumberFormatException) {
                ""
            }
        }.trim()
        Log.d(tag, "Parsed VIN: $vin")
        return vin
    }
}


