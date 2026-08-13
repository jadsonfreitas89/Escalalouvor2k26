package br.com.jadson.escalalouvor2k26.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Announcement
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Login : Screen("login", "Login", Icons.Default.Login)
    object Home : Screen("home", "Início", Icons.Default.Home)
    object Escala : Screen("escala", "Escala", Icons.Default.Event)
    object Integrantes : Screen("integrantes", "Integrantes", Icons.Default.Group)
    object Solicitacoes : Screen("solicitacoes", "Solicitações", Icons.Default.Assignment)
    object Recados : Screen("recados", "Recados", Icons.Default.Announcement)
    object Perfil : Screen("perfil", "Perfil", Icons.Default.Person)
    object AdminSolicitacoes : Screen("admin_solicitacoes", "Solicitações Pendentes", Icons.Default.Notifications)
    object EditEscala : Screen("edit_escala", "Editar Escala", Icons.Default.Edit)
    object CreateEscala : Screen("create_escala", "Criar Escala", Icons.Default.Add)
    object CreateRecado : Screen("create_recado", "Escrever Recado", Icons.Default.Add)
    object EditRecado : Screen("edit_recado/{recadoId}", "Editar Recado", Icons.Default.Edit) {
        fun createRoute(recadoId: String) = "edit_recado/$recadoId"
    }
    object ImageViewer : Screen("image_viewer/{recadoId}", "Visualizar Imagem", Icons.Default.Announcement) {
        fun createRoute(recadoId: String) = "image_viewer/$recadoId"
    }
}
