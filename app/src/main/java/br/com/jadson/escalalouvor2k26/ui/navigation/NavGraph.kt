package br.com.jadson.escalalouvor2k26.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import br.com.jadson.escalalouvor2k26.ui.screens.*
import br.com.jadson.escalalouvor2k26.ui.viewmodel.EscalaViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: EscalaViewModel,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController, viewModel)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController, viewModel)
        }
        composable(Screen.Escala.route) {
            EscalaScreen(navController, viewModel)
        }
        composable(Screen.CreateEscala.route) {
            CreateEscalaScreen(navController, viewModel)
        }
        composable(Screen.Integrantes.route) {
            IntegrantesScreen(viewModel)
        }
        composable(Screen.Solicitacoes.route) {
            SolicitacoesScreen(viewModel)
        }
        composable(Screen.Recados.route) {
            RecadosScreen(navController, viewModel)
        }
        composable(Screen.Perfil.route) {
            PerfilScreen(navController, viewModel)
        }
        composable(Screen.AdminSolicitacoes.route) {
            AdminSolicitacoesScreen(viewModel)
        }
        composable(Screen.EditEscala.route) {
            EditEscalaScreen(viewModel)
        }
        composable(Screen.CreateRecado.route) {
            CreateRecadoScreen(navController, viewModel)
        }
        composable(
            route = Screen.EditRecado.route,
            arguments = listOf(navArgument("recadoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val recadoId = backStackEntry.arguments?.getString("recadoId") ?: ""
            EditRecadoScreen(recadoId, navController, viewModel)
        }
        composable(
            route = Screen.ImageViewer.route,
            arguments = listOf(navArgument("recadoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val recadoId = backStackEntry.arguments?.getString("recadoId") ?: ""
            ImageViewerScreen(recadoId, navController, viewModel)
        }
    }
}
