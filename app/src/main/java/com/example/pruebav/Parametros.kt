package com.example.pruebav

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pruebav.database.Parametro
import com.example.pruebav.database.guardarParametros
import kotlinx.coroutines.launch

@Composable
fun Parametros(context: Context, navController: NavController, viewModel: OBDViewModel) {
    val vin by viewModel.vin.collectAsState()
    val datosMap by viewModel.parametros.collectAsState()
    val listaParametros by rememberUpdatedState(datosMap.map { (nombre, valor) -> Parametro(nombre, valor, categoria = "") })

    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("Todo") }
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.lecturaParametros()
                Lifecycle.Event.ON_PAUSE -> viewModel.detenerLectura()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.detenerLectura()
        }
    }

    /*
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.lecturaParametros()
                }
                Lifecycle.Event.ON_PAUSE -> {
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
    }

     */

    Scaffold(
        containerColor = Color.Black, // Fondo de la pantalla
        bottomBar = {
            NavigationBar(modifier = Modifier.fillMaxWidth()) {
                NavItem(onClickAction = {  }) {
                    IconContainer {
                        StateLayer {
                            Icon(
                                painter = painterResource(id = R.drawable.examples_detailed_view_mobile_icon3),
                                contentDescription = "Manager icon"
                            )
                        }
                    }
                    LabelText(texto = "Asistente")
                }

                NavItem(onClickAction = { navController.navigate("inicio") }) {
                    IconContainer {
                        StateLayer {
                            Icon(
                                painter = painterResource(id = R.drawable.examples_detailed_view_mobile_icon2),
                                contentDescription = "Inicio icon"
                            )
                        }
                    }
                    Text(
                        "Inicio",
                        modifier = Modifier.padding(top = 30.dp, start = 14.dp, end = 5.dp),
                        fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold
                    )
                }

                NavItem(onClickAction = { navController.navigate("datos") }) {
                    IconContainer {
                        StateLayer {
                            Icon(
                                painter = painterResource(id = R.drawable.examples_detailed_view_mobile_icon1),
                                contentDescription = "Asistente icon"
                            )
                        }
                    }
                    LabelText(texto = "Manager")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            Spacer(modifier = Modifier.height(35.dp))

            // TOP APP BAR
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(
                    onClick = { navController.navigate("inicio") },
                    modifier = Modifier.width(30.dp).height(28.dp).rotate(180.0F),
                    painter = painterResource(id = R.drawable.examples_detailed_view_mobile_icon),
                    contentDescription = "icon"
                )

                Text(
                    text = "Parámetros", modifier = Modifier.padding(start = 15.dp),
                    fontWeight = FontWeight.Bold, color = Color.White, fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // FILTER BUTTON
            /*
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp).padding(start=35.dp,end=22.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Mostrando : $selectedOption",
                    modifier = Modifier.padding(top=6.dp),
                    fontSize = 14.sp, color = Color.White
                )
                Button(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.width(110.dp).height(35.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonColors(contentColor = Color.White, containerColor = Color(0xFF1E88E5), disabledContentColor = Color.White, disabledContainerColor =Color(0xFF1E88E5)),
                ) {

                    IconF(
                        painter = painterResource(id = R.drawable.filterarrows),
                        contentDescription = "icon"
                    )
                    Text(text = "Filtrar")

                    Row(horizontalArrangement = Arrangement.Absolute.Right, modifier = Modifier.fillMaxWidth().padding(top=30.dp,start=25.dp)){
                        DropdownMenu(modifier = Modifier.background(Color(0xFF1E88E5)),
                            expanded = expanded,
                            onDismissRequest = { expanded = false
                            }, scrollState = rememberScrollState()
                        )
                        {
                            DropdownMenuItem(onClick = {
                                selectedOption = "Todo"
                                expanded = false
                            },  text = { Text("Todo", color = Color.White, fontSize = 16.sp) })
                            HorizontalDivider()
                            DropdownMenuItem(onClick = {
                                selectedOption = "Presión"
                                expanded = false
                            },  text = { Text("Presión", color = Color.White, fontSize = 16.sp) })
                            HorizontalDivider()
                            DropdownMenuItem(onClick = {
                                selectedOption = "Combustible"
                                expanded = false
                            },  text = { Text("Combustible", color = Color.White, fontSize = 16.sp) })
                            HorizontalDivider()
                            DropdownMenuItem(onClick = {
                                selectedOption = "Motor"
                                expanded = false
                            },  text = { Text("Motor", color = Color.White, fontSize = 16.sp) })
                            HorizontalDivider()
                            DropdownMenuItem(onClick = { selectedOption = "Temperatura"
                                expanded = false
                            },  text = { Text("Temperatura", color = Color.White, fontSize = 16.sp) })

                        }
                    }

                }


            }
            */

            Spacer(modifier = Modifier.height(20.dp))

            /*
            val filtrados = when (selectedOption) {
                "Presión" -> parametros.filter { it.categoria == "Presión" }
                "Motor" -> parametros.filter { it.categoria == "Motor" }
                "Temperatura" -> parametros.filter { it.categoria == "Temperatura" }
                "Combustible" -> parametros.filter { it.categoria == "Combustible" }
                else -> parametros
            }
            */

            Box(modifier = Modifier.weight(1f)) {
               /* if(isLoading){
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }else{
                    */
                    ParametersScreen(datosMap)
                    /*
                    LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        items(listaParametros) { param ->
                            ParameterCard(info = param)
                        }
                    }

                     */
                //}
            }

            // BOTÓN FIJO ARRIBA DE LA NAVIGATION BAR
            Button(
                onClick = { coroutineScope.launch{
                    guardarParametros(context, vin, listaParametros)
                } },
                colors = ButtonColors(contentColor = Color.White, containerColor = Color(0xFF1E88E5), disabledContentColor = Color.White, disabledContainerColor =Color(0xFF1E88E5)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Guardar Datos", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/*
@Composable
fun AllParam() {
    val paramData = listOf(
        Parameter("RPM", "3.000"),
        Parameter("Acelerador", "0%"),
        Parameter("Tiempo activo", "425min"),
        Parameter("Carga del motor", "30%"),
        Parameter("Temperatura Motor", "95°C"),
        Parameter("Temperatura del refrigerante", "80°C"),
        Parameter("Nivel de Combustible", "50%"),
        Parameter("Consumo de Combustible", "6 L/100 km"),
        Parameter("Presión", "Presión de los neumáticos: 32 psi"),
        Parameter("Presión de Combustible", "4 bar"),
        Parameter("Presión exterior", "1atm"),
        Parameter("Presión de Admisión", "4 bar")
    )
    LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp).height(525.dp)) {
        items(paramData) { item ->
            ParameterCard(info = item)
        }
    }
}

@Composable
fun Pressure() {
    val paramData = listOf(
        Parameter("Presión", "Presión de los neumáticos: 32 psi"),
        Parameter("Presión de Combustible", "4 bar"),
        Parameter("Presión exterior", "1atm"),
        Parameter("Presión de Admisión", "4 bar")
    )
    LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp).height(550.dp)) {
        items(paramData) { item ->
            ParameterCard(info = item)
        }
    }
}

@Composable
fun Combustible() {
    val paramData = listOf(
        Parameter("Nivel de Combustible", "50%"),
        Parameter("Consumo de Combustible", "6 L/100 km"),
        Parameter("Presión de Combustible", "4 bar")
    )

    LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp).height(550.dp)) {
        items(paramData) { item ->
            ParameterCard(info = item)
        }
    }
}

@Composable
fun Engine() {
    val paramData = listOf(
        Parameter("RPM", "3.000"),
        Parameter("Acelerador", "0%"),
        Parameter("Tiempo activo", "425min"),
        Parameter("Carga del motor", "30%"),
        Parameter("Temperatura Motor", "95°C"),
    )

    LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp).height(550.dp)) {
        items(paramData) { item ->
            ParameterCard(info = item)
        }
    }
}

@Composable
fun Temperature() {
    val paramData = listOf(
        Parameter("Temperatura Motor", "95°C"),
        Parameter("Temperatura del refrigerante", "80°C"),
    )

    LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp).height(550.dp)) {
        items(paramData) { item ->
            ParameterCard(info = item)
        }
    }
}*/

@Composable
fun ParametersScreen(parametros:List<Parametro>) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(parametros) { parametro ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = parametro.nombre,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = parametro.valor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}


@Composable
fun ParameterCard(info: Parametro) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(80.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(60.dp)
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = info.nombre,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = info.valor,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun IconF(painter: Painter, contentDescription: String) {
    androidx.compose.material3.Icon(
        painter = painter,
        contentDescription = contentDescription,
        tint = Color.White
    )
}

// funciones, clases y objetos












