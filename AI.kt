package com.example.pruebav


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pruebav.ai.GeminiViewModel
import com.example.pruebav.ai.Message
import com.example.pruebav.database.ErroresCoche
import com.example.pruebav.database.NumeroVin
import com.example.pruebav.database.ParametrosCoche
import kotlinx.coroutines.launch

@Composable
fun AI(navController: NavController,database: AppDatabase) {

    val chatGemini: GeminiViewModel = viewModel()
    var userInput by rememberSaveable { mutableStateOf("") }
    val messages = chatGemini.messages

    val scope = rememberCoroutineScope()
    var mostrarDialogo by remember { mutableStateOf(false) }
    var mostrarSelectorDatos by remember { mutableStateOf(false) }

    var listaCoches by remember { mutableStateOf<List<CocheMarcaModelo>>(emptyList()) }
    var listaVin by remember { mutableStateOf<List<String>>(emptyList()) }
    var listaVehiculos by remember { mutableStateOf<List<Vehiculo>>(emptyList()) }
    var cocheSeleccionado by remember { mutableStateOf<Vehiculo?>(null) }
    var seleccionado by remember { mutableStateOf(false) }

    var parametros by remember { mutableStateOf<ParametrosCoche?>(null) }
    var errores by remember { mutableStateOf<List<ErroresCoche>>(emptyList()) }
    var infoVin by remember { mutableStateOf(NumeroVin(vin = "")) }

    LaunchedEffect(Unit) {
        scope.launch {
            listaCoches = database.numeroVinDao().getCoches()
            listaVin = database.numeroVinDao().getAllVins() ?: emptyList()

            listaVehiculos = listaCoches.flatMap { coche ->
                listaVin.map { vin ->
                    Vehiculo(marca = coche.marca, vin = vin)
                }
            }
        }
    }

    LaunchedEffect(cocheSeleccionado, seleccionado) {
        cocheSeleccionado?.let { coche ->
            parametros = database.parametrosDao().obtenerParametros(coche.vin.trim().uppercase())
            errores = database.erroresDao().obtenerErroresCoche(coche.vin.trim().uppercase()) ?: emptyList()
            infoVin = database.numeroVinDao().getNumeroVin(coche.vin.trim().uppercase())!!
        }
    }


    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            NavigationBar(modifier = Modifier.fillMaxWidth()) {
                NavItem(onClickAction = { navController.navigate("") }) {
                    IconContainer {
                        StateLayer(modifier = Modifier.background(Color(0xFF1E88E5), shape = RoundedCornerShape(50.dp))) {
                        IconM(
                                painter = painterResource(id = R.drawable.examples_detailed_view_mobile_icon3),
                                contentDescription = "Manager icon"
                            )
                        }
                    }
                    Text(
                        "Asistente",
                        modifier = Modifier.padding(top = 30.dp, start = 3.dp, end = 5.dp),
                        fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold
                    )                }

                NavItem(onClickAction = { navController.navigate("inicio") }) {
                    IconContainer {
                        StateLayer {
                            IconM(
                                painter = painterResource(id = R.drawable.examples_detailed_view_mobile_icon2),
                                contentDescription = "Inicio icon"
                            )
                        }
                    }
                    Text(
                        "Inicio",
                        modifier = Modifier.padding(top = 25.dp, start = 14.dp, end = 5.dp),
                        fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold
                    )
                }

                NavItem(onClickAction = {navController.navigate("datos")}) {
                    IconContainer {
                        StateLayer {
                            IconM(
                            painter = painterResource(id = R.drawable.examples_detailed_view_mobile_icon1),
                            contentDescription = "Asistente icon"
                            )
                        }
                    }
                    Text(
                        "Manager",
                        modifier = Modifier.padding(top = 25.dp, start = 5.dp, end = 5.dp),
                        fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold
                    )
                }
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
        ){
            Spacer(modifier = Modifier.height(35.dp))
            Row(modifier = Modifier.padding(start=16.dp,bottom = 8.dp)) {
                IconM(
                    onClick = { navController.navigate("inicio") },
                    modifier = Modifier
                        .width(30.dp)
                        .height(28.dp)
                        .rotate(180.0F),
                    painter = painterResource(id = R.drawable.examples_detailed_view_mobile_icon),
                    contentDescription = "icon"
                )

                Text(
                    text = "Asistente", modifier = Modifier.padding(start = 15.dp),
                    fontWeight = FontWeight.Bold, color = Color.White, fontSize = 24.sp
                )
            }

            ChatScreen(
                messages = messages,
                userInput = userInput,
                onUserInputChange = { userInput = it },
                onSendClick = {
                    if (userInput.isNotBlank()) {
                        val prompt = userInput.trim()
                        userInput = ""

                        chatGemini.enviarMensaje(
                            prompt,
                            onResultado = { },
                            onError = { error ->
                                chatGemini.agregarMensaje(Message("model", "Error: $error"))
                            }
                        )
                    }
                },
                onButton1Click = { mostrarDialogo = true },
                onButton2Click = { mostrarSelectorDatos = true },
            )

            if (mostrarDialogo) {
                AlertDialog(
                    onDismissRequest = { mostrarDialogo = false },
                    title = { Text("Selecciona un vehículo") },
                    text = {
                        Column {
                            listaVehiculos.forEach { vehiculo ->
                                Button(
                                    onClick = {
                                        cocheSeleccionado = vehiculo
                                        mostrarDialogo = false
                                        seleccionado = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E88E5),
                                        contentColor = Color.White
                                    )
                                ){
                                    Text("Marca: ${vehiculo.marca}, VIN: ${vehiculo.vin}")
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                mostrarDialogo = false 
                                cocheSeleccionado=null
                            },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = Color(0xFF1E88E5),
                                contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }
            if (mostrarSelectorDatos) {
                AlertDialog(
                    onDismissRequest = { mostrarSelectorDatos = false },
                    title = { Text("¿Qué deseas cargar?") },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val textoParametros = formatearParametrosParaIA(parametros,infoVin)
                                    userInput = textoParametros
                                    mostrarSelectorDatos = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E88E5),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "Parámetros",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Button(
                                onClick = {
                                    val textoErrores = formatearCodigosErrorParaIA(errores,infoVin)
                                    userInput = textoErrores
                                    mostrarSelectorDatos = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E88E5),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "Códigos de error",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                    },
                    confirmButton = {
                        TextButton(
                            onClick = { mostrarSelectorDatos = false },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = Color(0xFF1E88E5),
                                contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar")
                        }
                    }

                )
            }

        }
    }
}

@Composable
fun ChatScreen(
    messages: List<Message>,
    userInput: String,
    onUserInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onButton1Click: () -> Unit,
    onButton2Click: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top=8.dp,start=16.dp,end=16.dp,bottom=16.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Transparent)
                .border(2.dp,Color.White.copy(alpha = 0.6f),shape= RoundedCornerShape(16.dp))
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "¡Pregúntame algo!",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 22.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 8.dp),
                    reverseLayout = true
                ) {
                    items(messages.reversed()) { message ->
                        val isUser = message.role == "user"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isUser) Color(0xFF3A3A3A) else Color(0xFF2A2A2A),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(12.dp)
                                    .widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = message.content,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(top = 8.dp)
        ) {
            OutlinedTextField(
                value = userInput,
                onValueChange = onUserInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                placeholder = { Text("Escribe tu mensaje...", color = Color.White.copy(alpha = 0.6f)) },
                maxLines = 4,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                    cursorColor = Color.White.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    IconButton(
                        onClick = onSendClick,
                        enabled = userInput.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Enviar",
                            tint = if (userInput.isNotBlank()) Color.White else Color.Gray
                        )
                    }

                }
            )

            Spacer(modifier = Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val buttonModifier = Modifier
                    .height(45.dp)
                    .padding(horizontal = 4.dp)

                val azulOpenAI = Color(0xFF1E88E5)

                Button(
                    onClick = onButton1Click,
                    colors = ButtonDefaults.buttonColors(containerColor = azulOpenAI),
                    modifier = buttonModifier
                ) {
                    Text(text = "Seleccionar coche", color = Color.White, fontSize = 14.sp)
                }

                Button(
                    onClick = onButton2Click,
                    colors = ButtonDefaults.buttonColors(containerColor = azulOpenAI),
                    modifier = buttonModifier
                ) {
                    Text(text = "Cargar datos", color = Color.White, fontSize = 14.sp)
                }

            }
        }
    }
}


