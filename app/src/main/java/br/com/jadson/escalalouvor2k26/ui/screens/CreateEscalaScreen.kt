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
import br.com.jadson.escalalouvor2k26.ui.theme.PrimaryOrange
import br.com.jadson.escalalouvor2k26.ui.viewmodel.EscalaViewModel
import br.com.jadson.escalalouvor2k26.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEscalaScreen(navController: androidx.navigation.NavController, viewModel: EscalaViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    var data by remember { mutableStateOf("") }
    var dirigente by remember { mutableStateOf("") }
    var vocal by remember { mutableStateOf("") }
    var musicos by remember { mutableStateOf("") }
    var mesario by remember { mutableStateOf("") }
    var louvoresResumo by remember { mutableStateOf("") }
    var uniforme by remember { mutableStateOf("") }
    var praiseItems by remember { mutableStateOf(emptyList<LouvorItem>()) }

    val integrantes = if (uiState is UiState.Success) (uiState as UiState.Success).data.integrantes else emptyList()
    
    // Filtros por função (ignora acentos e case)
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
                title = { Text("Criar Nova Escala", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DateSelectorField("Data:", data) { data = it }

            SingleSelectionField("Dirigente:", dirigente, dirigentesOptions) { dirigente = it }

            MultiSelectionField("Vocal:", vocal, vocalOptions) { vocal = it }

            MultiSelectionField("Músicos:", musicos, musicosOptions) { musicos = it }

            SingleSelectionField("Mesário:", mesario, mesariosOptions) { mesario = it }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.DarkGray)
            
            PraiseListEditor(
                praises = praiseItems,
                isAllowedToEdit = canManageLinks,
                onPraisesChanged = { list ->
                    praiseItems = list
                    louvoresResumo = list.filter { (it.louvor ?: "").isNotBlank() }.joinToString(", ") { it.louvor ?: "" }
                }
            )

            Text("Resumo dos Louvores (opcional):", color = PrimaryOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(
                value = louvoresResumo,
                onValueChange = { louvoresResumo = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ex: Louvor 1, Louvor 2", color = Color.DarkGray) },
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
                placeholder = { Text("Ex: Camiseta Branca", color = Color.DarkGray) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PrimaryOrange,
                    unfocusedBorderColor = Color.DarkGray
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (data.isBlank()) {
                        Toast.makeText(context, "Selecione uma data.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (dirigente.isBlank()) {
                        Toast.makeText(context, "Selecione um dirigente.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    val louvoresJson = if (praiseItems.isNotEmpty()) {
                        com.google.gson.Gson().toJson(praiseItems.filter { (it.louvor ?: "").isNotBlank() })
                    } else null

                    viewModel.createEscala(
                        data = data,
                        dirigente = dirigente,
                        vocal = vocal,
                        musicos = musicos,
                        mesario = mesario,
                        louvores = louvoresResumo,
                        uniforme = uniforme,
                        louvoresDetalhes = louvoresJson,
                        onSuccess = {
                            Toast.makeText(context, "Escala criada com sucesso.", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("SALVAR ESCALA", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
