package com.example.pruebav

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pruebav.database.CodigosError
import com.example.pruebav.database.ErroresCoche
import com.example.pruebav.database.guardarErrores
import kotlinx.coroutines.delay

@Composable
fun CError(context: Context, navController: NavController,viewModel: OBDViewModel) {

    var cargando by remember { mutableStateOf(true) }
    val vin by viewModel.vin.collectAsState()
    val codigosError by viewModel.codigosError.collectAsState()
    val listaerrores by viewModel.listacodigos.collectAsState()
    val toastMessage by viewModel.toastMessage

    val erroresList = remember(codigosError, vin) {
        codigosError.map { (codigo, descripcion) ->
            ErroresCoche(
                codigoError = codigo,
                vin = vin,
                descripcion = descripcion
            )
        }
    }
    var erroresEncontrados by remember { mutableStateOf<List<ErroresCoche>>(emptyList()) }

    LaunchedEffect(Unit) {
        delay(1000)

        cargando=false

        val codigosPuros = codigosError["listaErrores"]
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

    LaunchedEffect(toastMessage) {
        toastMessage?.let { mensaje ->
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            NavigationBar(modifier = Modifier.fillMaxWidth()) {
                NavItem(onClickAction = { navController.navigate("") }) {
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            Spacer(modifier = Modifier.height(35.dp))

            Row(modifier = Modifier.padding(16.dp)) {
                IconM(
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
                    ErrorList(context,vin,listaErrores = erroresList,viewModel)
                }
            }
        }
    }
}

//elementos composable

@Composable
fun ErrorList(context: Context, vin: String, listaErrores: List<ErroresCoche>,viewModel: OBDViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (listaErrores.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconM(
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
                    .weight(1f)
            ) {
                items(listaErrores) { error ->
                    ErrorCard(info = error)
                }
            }
        }

        Column {
            if (listaErrores.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { viewModel.borrarCodigos() },
                        colors = ButtonColors(
                            contentColor =Color.White,
                            containerColor = Color(0xFF670000),
                            disabledContentColor = Color.White,
                            disabledContainerColor =  Color(0xFF670000)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .padding(end = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Borrar Códigos", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            guardarErrores(context, vin, listaErrores)
                        },
                        colors = ButtonColors(
                            contentColor = Color.White,
                            containerColor = Color(0xFF1E88E5),
                            disabledContentColor = Color.White,
                            disabledContainerColor = Color(0xFF1E88E5)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .padding(start = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Guardar Datos", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

            } else{
                Button(
                    onClick = {
                        guardarErrores(context, vin, listaErrores)
                    },
                    colors = ButtonColors(
                        contentColor = Color.White,
                        containerColor = Color(0xFF1E88E5),
                        disabledContentColor = Color.White,
                        disabledContainerColor = Color(0xFF1E88E5)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Guardar registro vacío", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
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








