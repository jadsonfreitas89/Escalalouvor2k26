package br.com.jadson.escalalouvor2k26.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.jadson.escalalouvor2k26.data.model.Escala
import br.com.jadson.escalalouvor2k26.ui.components.ErrorScreen
import br.com.jadson.escalalouvor2k26.ui.components.LoadingScreen
import br.com.jadson.escalalouvor2k26.ui.theme.PrimaryOrange
import br.com.jadson.escalalouvor2k26.ui.theme.SurfaceDark
import br.com.jadson.escalalouvor2k26.ui.viewmodel.EscalaViewModel
import br.com.jadson.escalalouvor2k26.ui.viewmodel.UiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscalaScreen(navController: NavController, viewModel: EscalaViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLider = currentUser?.funcao?.uppercase()?.contains("LIDER") == true
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escala Completa", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            /* Botão movido para dentro da lista de escalas para melhor organização */
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is UiState.Loading -> LoadingScreen()
                is UiState.Error -> ErrorScreen(state.message) { viewModel.loadData() }
                is UiState.Success -> {
                    val allEscalas = state.data.escala.mapNotNull { escala ->
                        try {
                            escala to LocalDate.parse(escala.data, dateFormatter)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val proximas = allEscalas.filter { !it.second.isBefore(today) }.sortedBy { it.second }
                    val anteriores = allEscalas.filter { it.second.isBefore(today) }.sortedByDescending { it.second }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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
                            item {
                                SectionHeader("PRÓXIMAS")
                            }
                            items(proximas) { (escala, _) ->
                                EscalaItemCard(escala, currentUser?.nome, viewModel, isProxima = true)
                            }
                        }

                        if (anteriores.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                SectionHeader("ANTERIORES")
                            }
                            items(anteriores) { (escala, _) ->
                                EscalaItemCard(escala, currentUser?.nome, viewModel, isProxima = false)
                            }
                        }
                        
                        if (proximas.isEmpty() && anteriores.isEmpty()) {
                            item {
                                Text(
                                    text = "Nenhuma escala disponível.",
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = Color.Gray
                                )
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
fun EscalaItemCard(escala: Escala, myName: String?, viewModel: EscalaViewModel, isProxima: Boolean) {
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
                Text(
                    text = escala.data,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isProxima) PrimaryOrange else Color.White
                )
                
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
            InfoRowLocal("Músicos", escala.musicos)
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
