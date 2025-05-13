package com.example.pruebav

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pruebav.database.CodigosError
import com.example.pruebav.database.Parametro
import com.example.pruebav.database.cargarCodigosDesdeJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class OBDViewModel(application: Application) : AndroidViewModel(application) {

    //main activity

    //variables de estado
    private var _connectionStatus = MutableStateFlow("Desconectado")
    var connectionStatus: StateFlow<String> = _connectionStatus
    private var _isConnected = MutableStateFlow(false)
    var isConnected: StateFlow<Boolean> = _isConnected
    private val _ecuReady = MutableStateFlow(false)
    val ecuReady: StateFlow<Boolean> = _ecuReady

    fun setConnectionStatus(status: String) {
        _connectionStatus.value = status
    }
    fun onConnected(deviceName: String) {
        _connectionStatus.value = "Conectado a $deviceName"
        _isConnected.value = true
    }
    fun onDisconnected() {
        _connectionStatus.value = "Desconectado"
        _isConnected.value = false
    }
    fun setECUReady(ready: Boolean) {
        _ecuReady.value = ready
    }


    //variables obd
    private var _vin = MutableStateFlow("NO DATA")
    var vin: StateFlow<String> = _vin
    private val _km = MutableStateFlow("NO DATA")
    var km: StateFlow<String> = _km
    private val _nerrores = MutableStateFlow("NO DATA")
    var nerrores: StateFlow<String> = _nerrores
    private val _fecha = MutableStateFlow("-")
    var fecha: StateFlow<String> = _fecha

    fun setVin(nvin:String) {
        _vin.value=nvin
    }
    fun setKM(nkm:String){
        _km.value=nkm
    }
    fun setNErrores(new:String){
        _nerrores.value=new
    }
    fun setFecha(new:String){
        _fecha.value=new
    }

    //variables vin
    private val _marca = MutableStateFlow("")
    val marca: StateFlow<String> = _marca

    private val _modelo = MutableStateFlow("")
    val modelo: StateFlow<String> = _modelo

    private val _anio = MutableStateFlow("")
    val anio: StateFlow<String> = _anio

    private val _caract = MutableStateFlow("")
    val caract: StateFlow<String> = _caract

    fun setMarca(value: String) {
        _marca.value = value
    }
    fun setModelo(value: String) {
        _modelo.value = value
    }
    fun setAnio(value: String) {
        _anio.value = value
    }
    fun setCaract(value: String) {
        _caract.value = value
    }

    //composables

    private val _parametros = MutableStateFlow<List<Parametro>>(emptyList())
    val parametros: StateFlow<List<Parametro>> = _parametros

    private var _codigosError = MutableStateFlow<Map<String, String>>(emptyMap())
    val codigosError: StateFlow<Map<String, String>> = _codigosError

    private var _listacodigos = MutableStateFlow<List<CodigosError>>(emptyList())
    val listacodigos: StateFlow<List<CodigosError>> = _listacodigos


    @SuppressLint("StaticFieldLeak")
    private val context: Context = application.applicationContext
    private val obdConnection = OBDConnection(application.applicationContext)

    init {
        val context = getApplication<Application>().applicationContext
        _listacodigos.value = cargarCodigosDesdeJson(context)
    }

    suspend fun conectarDispositivo(device: BluetoothDevice) {
        withContext(Dispatchers.IO) {
            OBDManager.viewModel = this@OBDViewModel
            obdConnection.connectToOBD(device)
        }
    }


    private var lecturaIniciada = false

    fun lecturaInicial() {
        if (lecturaIniciada) return // Solo se ejecuta una vez
        lecturaIniciada = true

        viewModelScope.launch {
            runCatching {
                // Iniciar conexión OBD y leer datos iniciales
                val datosIniciales = withContext(Dispatchers.IO) {
                    OBDManager.iniciar(obdConnection)
                    OBDManager.leerDatosIniciales()
                }

                // Asigna los valores obtenidos al StateFlow
                val vinLimpio = OBDManager.limpiarVin(datosIniciales["vin"] ?: "")
                _vin.value = vinLimpio
                _km.value = datosIniciales["km"] ?: ""
                _nerrores.value = datosIniciales["numerCodes"] ?: ""

                delay(1000) // Espera para simular proceso de lectura

                // Leer los códigos de error
                val errores = withContext(Dispatchers.IO) {
                    OBDManager.leerCodigos()
                }
                _codigosError.value = errores

                // Establecer la fecha y hora actual
                val fechaActual = LocalDateTime.now()
                val formato = DateTimeFormatter.ofPattern("HH:mm")
                _fecha.value = fechaActual.format(formato)
                _isConnected.value = true

            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    private var leyendoParametros = false

    fun lecturaParametros() {
        if (leyendoParametros) return // Solo se ejecuta si no estamos ya leyendo parámetros

        leyendoParametros = true
        viewModelScope.launch {
            while (_isConnected.value) { // Mientras esté conectado
                try {
                    // Leer los parámetros cada 200ms
                    val datos = OBDManager.leerParametros()

                    // Asigna el resultado al StateFlow
                    _parametros.value = datos.map { (nombre, valor) -> Parametro(nombre, valor, categoria = "") }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(200) // Espera de 200ms para la próxima lectura
            }
            leyendoParametros = false // Detiene la lectura cuando ya no está conectado
        }
    }


    fun detenerLectura() {
        leyendoParametros = false
    }

}

object OBDManager {
    var viewModel: OBDViewModel? = null

    @SuppressLint("StaticFieldLeak")
    private var obdConnection: OBDConnection? = null

    fun iniciar(obdConn: OBDConnection) {
        obdConnection = obdConn
    }

    fun desconectar(){
        obdConnection?.disconnectOBD()
    }

    suspend fun leerDatosIniciales(): Map<String, String> {
        return obdConnection?.readFirst() ?: emptyMap()
    }

    suspend fun leerCodigos(): Map<String, String> {
        return obdConnection?.readCodes() ?: emptyMap()
    }

    suspend fun leerParametros(): Map<String, String> {
        return obdConnection?.readAllParameters() ?: emptyMap()
    }

    fun limpiarVin(vin: String): String {
        return vin.replace(Regex("[^\\x21-\\x7E]"), "")
    }



}



