package com.example.pruebav

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavegacionEntreVentanas(
    context: Context,
    enableBluetoothLauncher: ActivityResultLauncher<Intent>,
) {

    val application = context.applicationContext as Application
    val viewModel: OBDViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(application)
    )
    val controlador = rememberNavController()
    val db = AppDatabase.getDatabase(context)

    NavHost(navController = controlador, startDestination = "inicio") {
        composable("inicio") {
            OBDConnectionScreen(context, enableBluetoothLauncher, controlador, viewModel,db)
        }
        composable("parametros") {
            Parametros(context, controlador, viewModel)
        }
        composable("errores") {
            CError(context, controlador, viewModel)
        }
        composable("datos") {
            Datos(controlador, db,context)
        }
        composable("asistente"){
            AI(controlador,db)
        }
    }
}