data class Vehiculo(
    val marca:String,
    val vin:String
)

fun formatearParametrosParaIA(parametrosCoche: ParametrosCoche?, info: NumeroVin): String {
    if (parametrosCoche == null || parametrosCoche.parametros.isEmpty())
        return "No hay parámetros guardados para este vehículo."

    val sb = StringBuilder()
    sb.append("Ofrece recomendaciones para el vehículo detectado a partir del VIN.\n")
    sb.append("Puedes verificar la marca y modelo descifrando el VIN si encuentras incoherencias.\n\n")
    sb.append("VIN: ${info.vin}\n")
    sb.append("Marca: ${info.marca}\n")
    sb.append("Modelo: ${info.modelo}\n")
    info.anioFabricacion?.let { sb.append("Año de fabricación: $it\n") }
    info.caract?.let { sb.append("Características: $it\n") }
    sb.append("Parámetros:\n")
    parametrosCoche.parametros.forEach { parametro ->
        sb.append("- ${parametro.nombre}: ${parametro.valor}\n")
    }
    return sb.toString()
}

fun formatearCodigosErrorParaIA(errores: List<ErroresCoche>, info: NumeroVin): String {
    if (errores.isEmpty()) return "No hay errores guardados para este vehículo."

    val esSinErrores = errores.size == 1 && errores.first().codigoError == "NO_ERROR"
    if (esSinErrores) return "No se detectaron códigos de error para este vehículo."

    val sb = StringBuilder()
    sb.append("Ofrece recomendaciones para el vehículo detectado a partir del VIN.\n")
    sb.append("Puedes verificar la marca y modelo descifrando el VIN si encuentras incoherencias.\n\n")
    sb.append("VIN: ${info.vin}\n")
    sb.append("Marca: ${info.marca}\n")
    sb.append("Modelo: ${info.modelo}\n")
    info.anioFabricacion?.let { sb.append("Año de fabricación: $it\n") }
    info.caract?.let { sb.append("Características: $it\n") }
    sb.append("\n📋 Códigos de error detectados:\n")

    errores.forEach { error ->
        sb.append("- Código: ${error.codigoError}")
        if (!error.descripcion.isNullOrEmpty()) {
            sb.append(" – Descripción: ${error.descripcion}")
        }
        sb.append("\n")
    }

    return sb.toString()
}

