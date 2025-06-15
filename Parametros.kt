package com.example.pruebav

import android.content.Context
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.pruebav.database.Parametro
import com.example.pruebav.database.guardarParametros
import kotlinx.coroutines.launch

@Composable
fun Parametros(context: Context, navController: NavController, viewModel: OBDViewModel) {

    val vin by viewModel.vin.collectAsState()
    val datosMap by viewModel.parametros.collectAsState()
    val listaParametros = remember(datosMap) {
        datosMap.map { (nombre, valor) ->
            Parametro(nombre, valor, categoria = categoriaSegunNombre(nombre))
        }
    }
    
    val isLoading by remember { derivedStateOf { datosMap.isEmpty() } }
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

    Scaffold(
        bottomBar = {

            Column{
                Button(
                    onClick = { coroutineScope.launch {
                        guardarParametros(context, vin, listaParametros)
                    }},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    colors = ButtonColors(
                        contentColor = Color.White,
                        containerColor = Color(0xFF1E88E5),
                        disabledContentColor = Color.White,
                        disabledContainerColor = Color(0xFF1E88E5)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Guardar Datos", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                NavigationBar(modifier = Modifier.fillMaxWidth()) {
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

                    NavItem(onClickAction = { navController.navigate("datos") }) {
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1E1E), Color(0xFF121212))
                    )
                )
        ) {
            Spacer(modifier = Modifier.height(35.dp))

            // TOP APP BAR
            Row(modifier = Modifier.padding(16.dp)) {
                IconM(
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

            // BOTÓN DE FILTRO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(start = 35.dp, end = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Mostrando : $selectedOption",
                    modifier = Modifier.padding(top = 6.dp),
                    fontSize = 14.sp,
                    color = Color.White
                )
                Box {
                    Button(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.width(110.dp).height(35.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonColors(
                            contentColor = Color.White,
                            containerColor = Color(0xFF1E88E5),
                            disabledContentColor = Color.White,
                            disabledContainerColor = Color(0xFF1E88E5)
                        )
                    ) {
                        IconF(
                            painter = painterResource(id = R.drawable.filterarrows),
                            contentDescription = "icon"
                        )
                        Text(text = "Filtrar")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF1E88E5))
                    ) {
                        val categorias = listOf("Todo", "Presión", "Combustible", "Motor", "Temperatura")
                        categorias.forEachIndexed { index, categoria ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedOption = categoria
                                    expanded = false
                                },
                                text = {
                                    Text(text = categoria, color = Color.White, fontSize = 16.sp)
                                }
                            )
                            if (index < categorias.lastIndex) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF1E88E5),
                        strokeWidth = 4.dp
                    )
                }
            } else {
                Column {
                    val filtrados = when (selectedOption) {
                        "Presión" -> listaParametros.filter { it.categoria == "Presión" }
                        "Motor" -> listaParametros.filter { it.categoria == "Motor" }
                        "Temperatura" -> listaParametros.filter { it.categoria == "Temperatura" }
                        "Combustible" -> listaParametros.filter { it.categoria == "Combustible" }
                        "Otros" -> listaParametros.filter { it.categoria == "Otros" }
                        else -> listaParametros
                    }

                    ParametersScreen(filtrados)
                }
            }
        }
    }
}

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
fun IconF(painter: Painter, contentDescription: String) {
    androidx.compose.material3.Icon(
        painter = painter,
        contentDescription = contentDescription,
        tint = Color.White
    )
}

// funciones

fun categoriaSegunNombre(nombre: String): String {
    return when (nombre) {
        "RPM", "Speed", "Engine Load", "Throttle Position" -> "Motor"

        "Fuel Type", "Fuel Level", "Fuel Pressure", "Fuel Rail Pressure" -> "Combustible"

        "Intake Manifold Pressure" -> "Presión"

        "Air Intake Temperature", "Ambient Air Temperature",
        "Engine Coolant Temperature", "Oil Temperature" -> "Temperatura"

        else -> "Otros"
    }
}











