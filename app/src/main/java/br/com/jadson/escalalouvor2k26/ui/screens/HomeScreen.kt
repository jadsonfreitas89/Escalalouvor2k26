package br.com.jadson.escalalouvor2k26.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import br.com.jadson.escalalouvor2k26.data.api.RetrofitClient
import br.com.jadson.escalalouvor2k26.data.model.Escala
import br.com.jadson.escalalouvor2k26.data.model.Notificacao
import br.com.jadson.escalalouvor2k26.ui.components.ErrorScreen
import br.com.jadson.escalalouvor2k26.ui.components.LoadingScreen
import br.com.jadson.escalalouvor2k26.ui.navigation.Screen
import br.com.jadson.escalalouvor2k26.ui.theme.*
import br.com.jadson.escalalouvor2k26.ui.viewmodel.EscalaViewModel
import br.com.jadson.escalalouvor2k26.ui.viewmodel.UiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import br.com.jadson.escalalouvor2k26.util.CultoUtils

@Composable
fun HomeScreen(navController: NavController, viewModel: EscalaViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val pendingCount by viewModel.pendingSolicitacoesCount.collectAsState()
    val notifications by viewModel.notificacoes.collectAsState()
    val isLider = currentUser?.funcao?.uppercase()?.contains("LIDER") == true

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            viewModel.carregarNotificacoes()
        }
    }

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val today = LocalDate.now()
    
    var showNextEscalaDialog by remember { mutableStateOf(false) }
    var selectedEscalaForDialog by remember { mutableStateOf<Escala?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            var showNotificationsDialog by remember { mutableStateOf(false) }

            // Header unificado
            Header(
                userName = currentUser?.nome ?: "Visitante",
                notificationCount = notifications.size,
                onProfileClick = { navController.navigate(Screen.Perfil.route) },
                onRefresh = { 
                    viewModel.loadData()
                    viewModel.carregarNotificacoes()
                },
                onNotificationsClick = { showNotificationsDialog = true }
            )

            if (showNotificationsDialog) {
                val context = androidx.compose.ui.platform.LocalContext.current
                NotificationsDialog(
                    notifications = notifications,
                    onDismiss = { showNotificationsDialog = false },
                    onMarkAsRead = { id -> 
                        viewModel.marcarNotificacaoComoLida(id) { sucesso, msg ->
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onNotificationClick = { notif ->
                        viewModel.marcarNotificacaoComoLida(notif.id)
                        showNotificationsDialog = false
                        
                        val notifType = notif.tipo.uppercase()
                        when {
                            notifType == br.com.jadson.escalalouvor2k26.util.NotificationHelper.TYPE_RECADO -> 
                                navController.navigate(Screen.Recados.route)
                            notifType == br.com.jadson.escalalouvor2k26.util.NotificationHelper.TYPE_ESCALA_ALTERADA || 
                            notifType == br.com.jadson.escalalouvor2k26.util.NotificationHelper.TYPE_LOUVORES ||
                            notifType.contains("ESCALA") || notifType.contains("LOUVOR") -> 
                                navController.navigate(Screen.Escala.route)
                            notifType == br.com.jadson.escalalouvor2k26.util.NotificationHelper.TYPE_SOLICITACAO_TROCA || 
                            notifType == br.com.jadson.escalalouvor2k26.util.NotificationHelper.TYPE_TROCA_RECUSADA ||
                            notifType.contains("SOLICITAC") || notifType.contains("TROCA") -> {
                                if (isLider) {
                                    navController.navigate(Screen.AdminSolicitacoes.route)
                                } else {
                                    navController.navigate(Screen.Solicitacoes.route)
                                }
                            }
                            else -> {}
                        }
                    }
                )
            }

            when (val state = uiState) {
                is UiState.Loading -> LoadingScreen()
                is UiState.Error -> ErrorScreen(state.message) { viewModel.loadData() }
                is UiState.Success -> {
                    val allFutureEscalas = state.data.escala
                        .mapNotNull { escala ->
                            try {
                                escala to LocalDate.parse(escala.data, dateFormatter)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        .filter { (_, date) -> !date.isBefore(today) }
                        .sortedBy { it.second }

                    val myNextEscalaEntry = allFutureEscalas.firstOrNull { (escala, _) ->
                        getMyRole(escala, currentUser?.nome) != null
                    }
                    val generalNextEscalaEntry = allFutureEscalas.firstOrNull()

                    if (showNextEscalaDialog && selectedEscalaForDialog != null) {
                        NextEscalaDialog(
                            escala = selectedEscalaForDialog!!, 
                            myName = currentUser?.nome,
                            viewModel = viewModel
                        ) {
                            showNextEscalaDialog = false 
                        }
                    }

                    // DESTAQUES (Visíveis para todos, incluindo Líder)
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        if (myNextEscalaEntry != null) {
                            DestaqueItem(
                                label = "SUA PRÓXIMA ESCALA",
                                labelColor = PrimaryOrange,
                                escala = myNextEscalaEntry.first,
                                currentUser = currentUser?.nome,
                                onClick = {
                                    selectedEscalaForDialog = myNextEscalaEntry.first
                                    showNextEscalaDialog = true
                                }
                            )
                        }

                        if (generalNextEscalaEntry != null && generalNextEscalaEntry.first != myNextEscalaEntry?.first) {
                            DestaqueItem(
                                label = "PRÓXIMO CULTO",
                                labelColor = Color.Gray,
                                escala = generalNextEscalaEntry.first,
                                currentUser = currentUser?.nome,
                                onClick = {
                                    selectedEscalaForDialog = generalNextEscalaEntry.first
                                    showNextEscalaDialog = true
                                }
                            )
                        }
                    }

                    // GRID DE MENU (Adaptado para Líder ou Membro)
                    if (isLider) {
                        LeaderGrid(navController, pendingCount, viewModel)
                    } else {
                        MemberGrid(navController)
                    }
                }
                else -> Box(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun MemberGrid(navController: NavController) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MenuCard(title = "Escala", icon = Icons.Default.CalendarMonth, startColor = EscalaStart, endColor = EscalaEnd, modifier = Modifier.weight(1f)) { navController.navigate(Screen.Escala.route) }
            MenuCard(title = "Integrantes", icon = Icons.Default.Groups, startColor = IntegrantesStart, endColor = IntegrantesEnd, modifier = Modifier.weight(1f)) { navController.navigate(Screen.Integrantes.route) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MenuCard(title = "Solicitações", icon = Icons.Default.Assignment, startColor = SolicitacoesStart, endColor = SolicitacoesEnd, modifier = Modifier.weight(1f)) { navController.navigate(Screen.Solicitacoes.route) }
            MenuCard(title = "Recados", icon = Icons.Default.ChatBubble, startColor = RecadosStart, endColor = RecadosEnd, modifier = Modifier.weight(1f)) { navController.navigate(Screen.Recados.route) }
        }
    }
}

@Composable
fun LeaderGrid(navController: NavController, pendingCount: Int, viewModel: EscalaViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val solicitacoesTitle = if (pendingCount > 0) "Solicitações ($pendingCount)" else "Solicitações"
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("ADMINISTRAÇÃO", style = MaterialTheme.typography.labelSmall, color = PrimaryOrange, fontWeight = FontWeight.Bold)
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MenuCard(title = "Escala", icon = Icons.Default.CalendarMonth, startColor = EscalaStart, endColor = EscalaEnd, modifier = Modifier.weight(1f)) { navController.navigate(Screen.Escala.route) }
            MenuCard(title = "Recados", icon = Icons.Default.ChatBubble, startColor = RecadosStart, endColor = RecadosEnd, modifier = Modifier.weight(1f)) { navController.navigate(Screen.Recados.route) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                MenuCard(title = solicitacoesTitle, icon = Icons.Default.Notifications, startColor = SolicitacoesStart, endColor = SolicitacoesEnd, modifier = Modifier.fillMaxWidth()) { navController.navigate(Screen.AdminSolicitacoes.route) }
                if (pendingCount > 0) {
                    Surface(color = Color.Red, shape = CircleShape, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = pendingCount.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            MenuCard(title = "Integrantes", icon = Icons.Default.Groups, startColor = IntegrantesStart, endColor = IntegrantesEnd, modifier = Modifier.weight(1f)) { navController.navigate(Screen.Integrantes.route) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MenuCard(title = "Editar Planilha", icon = Icons.Default.OpenInNew, startColor = Color(0xFF1D6F42), endColor = Color(0xFF0D3D22), modifier = Modifier.weight(1f)) {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(RetrofitClient.MASTER_SPREADSHEET_URL))
                context.startActivity(intent)
            }
            MenuCard(title = "Copiar Link Planilha", icon = Icons.Default.ContentCopy, startColor = Color(0xFF424242), endColor = Color(0xFF212121), modifier = Modifier.weight(1f)) {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Planilha Mestre", RetrofitClient.MASTER_SPREADSHEET_URL)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(context, "Link copiado.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun DestaqueItem(label: String, labelColor: Color, escala: Escala, currentUser: String?, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = labelColor)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark), onClick = onClick) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = escala.data, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = CultoUtils.getTituloCulto(escala.data), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    val myRole = getMyRole(escala, currentUser)
                    if (myRole != null) {
                        Text(text = "Você é: $myRole", style = MaterialTheme.typography.labelSmall, color = PrimaryOrange, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = onClick, modifier = Modifier.background(if (label == "SUA PRÓXIMA ESCALA") PrimaryOrange else Color.DarkGray, CircleShape)) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = if (label == "SUA PRÓXIMA ESCALA") Color.Black else Color.White)
                }
            }
        }
    }
}

@Composable
fun Header(
    userName: String, 
    notificationCount: Int,
    onProfileClick: () -> Unit, 
    onRefresh: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onProfileClick, modifier = Modifier.size(40.dp).background(SurfaceDark, CircleShape)) {
            Icon(imageVector = Icons.Default.Person, contentDescription = "Perfil", tint = PrimaryOrange, modifier = Modifier.size(24.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onProfileClick() }
        ) {
            Text(text = "Olá, $userName!", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(
                text = "ESCALA DE LOUVOR", 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Black, 
                color = Color.White,
                lineHeight = 20.sp,
                softWrap = false,
                overflow = TextOverflow.Visible
            )
        }

        // Ícone de Notificações com Badge
        Box {
            IconButton(onClick = onNotificationsClick, modifier = Modifier.size(40.dp).background(SurfaceDark, CircleShape)) {
                Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notificações", tint = if (notificationCount > 0) PrimaryOrange else Color.Gray, modifier = Modifier.size(20.dp))
            }
            if (notificationCount > 0) {
                Surface(
                    color = Color.Red,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .offset(x = (-2).dp, y = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (notificationCount > 9) "9+" else notificationCount.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        IconButton(onClick = onRefresh, modifier = Modifier.size(40.dp).background(SurfaceDark, CircleShape)) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Atualizar", tint = PrimaryOrange, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun NotificationsDialog(
    notifications: List<Notificacao>,
    onDismiss: () -> Unit,
    onMarkAsRead: (String) -> Unit,
    onNotificationClick: (Notificacao) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notificações", color = Color.White) },
        containerColor = SurfaceDark,
        text = {
            if (notifications.isEmpty()) {
                Text("Nenhuma notificação nova.", color = Color.Gray)
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    notifications.forEach { notif ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onNotificationClick(notif) },
                            colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = PrimaryOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = notif.titulo, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Text(text = notif.mensagem, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = notif.data.split("T").first(), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    TextButton(onClick = { 
                                        android.util.Log.d("NOTIFICACAO", "Clique em Marcar como lida, ID: ${notif.id}")
                                        onMarkAsRead(notif.id) 
                                    }) {
                                        Text("Marcar como lida", color = PrimaryOrange, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)) {
                Text("Fechar", color = Color.Black)
            }
        }
    )
}

@Composable
fun MenuCard(title: String, icon: ImageVector, startColor: Color, endColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(90.dp), shape = RoundedCornerShape(20.dp), onClick = onClick) {
        Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(startColor, endColor))).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
