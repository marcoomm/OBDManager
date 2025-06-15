package com.example.pruebav

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.pruebav.database.Coche
import com.example.pruebav.database.CocheDao
import com.example.pruebav.database.CodigosDao
import com.example.pruebav.database.CodigosError
import com.example.pruebav.database.Converters
import com.example.pruebav.database.ErroresCoche
import com.example.pruebav.database.ErroresCocheDao
import com.example.pruebav.database.NumeroVin
import com.example.pruebav.database.NumeroVinDao
import com.example.pruebav.database.ParametrosCoche
import com.example.pruebav.database.ParametrosDao
import com.example.pruebav.database.borrarErrores
import com.example.pruebav.database.borrarParametros
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Datos(navController: NavController, database: AppDatabase,context: Context) {

    var expandedVin by remember { mutableStateOf(false) }
    var expandedOption by remember { mutableStateOf(false) }
    var option by rememberSaveable { mutableStateOf("Parámetros") }
    var cargando by remember { mutableStateOf(false) }

    var todosVin  by remember { mutableStateOf<List<String>>(emptyList()) }
    var seleccionadoVin by rememberSaveable { mutableStateOf("") }
    var todosCoches by remember { mutableStateOf<List<CocheMarcaModelo>>(emptyList()) }

    var parametros by remember { mutableStateOf<ParametrosCoche?>(null) }
    var errores by remember { mutableStateOf<List<ErroresCoche>>(emptyList()) }

    var dataLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            todosVin = database.numeroVinDao().getAllVins()!!
            todosCoches = database.numeroVinDao().getCoches()
        }
        dataLoaded = true
    }

    LaunchedEffect(seleccionadoVin, option) {
        if (seleccionadoVin.isNotBlank()) {
            cargando = true

            try {
                //delay(2000)

                if (option == "Parámetros") {
                    parametros = database.parametrosDao().obtenerParametros(seleccionadoVin.trim().uppercase())
                }else {
                    errores = database.erroresDao().obtenerErroresCoche(seleccionadoVin.trim().uppercase()) ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("OBD_Database", "Error al leer datos: ${e.message}")
            } finally {
                cargando = false
            }
        }
    }


    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
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

                NavItem(onClickAction = {}) {
                    IconContainer {
                        StateLayer(modifier = Modifier.background(Color(0xFF1E88E5), shape = RoundedCornerShape(50.dp))){
                            IconM(
                                painter = painterResource(id = R.drawable.examples_detailed_view_mobile_icon1),
                                contentDescription = "Asistente icon"
                            )
                        }
                    }
                    Text(
                        "Manager",
                        modifier = Modifier.padding(top = 30.dp, start = 5.dp, end = 5.dp),
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
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1E1E), Color(0xFF121212))
                    )
                )
        ) {
            Spacer(modifier = Modifier.height(35.dp))

            Row(modifier = Modifier.padding(16.dp)) {
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
                    text = "Datos Guardados", modifier = Modifier.padding(start = 15.dp),
                    fontWeight = FontWeight.Bold, color = Color.White, fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // FILTER BUTTON
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(start = 22.dp, end = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box {
                    Button(
                        onClick = { expandedVin = !expandedVin },
                        modifier = Modifier
                            .width(145.dp)
                            .height(35.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color.White,
                            containerColor = Color(0xFF1E88E5)
                        )
                    ) {
                        Text(text = "Filtrar coche")
                    }

                    DropdownMenu(
                        expanded = expandedVin,
                        onDismissRequest = { expandedVin = false },
                        modifier = Modifier.background(Color(0xFF1E88E5)),
                        offset = DpOffset(x = 0.dp, y = 5.dp)
                    ) {
                        todosVin.forEachIndexed { index, vin ->
                            DropdownMenuItem(
                                onClick = {
                                    seleccionadoVin = vin.uppercase()
                                    Log.d("vinseleccionado", seleccionadoVin)
                                    expandedVin = false
                                },
                                text = { Text(vin, color = Color.White, fontSize = 16.sp) }
                            )
                            if (index < todosVin.lastIndex) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                            }
                        }
                    }

                }
                Box {
                    Button(
                        onClick = { expandedOption = !expandedOption },
                        modifier = Modifier
                            .width(170.dp)
                            .height(35.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color.White,
                            containerColor = Color(0xFF1E88E5)
                        )
                    ) {
                        Text(text = "Cambiar datos")
                    }

                    DropdownMenu(
                        expanded = expandedOption,
                        onDismissRequest = { expandedOption = false },
                        modifier = Modifier.background(Color(0xFF1E88E5)),
                        offset = DpOffset(x = 25.dp, y = 5.dp)
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                option = "Parámetros"
                                expandedOption = false
                            },
                            text = { Text("Parámetros", color = Color.White, fontSize = 16.sp) }
                        )

                        HorizontalDivider()

                        DropdownMenuItem(
                            onClick = {
                                option = "Códigos de error"
                                expandedOption = false
                            },
                            text = { Text("Códigos de error", color = Color.White, fontSize = 16.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.weight(1f)) {

                if (seleccionadoVin != "" || !dataLoaded) {
                    if (cargando) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    } else {
                        when (option) {
                            "Parámetros" -> {
                                if (parametros != null) {
                                    ListaParametros(
                                        parametros = parametros!!,
                                        vin = seleccionadoVin,
                                        context = context,
                                        onUpdate = {
                                            parametros = null
                                        }
                                    )

                                } else {
                                    DatosVacios(option)
                                }
                            }
                            "Códigos de error" -> {
                                if (errores.isNotEmpty()) {
                                    ListaErrores(errores, seleccionadoVin, context)
                                } else {
                                    DatosVacios(option)
                                }
                            }
                        }

                    }
                } else {
                    PantallaCarga()
                }
            }
        }
    }
}


