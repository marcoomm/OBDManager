package com.example.pruebav

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pruebav.database.CodigosError
import com.example.pruebav.database.Parametro
import com.example.pruebav.database.cargarCodigosDesdeJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.cancellation.CancellationException

class OBDViewModel(application: Application) : AndroidViewModel(application) {

    //bluetooth
    private var _bluetoothReady = MutableStateFlow(false)
    var bluetoothReady :StateFlow<Boolean> = _bluetoothReady

    fun setBluetoothReady(value: Boolean) {
        _bluetoothReady.value = value
    }

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

    private var lastCheckedVin: String? = null
    fun shouldProcessVin(currentVin: String): Boolean {
        return if (currentVin.isNotBlank() && currentVin != lastCheckedVin) {
            lastCheckedVin = currentVin
            true
        } else {
            false
        }
    }

    private var lecturaIniciada = false

    @RequiresApi(Build.VERSION_CODES.O)
    fun lecturaInicial() {
        if (lecturaIniciada) return
        lecturaIniciada = true

        viewModelScope.launch {
            runCatching {
                val datosIniciales = withContext(Dispatchers.IO) {
                    OBDManager.iniciar(obdConnection)
                    OBDManager.leerDatosIniciales()
                }

                val vinLimpio = OBDManager.limpiarVin(datosIniciales["vin"] ?: "")
                _vin.value = vinLimpio
                _km.value = datosIniciales["km"] ?: ""
                _nerrores.value = datosIniciales["numerCodes"] ?: ""

                delay(1000)

                val errores = withContext(Dispatchers.IO) {
                    OBDManager.leerCodigos()
                }
                _codigosError.value = errores

                val fechaActual = LocalDateTime.now()
                val formato = DateTimeFormatter.ofPattern("HH:mm")
                _fecha.value = fechaActual.format(formato)
                _isConnected.value = true

            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    val toastMessage = mutableStateOf<String?>(null)
    private val deleteResult = mutableStateOf<Boolean?>(null)

    fun leerCodigos(){
        viewModelScope.launch {
            runCatching {
                val errores = withContext(Dispatchers.IO) {
                    OBDManager.leerCodigos()
                }
                _codigosError.value = errores
            }

        }
    }

    fun borrarCodigos() {
        viewModelScope.launch {
            val resultado = OBDManager.borrarCodigos()
            deleteResult.value = resultado

            if (resultado) {
                toastMessage.value = "Códigos de error borrados correctamente"
                leerCodigos()
            } else {
                toastMessage.value = "No se pudieron borrar los códigos de error"
            }
        }
    }


    private var leyendoParametros = false
    private var lecturaJob: Job? = null

    fun lecturaParametros() {
        if (leyendoParametros) return
        leyendoParametros = true

        lecturaJob = viewModelScope.launch {
            while (_isConnected.value) {
                val start = System.currentTimeMillis()

                try {

                    val datos = withContext(Dispatchers.IO) {
                        OBDManager.leerParametros()
                    }
                    _parametros.value = datos.map { (nombre, valor) ->
                        Log.d("VIEWMODEL", "Parametro $nombre = $valor")
                        Parametro(nombre, valor, categoria = "")
                    }

                } catch (e: Exception) {
                    if (e is CancellationException) {
                        Log.d("OBD", "Lectura cancelada correctamente")
                        throw e
                    } else {
                        Log.e("OBD", "Error al leer parámetros: ${e.message}")
                        e.printStackTrace()
                    }
                }
                val tiempo = System.currentTimeMillis() - start
                Log.d("OBD", "Lectura completa en $tiempo ms")
                if (tiempo < 3000) delay(3000 - tiempo)
            }
            leyendoParametros = false
        }
    }

    fun detenerLectura() {
        Log.d("OBD","Lectura detenida")
        lecturaJob?.cancel()
        lecturaJob = null
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

    suspend fun borrarCodigos(): Boolean {
        return obdConnection?.deleteCodes() ?: false
    }

}



