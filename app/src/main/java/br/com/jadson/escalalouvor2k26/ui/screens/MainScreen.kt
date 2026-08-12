package br.com.jadson.escalalouvor2k26.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import br.com.jadson.escalalouvor2k26.ui.navigation.NavGraph
import br.com.jadson.escalalouvor2k26.ui.navigation.Screen
import br.com.jadson.escalalouvor2k26.ui.viewmodel.EscalaViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.jadson.escalalouvor2k26.data.session.SessionManager

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val sessionManager = SessionManager(context)
    val navController = rememberNavController()
    
    val viewModel: EscalaViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EscalaViewModel(sessionManager = sessionManager) as T
            }
        }
    )
    
    val currentUser by viewModel.currentUser.collectAsState()
    val startDestination = if (sessionManager.isLoggedIn()) Screen.Home.route else Screen.Login.route

    Scaffold(
        // O rodapé (BottomBar) foi removido permanentemente para limpar a interface.
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            viewModel = viewModel,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
