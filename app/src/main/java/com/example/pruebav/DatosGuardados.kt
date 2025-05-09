package com.example.pruebav

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.pruebav.database.Parametro
import com.example.pruebav.database.ParametrosCoche
import com.example.pruebav.database.ParametrosDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun Datos(navController: NavController, database: AppDatabase) {

    // Cargar datos al inicio (lógica de Room usando coroutines)
    val coroutineScope = rememberCoroutineScope()
    var expandedVin by remember { mutableStateOf(false) }
    var expandedOption by remember { mutableStateOf(false) }
    var option by remember { mutableStateOf("Parametros") }
    var cargando by remember { mutableStateOf(false) }

    var todosVin  by remember { mutableStateOf<List<String>>(emptyList()) }
    var seleccionadoVin by remember{ mutableStateOf("") }

    var todosCoches by remember { mutableStateOf<List<CocheMarcaModelo>>(emptyList()) }
    var seleccionadoCoche by remember { mutableStateOf<CocheMarcaModelo?>(null) }

    var parametros by remember { mutableStateOf<ParametrosCoche?>(null) }
    var errores by remember { mutableStateOf<List<ErroresCoche>>(emptyList()) }


    LaunchedEffect(Unit) {
        coroutineScope.launch {

            todosVin = database.numeroVinDao().getAllVins()!!
            todosCoches = database.numeroVinDao().getCoches()

            todosCoches.forEach{ coche ->
                Log.d("Coche", "${coche.marca} ${coche.modelo}")
            }

            /*
            if (vinS != "") {
                //vins = database.numeroVinDao().getAllVins() ?: emptyList()
                //errores = database.erroresDao().obtenerErroresCoche(vinS) ?: emptyList()

            }
            */

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

                NavItem(onClickAction = {}) {
                    IconContainer {
                        StateLayer(modifier = Modifier.background(Color(0xFF1E88E5), shape = RoundedCornerShape(50.dp))){
                            Icon(
                                painter = painterResource(id = R.drawable.examples_detailed_view_mobile_icon1),
                                contentDescription = "Asistente icon"
                            )
                        }
                    }
                    Text(
                        "Manager",
                        modifier = Modifier.padding(top = 30.dp, start = 14.dp, end = 5.dp),
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

            // TOP APP BAR
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(
                    onClick = { navController.navigate("inicio") },
                    modifier = Modifier.width(30.dp).height(28.dp).rotate(180.0F),
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
                Button(
                    onClick = { expandedVin = !expandedVin },
                    modifier = Modifier.width(145.dp).height(35.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonColors(contentColor = Color.White, containerColor = Color(0xFF1E88E5), disabledContentColor = Color.White, disabledContainerColor =Color(0xFF1E88E5)),
                ) {
                    Text(text = "Filtrar coche")
                    Row(modifier = Modifier
                        .padding(top = 30.dp)
                    ){
                        DropdownMenu(
                            expanded = expandedVin,
                            onDismissRequest = { expandedVin = false },
                            modifier = Modifier.background(Color(0xFF1E88E5))
                        ) {

                            todosCoches.forEach{coche->
                                DropdownMenuItem(
                                    onClick = {
                                        seleccionadoCoche = coche
                                        expandedVin = false
                                        cargando=true
                                    },text = { Text(text = "${coche.marca} ${coche.modelo}", color = Color.White, fontSize = 16.sp) }
                                )
                            }

                            /*
                            vins.forEach { vin ->
                                DropdownMenuItem(
                                    onClick = {
                                        vinS = vin
                                        expandedVin = false
                                    },
                                    text = { Text(vin, color = Color.White, fontSize = 16.sp) }
                                )
                            }*/
                        }
                    }
                }

                Button(
                    onClick = { expandedOption = !expandedOption },
                    modifier = Modifier.width(170.dp).height(35.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonColors(contentColor = Color.White, containerColor = Color(0xFF1E88E5), disabledContentColor = Color.White, disabledContainerColor =Color(0xFF1E88E5)),
                ) {
                    Text(text = "Cambiar datos")
                    Row(
                        modifier = Modifier
                            .padding(top = 30.dp, start = 15.dp)
                    ) {
                        DropdownMenu(
                            expanded = expandedOption,
                            onDismissRequest = { expandedOption = false },
                            modifier = Modifier.background(Color(0xFF1E88E5))
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    option = "Parametros"
                                    CoroutineScope(Dispatchers.IO).launch {
                                        if (seleccionadoCoche != null) {
                                            val vin = database.numeroVinDao().getVinFromCoche(
                                                seleccionadoCoche!!.marca, seleccionadoCoche!!.modelo
                                            )

                                            val datos = database.parametrosDao().obtenerParametros(vin)

                                            withContext(Dispatchers.Main) {
                                                seleccionadoVin = vin
                                                parametros = datos
                                                cargando = false
                                            }
                                        } else {
                                            Log.e("OBD_Database", "los datos no se pueden cargar suprimo")
                                            withContext(Dispatchers.Main) {
                                                cargando = false // También aquí, por si no hay coche válido
                                            }
                                        }
                                    }

                                    expandedOption = false
                                },
                                text = { Text("Parametros", color = Color.White, fontSize = 16.sp) })

                            HorizontalDivider()

                            DropdownMenuItem(
                                onClick = {
                                    option = "Codigos de error"

                                    CoroutineScope(Dispatchers.IO).launch {
                                        if (seleccionadoCoche != null) {
                                            val vin = database.numeroVinDao().getVinFromCoche(
                                                seleccionadoCoche!!.marca, seleccionadoCoche!!.modelo
                                            )

                                            val datos = database.erroresDao().obtenerErroresCoche(vin)

                                            withContext(Dispatchers.Main) {
                                                seleccionadoVin = vin
                                                if (datos != null) {
                                                    errores = datos
                                                }
                                            }
                                            cargando = false
                                        } else {
                                            Log.e("OBD_Database", "los datos no se pueden cargar suprimo")
                                            withContext(Dispatchers.Main) {
                                                cargando = false // También aquí, por si no hay coche válido
                                            }
                                        }
                                    }
                                    expandedOption = false
                                },
                                text = { Text("Códigos de error", color = Color.White, fontSize = 16.sp) })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CONTENIDO SCROLLEABLE
            Box(modifier = Modifier.weight(1f)) {

                if (seleccionadoCoche != null) {
                    if (cargando) {
                        // Mostrar loading mientras se obtienen los datos
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    } else {
                        when (option) {
                            "Parametros" -> {
                                if (parametros != null) {
                                    ListaParametros(parametros!!)
                                } else {
                                    DatosVacios()
                                }
                            }
                            "Codigos de error" -> {
                                if (errores.isNotEmpty()) {
                                    ListaErrores(errores)
                                } else {
                                    DatosVacios()
                                }
                            }
                        }
                    }
                } else {
                    // No se ha seleccionado un coche
                    DatosVacios()
                }


                /*
                if(seleccionadoVin.isNotEmpty()){
                    PantallaCarga()

                    when (option) {
                        "Parametros" -> {
                            if (parametros != null) {
                                ListaParametros(parametros!!)
                            } else {
                                DatosVacios()
                            }
                        }
                        "Codigos de error" -> {
                            if (errores.isNotEmpty()) {
                                ListaErrores(errores)
                            } else {
                                DatosVacios()
                            }
                        }
                    }
                }else{
                    DatosVacios()
                }
                */

            }
        }
    }
}


@Composable
fun ListaParametros(parametros: ParametrosCoche) {
    // Lista de parámetros con el contenido de la base de datos
    LazyColumn {
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
                        // Si quieres añadir hora de lectura, tendrías que incluirla en Parametro
                        // Text(text = "Comprobado a las: 12:45", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}


@Composable
fun ListaErrores(errores: List<ErroresCoche>) {
    LazyColumn {
        items(errores) { error ->
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
                            text = error.codigoError,
                            color = Color.Black,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(30.dp))

                    Text(
                        text = error.descripcion ?: "Sin descripción",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )

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
fun DatosVacios(){
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
        contentAlignment = Alignment.Center)
    {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No hay datos guardados",
                color = Color.White
            )
        }
    }
}

data class CocheMarcaModelo(
    val marca: String,
    val modelo: String
)


@Database(entities = [ParametrosCoche::class, ErroresCoche::class, NumeroVin::class, Coche::class, CodigosError::class], version = 2, exportSchema = false)
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




