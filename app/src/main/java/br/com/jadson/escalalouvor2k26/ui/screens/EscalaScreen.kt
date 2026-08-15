package br.com.jadson.escalalouvor2k26.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.jadson.escalalouvor2k26.data.model.Escala
import br.com.jadson.escalalouvor2k26.data.model.Integrante
import br.com.jadson.escalalouvor2k26.data.model.Solicitacao
import br.com.jadson.escalalouvor2k26.ui.components.ErrorScreen
import br.com.jadson.escalalouvor2k26.ui.components.LoadingScreen
import br.com.jadson.escalalouvor2k26.ui.theme.PrimaryOrange
import br.com.jadson.escalalouvor2k26.ui.theme.SurfaceDark
import br.com.jadson.escalalouvor2k26.ui.viewmodel.EscalaViewModel
import br.com.jadson.escalalouvor2k26.ui.viewmodel.UiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import br.com.jadson.escalalouvor2k26.util.CultoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscalaScreen(navController: NavController, viewModel: EscalaViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLider = currentUser?.funcao?.uppercase()?.contains("LIDER") == true
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val today = LocalDate.now()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = { Text("Escala Completa", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = Color.White
                    )
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = PrimaryOrange,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PrimaryOrange
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("ESCALAS", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("SOLICITAÇÕES", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is UiState.Loading -> LoadingScreen()
                is UiState.Error -> ErrorScreen(state.message) { viewModel.loadData() }
                is UiState.Success -> {
                    if (selectedTab == 0) {
                        val proximas = remember(state.data.escala, today) {
                            state.data.escala.mapNotNull { escala ->
                                try {
                                    escala to LocalDate.parse(escala.data, dateFormatter)
                                } catch (e: Exception) {
                                    null
                                }
                            }.filter { !it.second.isBefore(today) }.sortedBy { it.second }
                        }

                        val anteriores = remember(state.data.escala, today) {
                            state.data.escala.mapNotNull { escala ->
                                try {
                                    escala to LocalDate.parse(escala.data, dateFormatter)
                                } catch (e: Exception) {
                                    null
                                }
                            }.filter { it.second.isBefore(today) }.sortedByDescending { it.second }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val integrantes = state.data.integrantes
                            
                            if (isLider) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = { navController.navigate(br.com.jadson.escalalouvor2k26.ui.navigation.Screen.CreateEscala.route) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                            Spacer(Modifier.width(8.dp))
                                            Text("NOVA ESCALA", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        }
                                        
                                        Button(
                                            onClick = { navController.navigate(br.com.jadson.escalalouvor2k26.ui.navigation.Screen.EditEscala.route) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryOrange)
                                            Spacer(Modifier.width(8.dp))
                                            Text("EDITAR ESCALA", color = PrimaryOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }

                            if (proximas.isNotEmpty()) {
                                item { SectionHeader("PRÓXIMAS") }
                                items(proximas, key = { it.first.data }) { (escala, _) ->
                                    EscalaItemCard(escala, currentUser?.nome, viewModel, isProxima = true, integrantes = integrantes)
                                }
                            }

                            if (anteriores.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    SectionHeader("ANTERIORES")
                                }
                                items(anteriores, key = { it.first.data }) { (escala, _) ->
                                    EscalaItemCard(escala, currentUser?.nome, viewModel, isProxima = false, integrantes = integrantes)
                                }
                            }
                        }
                    } else {
                        // SOLICITAÇÕES
                        val solicitacoes = state.data.solicitacoes
                        
                        if (solicitacoes.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Nenhuma solicitação encontrada.", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(solicitacoes, key = { it.id }) { solicitacao ->
                                    SolicitacaoTrocaCard(
                                        solicitacao = solicitacao,
                                        isLider = isLider,
                                        isOwner = solicitacao.quemPediu.equals(currentUser?.nome, ignoreCase = true),
                                        onApprove = {
                                            viewModel.processaSolicitacao(
                                                dataEscala = solicitacao.dataEscala,
                                                quemPediu = solicitacao.quemPediu,
                                                substituto = solicitacao.substituto,
                                                acao = "APROVAR",
                                                onSuccess = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                                                onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                            )
                                        },
                                        onReject = { motivo ->
                                            viewModel.processaSolicitacao(
                                                dataEscala = solicitacao.dataEscala,
                                                quemPediu = solicitacao.quemPediu,
                                                substituto = solicitacao.substituto,
                                                acao = "RECUSAR",
                                                motivoDecisao = motivo,
                                                onSuccess = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                                                onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                            )
                                        },
                                        onCancel = {
                                            viewModel.processaSolicitacao(
                                                dataEscala = solicitacao.dataEscala,
                                                quemPediu = solicitacao.quemPediu,
                                                substituto = solicitacao.substituto,
                                                acao = "CANCELAR",
                                                onSuccess = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                                                onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun SolicitacaoTrocaCard(
    solicitacao: Solicitacao,
    isLider: Boolean,
    isOwner: Boolean,
    onApprove: () -> Unit,
    onReject: (String) -> Unit,
    onCancel: () -> Unit
) {
    var showRejectDialog by remember { mutableStateOf(false) }
    var motivoRecusa by remember { mutableStateOf("") }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Recusar Solicitação") },
            text = {
                OutlinedTextField(
                    value = motivoRecusa,
                    onValueChange = { motivoRecusa = it },
                    label = { Text("Motivo da recusa (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { onReject(motivoRecusa); showRejectDialog = false }) { Text("RECUSAR", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("CANCELAR") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = solicitacao.dataEscala, fontWeight = FontWeight.Bold, color = PrimaryOrange)
                SolicitacaoStatusBadge(solicitacao.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(text = "${solicitacao.quemPediu} ➔ ${solicitacao.substituto}", color = Color.White, fontWeight = FontWeight.Bold)
            
            if (isLider || isOwner) {
                Text(text = "Motivo: ${solicitacao.motivo}", color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                
                if (!solicitacao.motivoDecisao.isNullOrBlank()) {
                    Text(text = "Resposta: ${solicitacao.motivoDecisao}", color = PrimaryOrange, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }

            if (solicitacao.status.uppercase() == "PENDENTE") {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isLider) {
                        Button(
                            onClick = { showRejectDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("RECUSAR", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = onApprove,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("APROVAR", style = MaterialTheme.typography.labelSmall)
                        }
                    } else if (isOwner) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                        ) {
                            Text("CANCELAR SOLICITAÇÃO", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SolicitacaoStatusBadge(status: String) {
    val color = when (status.uppercase()) {
        "PENDENTE" -> Color(0xFFFFC107)
        "APROVADA", "AUTORIZADA" -> Color(0xFF4CAF50)
        "RECUSADA" -> Color(0xFFF44336)
        "CANCELADA" -> Color.Gray
        else -> Color.Gray
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = PrimaryOrange,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun EscalaItemCard(
    escala: Escala, 
    myName: String?, 
    viewModel: EscalaViewModel, 
    isProxima: Boolean,
    integrantes: List<Integrante> = emptyList()
) {
    var showDetails by remember { mutableStateOf(false) }

    if (showDetails) {
        NextEscalaDialog(
            escala = escala, 
            myName = myName,
            viewModel = viewModel
        ) { showDetails = false }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        onClick = { showDetails = true }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = escala.data,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isProxima) PrimaryOrange else Color.White
                    )
                    Text(
                        text = CultoUtils.getTituloCulto(escala.data),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                
                val myRole = getMyRole(escala, myName)
                if (myRole != null) {
                    Surface(
                        color = PrimaryOrange.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "VOCÊ ESTÁ ESCALADO",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.DarkGray)
            
            InfoRowLocal("Dirigente", escala.dirigente)
            InfoRowLocal("Vocal", escala.vocal)
            
            val musicosFormatados = remember(escala.musicos, integrantes) {
                formatMusiciansWithInstrument(escala.musicos, integrantes)
                    .replace("• ", "")
                    .replace("\n", ", ")
            }
            
            InfoRowLocal("Músicos", if (musicosFormatados.isBlank()) escala.musicos else musicosFormatados)

            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { showDetails = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "VER LOUVORES E LINKS",
                    color = PrimaryOrange,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InfoRowLocal(label: String, value: String) {
    if (value.isNotBlank()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        }
    }
}