@Composable
fun ListaParametros(parametros: ParametrosCoche, vin: String, context: Context, onUpdate: () -> Unit) {

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        borrarParametros(context, vin)
                        onUpdate()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF670000))
                ) {
                    Text("Eliminar parámetros", color = Color.White)
                }

            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding
        ) {
            items(parametros.parametros) { parametro ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(145.dp)
                                .height(70.dp)
                                .background(Color.White, shape = RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = parametro.valor,
                                color = Color.Black,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(30.dp))

                        Column {
                            Text(
                                text = parametro.nombre,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = parametro.categoria,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ListaErrores(
    errores: List<ErroresCoche>,
    vin: String,
    context: Context,
    onUpdate: () -> Unit = {}
) {
    val esSinErrores = errores.size == 1 && errores.first().codigoError == "NO_ERROR"

    Scaffold(
        bottomBar = {
            if (!esSinErrores) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            borrarErrores(context, vin)
                            onUpdate()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar errores",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Eliminar errores", color = Color.White)
                    }
                }
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (esSinErrores) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Mostrando los códigos de error de: $vin",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        elevation = CardDefaults.cardElevation(8.dp),
                        border = BorderStroke(2.dp, Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x884CAF50))
                                    .shadow(6.dp, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable._696306),
                                    contentDescription = "Icono del asistente",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = errores.first().descripcion
                                    ?: "¡No se detectaron errores en el vehículo!",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }

        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Mostrando los códigos de error de: $vin",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(contentPadding = innerPadding) {
                    items(errores) { error ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF212121)),
                            elevation = CardDefaults.cardElevation(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .height(65.dp)
                                        .background(Color(0xFFEF5350), shape = RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = error.codigoError,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(20.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Código de error",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = error.descripcion ?: "Sin descripción",
                                        color = Color.LightGray,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PantallaCarga(){

            Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Selecciona un vehículo para realizar la búsqueda",
                    color = Color.White
                )
            }
        }

}

@Composable
fun DatosVacios(tipo: String) {
    val mensaje = when (tipo) {
        "Parámetros" -> "No hay parámetros guardados para este coche"
        "Codigos de error" -> "No hay códigos de error guardados"
        else -> "No hay datos guardados"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconM(
                painter = painterResource(id = R.drawable.baseline_announcement_24),
                contentDescription = "icon",
                modifier = Modifier.size(50.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = mensaje,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

//dataclasses y appdatabase

data class CocheMarcaModelo(
    val marca: String,
    val modelo: String
)

@Database(entities = [ParametrosCoche::class, ErroresCoche::class, NumeroVin::class, Coche::class, CodigosError::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun parametrosDao(): ParametrosDao
    abstract fun erroresDao(): ErroresCocheDao
    abstract fun numeroVinDao(): NumeroVinDao
    abstract fun cocheDao(): CocheDao
    abstract fun codigosDao(): CodigosDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "obd_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}




