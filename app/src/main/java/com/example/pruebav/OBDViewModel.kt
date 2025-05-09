package com.example.pruebav

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
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
    private var _vin = MutableStateFlow("")
    var vin: StateFlow<String> = _vin
    private val _km = MutableStateFlow("")
    var km: StateFlow<String> = _km
    private val _nerrores = MutableStateFlow("")
    var nerrores: StateFlow<String> = _nerrores
    private val _fecha = MutableStateFlow("")
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
        // Una vez conectado, registramos fecha y hora actual
        val fechaActual = LocalDateTime.now()
        val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val fechaFormateada = fechaActual.format(formato)
        _fecha.value = fechaFormateada  // o llama a tu setFecha()
        _isConnected.value = true
    }


    fun lecturaInicial() {
        viewModelScope.launch {
            runCatching {
                val datosIniciales = withContext(Dispatchers.IO) {
                    OBDManager.iniciar(obdConnection)
                    OBDManager.leerDatosIniciales()
                }

                // Estos ya pueden estar en Main, si no son costosos
                val vinLimpio = OBDManager.limpiarVin(datosIniciales["vin"] ?: "")
                _vin.value = vinLimpio
                _km.value = datosIniciales["km"] ?: ""
                _nerrores.value = datosIniciales["numerCodes"] ?: ""

                delay(1000)

                val errores = withContext(Dispatchers.IO) {
                    OBDManager.leerCodigos()
                }
                _codigosError.value = errores

                // Luego lanzar la lectura periódica
                lecturaParametros()
            }.onFailure {
                it.printStackTrace()
            }
        }
    }


    private fun lecturaParametros() {
        viewModelScope.launch(Dispatchers.IO) {
            //while (isConnected.value) {
                try {
                    val datos = OBDManager.leerParametros()

                    // Asigna el resultado al StateFlow
                    _parametros.value = datos.map { (nombre, valor) -> Parametro(nombre, valor, categoria = "") }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
                //delay(1000)
            //}
        }
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



