package br.com.jadson.escalalouvor2k26.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.jadson.escalalouvor2k26.data.model.LouvorItem
import br.com.jadson.escalalouvor2k26.ui.components.ErrorScreen
import br.com.jadson.escalalouvor2k26.ui.components.LoadingScreen
import br.com.jadson.escalalouvor2k26.ui.theme.PrimaryOrange
import br.com.jadson.escalalouvor2k26.ui.viewmodel.EscalaViewModel
import br.com.jadson.escalalouvor2k26.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEscalaScreen(viewModel: EscalaViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    
    var selectedDate by remember { mutableStateOf("") }
    var dirigente by remember { mutableStateOf("") }
    var vocal by remember { mutableStateOf("") }
    var musicos by remember { mutableStateOf("") }
    var mesario by remember { mutableStateOf("") }
    var louvoresResumo by remember { mutableStateOf("") }
    var uniforme by remember { mutableStateOf("") }
    var praiseItems by remember { mutableStateOf(emptyList<LouvorItem>()) }

    val integrantes = if (uiState is UiState.Success) (uiState as UiState.Success).data.integrantes else emptyList()
    val allPraiseItems = if (uiState is UiState.Success) (uiState as UiState.Success).data.linkLouvores else emptyList()
    
    // Filtros por função
    val dirigentesOptions = integrantes.filter { it.funcao.contains("Dirigente", ignoreCase = true) || it.funcao.contains("Lider", ignoreCase = true) }
    val vocalOptions = integrantes.filter { it.funcao.contains("Vocal", ignoreCase = true) || it.funcao.contains("Integrante", ignoreCase = true) }
    val musicosOptions = integrantes.filter { it.funcao.contains("Musico", ignoreCase = true) || it.funcao.contains("Músico", ignoreCase = true) }
    val mesariosOptions = integrantes.filter { it.funcao.contains("Mesário", ignoreCase = true) || it.funcao.contains("Mesario", ignoreCase = true) }

    val isLider = currentUser?.funcao?.uppercase()?.contains("LIDER") == true
    val isDirigente = currentUser?.funcao?.uppercase()?.contains("DIRIGENTE") == true
    val canManageLinks = isLider || isDirigente

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Escala", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is UiState.Loading -> LoadingScreen()
                is UiState.Error -> ErrorScreen(state.message) { viewModel.loadData() }
                is UiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Seletor de Data
                        Text("Selecione a Escala para Editar:", color = PrimaryOrange, fontWeight = FontWeight.Bold)
                        var expanded by remember { mutableStateOf(false) }
                        
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (selectedDate.isEmpty()) "Escolher data..." else selectedDate, color = Color.White)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                state.data.escala.forEach { escala ->
                                    DropdownMenuItem(
                                        text = { Text(escala.data) },
                                        onClick = {
                                            selectedDate = escala.data
                                            dirigente = escala.dirigente
                                            vocal = escala.vocal
                                            musicos = escala.musicos
                                            mesario = escala.mesario
                                            louvoresResumo = escala.louvores
                                            uniforme = escala.uniforme
                                            
                                            // Importar Louvores
                                            val details = allPraiseItems.filter { it.dataEscala == escala.data }
                                            if (details.isNotEmpty()) {
                                                praiseItems = details.sortedBy { it.ordem }
                                            } else if (escala.louvores.isNotBlank()) {
                                                // Se não tem detalhes, tenta importar do resumo
                                                praiseItems = escala.louvores.split(",").mapIndexed { index, s ->
                                                    LouvorItem(dataEscala = escala.data, ordem = index + 1, louvor = s.trim())
                                                }
                                            } else {
                                                praiseItems = emptyList()
                                            }
                                            
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (selectedDate.isNotEmpty()) {
                            SingleSelectionField("Dirigente:", dirigente, dirigentesOptions) { dirigente = it }

                            MultiSelectionField("Vocal:", vocal, vocalOptions) { vocal = it }

                            MultiSelectionField("Músicos:", musicos, musicosOptions) { musicos = it }

                            SingleSelectionField("Mesário:", mesario, mesariosOptions) { mesario = it }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.DarkGray)

                            // EDITOR DE LOUVORES
                            PraiseListEditor(
                                praises = praiseItems,
                                isAllowedToEdit = canManageLinks,
                                onPraisesChanged = { list ->
                                    praiseItems = list
                                    // Atualiza o resumo automaticamente para manter consistência
                                    louvoresResumo = list.filter { (it.louvor ?: "").isNotBlank() }.joinToString(", ") { it.louvor ?: "" }
                                }
                            )

                            Text("Resumo dos Louvores (opcional):", color = PrimaryOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            OutlinedTextField(
                                value = louvoresResumo,
                                onValueChange = { louvoresResumo = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = PrimaryOrange,
                                    unfocusedBorderColor = Color.DarkGray
                                )
                            )

                            Text("Uniforme:", color = PrimaryOrange, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = uniforme,
                                onValueChange = { uniforme = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = PrimaryOrange,
                                    unfocusedBorderColor = Color.DarkGray
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (dirigente.isBlank() || vocal.isBlank() || musicos.isBlank() || mesario.isBlank()) {
                                        Toast.makeText(context, "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    
                                    val louvoresJson = if (praiseItems.isNotEmpty()) {
                                        com.google.gson.Gson().toJson(praiseItems.filter { (it.louvor ?: "").isNotBlank() })
                                    } else null

                                    viewModel.updateFullEscala(
                                        data = selectedDate,
                                        dirigente = dirigente,
                                        vocal = vocal,
                                        musicos = musicos,
                                        mesario = mesario,
                                        louvores = louvoresResumo,
                                        uniforme = uniforme,
                                        louvoresDetalhes = louvoresJson,
                                        onSuccess = { 
                                            Toast.makeText(context, "Escala atualizada com sucesso.", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("SALVAR ALTERAÇÕES", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
