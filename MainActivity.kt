package com.example.pruebav

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.pruebav.database.NumeroVin
import com.example.pruebav.ui.theme.PruebaVTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val requestBluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Log.e("OBD", "Permiso de Bluetooth denegado.")
            }
        }

    private val viewModel: OBDViewModel by viewModels()
    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.i("OBD", "Bluetooth activado correctamente.")
                viewModel.setBluetoothReady(true)
            } else {
                Log.e("OBD", "El usuario rechazó activar Bluetooth.")
            }
        }
    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBluetoothPermissions()

        setContent {
            PruebaVTheme {
                NavegacionEntreVentanas(this, enableBluetoothLauncher)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
@Composable
fun OBDConnectionScreen(context: Context, enableBluetoothLauncher: androidx.activity.result.ActivityResultLauncher<Intent>, navController : NavController,viewModel: OBDViewModel,database: AppDatabase) {

    val scope = rememberCoroutineScope()
    var showDeviceList by remember { mutableStateOf(false) }
    val bluetoothReady by viewModel.bluetoothReady.collectAsState()
    var pairedDevices by remember { mutableStateOf(emptyList<BluetoothDevice>()) }
    val mostrar = remember { mutableStateOf(false) }
    val currentTime by viewModel.fecha.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val ecuReady by viewModel.ecuReady.collectAsState()

    //variables obd
    val vin by viewModel.vin.collectAsState()
    val km by viewModel.km.collectAsState()
    val ncodigos by viewModel.nerrores.collectAsState()

    //Variables NºVin
    val marca by viewModel.marca.collectAsState()
    val modelo by viewModel.modelo.collectAsState()
    val anio by viewModel.anio.collectAsState()
    val caract by viewModel.caract.collectAsState()

    val vinprueba = remember { mutableStateOf("-") }

    LaunchedEffect(isConnected, ecuReady) {
        if (isConnected && ecuReady) {
            viewModel.lecturaInicial()
        } else if (!isConnected) {
            OBDManager.desconectar()
            viewModel.detenerLectura()
        }
    }

    LaunchedEffect(bluetoothReady) {
        if (bluetoothReady && !isConnected) {
            withContext(Dispatchers.IO) {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter != null && adapter.isEnabled) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        val devices = adapter.bondedDevices.toList()
                        withContext(Dispatchers.Main) {
                            if (devices.isNotEmpty()) {
                                pairedDevices = devices
                                showDeviceList = true
                            } else {
                                viewModel.setConnectionStatus("No hay dispositivos emparejados")
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            viewModel.setConnectionStatus("Permiso BLUETOOTH_CONNECT no concedido")
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(vin) {
        if (isConnected && ecuReady && viewModel.shouldProcessVin(vin)) {
            val numeroVin = NumeroVin.decodeVinHttpClient(vin)
            numeroVin?.let { vinData ->
                withContext(Dispatchers.IO) {
                    val dao = database.numeroVinDao()
                    val existingVin = dao.getNumeroVin(vin)

                    if (existingVin == null) {
                        dao.insertNumeroVin(vinData)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Nuevo coche registrado", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.i("VIN", "El VIN ya estaba registrado")
                    }
                }
            } ?: Log.e("VIN", "No se pudo decodificar el VIN")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.detenerLectura()
        }
    }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                NavItem(onClickAction = { navController.navigate("asistente") }) {
                    IconContainer {
                        StateLayer {
                            IconM(
                                painter = painterResource(id = R.drawable.examples_detailed_view_mobile_icon3),
                                contentDescription = "Manager icon"
                            )
                        }
                    }
                    LabelText(texto = "Asistente")
                }
                NavItem(onClickAction = {  }) {
                    IconContainer {
                        StateLayer(
                            modifier = Modifier.background(
                                Color(0xFF1E88E5),
                                shape = RoundedCornerShape(50.dp)
                            )
                        ) {
                            IconM(
                                painter = painterResource(id = R.drawable.examples_detailed_view_mobile_icon2),
                                contentDescription = "Inicio icon"
                            )
                        }
                    }
                    Text(
                        "Inicio",
                        modifier = Modifier.padding(top = 30.dp, start = 14.dp, end = 5.dp),
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                NavItem(onClickAction = { navController.navigate("datos") }) {
                    IconContainer {
                        StateLayer {
                            IconM(
                                painter = painterResource(id = R.drawable.examples_detailed_view_mobile_icon1),
                                contentDescription = "Manager icon"
                            )
                        }
                    }
                    Text(
                        "Manager",
                        modifier = Modifier.padding(top = 25.dp, start = 5.dp, end = 5.dp),
                        fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold
                    )                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1E1E), Color(0xFF121212))
                    )
                )
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(35.dp))
            Text(
                text = "Conexión : $connectionStatus",
                modifier = Modifier.padding(start = 30.dp),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(25.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(modifier = Modifier, colors = ButtonColors(Color(0xFF1E88E5), Color.White,Color(0xFF1E88E5),Color.White),
                    onClick = {
                    scope.launch {
                        val adapter = BluetoothAdapter.getDefaultAdapter()
                        if (adapter == null) {
                            viewModel.setConnectionStatus("Bluetooth no disponible")
                            return@launch
                        }
                        if (!adapter.isEnabled) {
                            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                            return@launch
                        }
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.setConnectionStatus("Permiso no concedido")
                            return@launch
                        }
                        viewModel.setBluetoothReady(true)
                        showDeviceList=true
                    }
                }) {
                    Text("Conectar OBD-II")
                }
                if (showDeviceList) {
                    AlertDialog(
                        onDismissRequest = { showDeviceList = false },
                        title = { Text("Selecciona un dispositivo") },
                        text = {
                            Column {
                                pairedDevices.forEach { device ->
                                    Button(onClick = {
                                        scope.launch {
                                            try {
                                                viewModel.conectarDispositivo(device)
                                                viewModel.onConnected(device.name ?: "desconocido")
                                            } catch (e: Exception) {
                                                viewModel.setConnectionStatus("Error al conectar")
                                            }
                                        }
                                        showDeviceList = false
                                    }) {
                                        Text(device.name ?: "Desconocido")
                                    }
                                }
                            }
                        },
                        confirmButton = {}
                    )
                }

                Button(modifier = Modifier, colors = ButtonColors(Color(0xFF1E88E5), Color.White,Color(0xFF1E88E5),Color.White),
                    onClick = {
                        scope.launch(Dispatchers.IO){
                            viewModel.onDisconnected()
                            OBDManager.desconectar()

                            vinprueba.value=""
                            viewModel.setVin("NO DATA")
                            viewModel.setKM("NO DATA")
                            viewModel.setNErrores("NO DATA")

                        }
                    }) {
                    Text("Desconectar OBD-II")
                }
            }


            Spacer(modifier=Modifier.padding(top=25.dp))

            VehicleInfoScreen(
                marca = marca,
                modelo = modelo,
                anio = anio,
                carac = caract,
                mostrar = mostrar,
                vin = vin,
                currentTime = currentTime,
                errores = ncodigos,
                km = km
            )

            Botones(context,navController,isConnected)

            Spacer(modifier = Modifier.weight(1.0f))
       }


   }
}

//elementos composable

@Composable
fun VinCard(marca: String,modelo: String,anio:String,carac:String,mostrar: MutableState<Boolean>, onDismiss: () -> Unit) {

    if (mostrar.value) {
        AlertDialog(
            onDismissRequest = {
                mostrar.value = false
                onDismiss()
            },
            title = {
                Text("$marca $modelo")
            },
            text = {

                Box{
                    Column{
                        Text("Características: $carac")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Año de fabricación: $anio")
                    }
                }

            },
            confirmButton = {
                Button(onClick = {
                    mostrar.value = false
                    onDismiss()
                }) {
                    Text("Cerrar")
                }
            }
        )
    }
}


@Composable
fun VehicleInfoScreen(
    marca: String,
    modelo: String,
    anio: String,
    carac: String,
    mostrar: MutableState<Boolean>,
    vin: String,
    currentTime: String,
    errores: String,
    km: String
) {
    val vehicleData = listOf(
        VehicleInfo("Estado", "Nºerrores detectados", currentTime),
        VehicleInfo("VIN", "Número de bastidor", currentTime),
        VehicleInfo("Km", "Distancia desde borrado", currentTime)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row {
            Text(
                text = "Acerca del vehículo",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )

            IconM(
                onClick = {
                    if (vin.length == 17) {
                        mostrar.value = true
                    }
                },
                modifier = Modifier
                    .size(35.dp)
                    .padding(bottom = 10.dp),
                contentDescription = "seeInfo",
                painter = painterResource(id = R.drawable.examples_detailed_view_mobile_icon)
            )
        }

        VinCard(marca, modelo, anio, carac, mostrar, onDismiss = { mostrar.value = false })

        LazyColumn {
            items(vehicleData) { item ->
                val dato = when (item.title) {
                    "Estado" -> errores
                    "VIN" -> vin
                    "Km" -> km
                    else -> ""
                }

                VehicleInfoCard(item, dato)
            }
        }
    }
}



@Composable
fun VehicleInfoCard(info: VehicleInfo, valor: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(70.dp)
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = valor,
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.width(30.dp))

            Column {
                Text(
                    text = info.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    text = info.description,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Text(
                    text = "Comprobado a las: ${info.checkedTime}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}



data class VehicleInfo(
    val title: String,
    val description: String,
    val checkedTime: String
)



@Composable
fun Botones(context: Context,navController: NavController,isConnected:Boolean){

   Column(
       modifier = Modifier
           .fillMaxWidth()
           .padding(16.dp)
   ) {
       Row(
           modifier = Modifier.fillMaxWidth(),
           horizontalArrangement = Arrangement.SpaceEvenly
       ) {
           Card(
               modifier = Modifier
                   .weight(1f)
                   .padding(8.dp)
                   .clickable {

                       if (isConnected) {
                           navController.navigate("errores")
                       } else {
                           Toast
                               .makeText(context, "Conecta el OBD primero", Toast.LENGTH_SHORT)
                               .show()
                       }
                        /*navController.navigate("errores")*/
                   },
               shape = RoundedCornerShape(12.dp),
               colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
           ) {
               Box(
                   modifier = Modifier
                       .padding(12.dp)
                       .height(70.dp),
                   contentAlignment = Alignment.Center
               ) {
                   Column(horizontalAlignment = Alignment.CenterHorizontally) {
                       Text("DIAGNÓSTICO", color = Color.White, fontWeight = FontWeight.Bold)
                       Text("Verifica el estado   del   vehículo", color = Color.LightGray, fontSize = 12.sp)
                   }

                   IconM(
                       painter = painterResource(id = R.drawable.examples_detailed_view_mobile_icon),
                       contentDescription = "Icono de diagnóstico",
                       modifier = Modifier
                           .align(Alignment.BottomEnd)
                           .padding(end = 12.dp, bottom = 8.dp, top = 15.dp)
                           .size(20.dp)
                   )
               }
           }


           Card(
               modifier = Modifier
                   .weight(1f)
                   .padding(8.dp)
                   .clickable {


                       if (isConnected) {
                           navController.navigate("parametros")

                       } else {
                           Toast.makeText(context, "Conecta el OBD primero", Toast.LENGTH_SHORT).show()
                       }

                       /*navController.navigate("parametros")*/

                   },
               shape = RoundedCornerShape(12.dp),
               colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
           ) {
               Box(
                   modifier = Modifier
                       .padding(12.dp)
                       .height(70.dp),
                   contentAlignment = Alignment.Center
               ) {
                   Column(horizontalAlignment = Alignment.CenterHorizontally) {
                       Text("DATOS EN VIVO", color = Color.White, fontWeight = FontWeight.Bold)
                       Text("Monitorea datos en tiempo real", color = Color.LightGray, fontSize = 12.sp)
                   }

                   IconM(
                       painter = painterResource(id = R.drawable.examples_detailed_view_mobile_icon),
                       contentDescription = "Icono de parámetros",
                       modifier = Modifier
                           .align(Alignment.BottomEnd)
                           .padding(end = 12.dp, bottom = 8.dp, top = 15.dp)
                           .size(20.dp),
                   )
               }

           }
       }
   }
}

@Composable
fun NavigationBar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
   Row(
       modifier = modifier
           .background(Color(29, 27, 32))
           .padding(start = 8.dp, end = 8.dp)
           .height(75.dp)
           .fillMaxWidth(),
       horizontalArrangement = Arrangement.Center,
       verticalAlignment = Alignment.CenterVertically
   ) {
       content()
   }
}

@Composable
fun NavItem(modifier: Modifier = Modifier, onClickAction: () -> Unit, content: @Composable () -> Unit) {
   Box(
       modifier = modifier
           .padding(top = 12.dp, bottom = 16.dp, start = 30.dp, end = 30.dp)
           .width(65.dp)
           .height(72.dp)
           .clickable { onClickAction() }
   ) {
       content()
   }
}

@Composable
fun IconContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
   Box(
       modifier = modifier
           .width(60.dp)
           .height(60.dp)
   ) {
       content()
   }
}

@Composable
fun StateLayer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
   Box(
       modifier = modifier
           .width(62.dp)
           .height(32.dp)
           .padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
   ) {
       content()
   }
}

@Composable
fun IconM(
   modifier: Modifier = Modifier,
   painter: Painter,
   contentDescription: String,
   onClick: (() -> Unit)? = null
) {
   Icon(
       painter = painter,
       contentDescription = contentDescription,
       modifier = modifier
           .size(50.dp)
           .padding(4.dp)
           .then(
               onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier
           ),
       tint = Color.White
   )
}


@Composable
fun LabelText(modifier: Modifier = Modifier, texto: String) {
   Text(
       text = texto,
       fontSize = 12.sp,
       color = Color.White,
       fontWeight = FontWeight.Bold,
       modifier = modifier
           .fillMaxWidth()
           .padding(top = 27.dp, start = 3.dp, end = 5.dp),
   )
}

