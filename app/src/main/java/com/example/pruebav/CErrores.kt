package com.example.pruebav

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pruebav.database.CodigosError
import com.example.pruebav.database.ErroresCoche
import com.example.pruebav.database.ErroresCocheDao
import com.example.pruebav.database.cargarCodigosDesdeJson
import com.example.pruebav.database.guardarErrores
import com.example.pruebav.database.obtenerErrores
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun CError(context: Context, navController: NavController,viewModel: OBDViewModel) {

    var cargando by remember { mutableStateOf(true) }
    val vin by viewModel.vin.collectAsState()
    val listaErrores by viewModel.codigosError.collectAsState()

    val erroresList = remember(listaErrores, vin) {
        listaErrores.map { (codigo, descripcion) ->
            ErroresCoche(
                codigoError = codigo,
                vin = vin,
                descripcion = descripcion
            )
        }
    }

    val listaerrores by viewModel.listacodigos.collectAsState()
    var erroresEncontrados by remember { mutableStateOf<List<ErroresCoche>>(emptyList()) }

    LaunchedEffect(Unit) {
        delay(1000)

        cargando=false

        // Extraemos los códigos puros del mapa
        val codigosPuros = listaErrores["listaErrores"]
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        /*
                listaErrores.find { it.codigoError == codigo } ?: CodigosError(
            codigoError = codigo,
            descripcion = "Descripción no disponible",
            categoria = "Categoria no disponible",
            componente = "Componente no disponible"
        )
         */

        // Aquí puedes mapear 'codigosPuros' contra 'listaErroresJson' si necesitas
        erroresEncontrados = codigosPuros.mapNotNull { codigo ->
            val codigoError = listaerrores.find { it.codigoError == codigo }
            if (codigoError != null) {
                ErroresCoche(
                    codigoError = codigoError.codigoError,
                    vin = vin,
                    descripcion = codigoError.descripcion
                )
            } else {
                null
            }
        }

    }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            NavigationBar(modifier = Modifier.fillMaxWidth()) {
                NavItem(onClickAction = { navController.navigate("") }) {
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

                NavItem(onClickAction = { }) {
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
                        modifier = Modifier.padding(top = 25.dp, start = 14.dp, end = 5.dp),
                        fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold
                    )
                }

                NavItem(onClickAction = { navController.navigate("") }) {
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
                    text = "Códigos de error", modifier = Modifier.padding(start = 15.dp),
                    fontWeight = FontWeight.Bold, color = Color.White, fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (cargando) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else {
                    ErrorList(listaErrores = erroresList)
                }
            }

            Button(
                onClick = {
                    if(!cargando){
                        guardarErrores(context, erroresList)
                    }else{
                        Toast.makeText(context, "Cargando datos...", Toast.LENGTH_SHORT).show()
                    }},
                colors = ButtonColors(contentColor = Color.White, containerColor = Color(0xFF1E88E5), disabledContentColor = Color.White, disabledContainerColor = Color(0xFF1E88E5)),
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

//elementos composable

@Composable
fun ErrorList(listaErrores: List<ErroresCoche>) {
    if (listaErrores.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_check_24),
                    contentDescription = "icon",
                    modifier = Modifier.size(50.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No se encontraron códigos de error",
                    color = Color.White
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(550.dp)
        ) {
            items(listaErrores) { error ->
                ErrorCard(info = error)
            }
        }
    }
}


@Composable
fun ErrorCard(info: ErroresCoche) {
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
                    text = info.codigoError,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = info.descripcion.toString(),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

//funciones

fun mapearErrores(pid: List<String>, listaErrores: List<CodigosError>): List<CodigosError> {
    return pid.map { codigo ->
        listaErrores.find { it.codigoError == codigo } ?: CodigosError(
            codigoError = codigo,
            descripcion = "Descripción no disponible",
            categoria = "Categoria no disponible",
            componente = "Componente no disponible"
        )
    }
}








