package br.com.jadson.escalalouvor2k26.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.jadson.escalalouvor2k26.data.model.Escala
import br.com.jadson.escalalouvor2k26.data.model.Integrante
import br.com.jadson.escalalouvor2k26.data.model.LouvorItem
import br.com.jadson.escalalouvor2k26.ui.theme.PrimaryOrange
import br.com.jadson.escalalouvor2k26.ui.theme.SurfaceDark
import br.com.jadson.escalalouvor2k26.ui.viewmodel.EscalaViewModel
import br.com.jadson.escalalouvor2k26.ui.viewmodel.UiState

@Composable
fun NextEscalaDialog(
    escala: Escala, 
    myName: String?, 
    viewModel: EscalaViewModel? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel?.uiState?.collectAsState() ?: remember { mutableStateOf(null) }
    val currentUser by viewModel?.currentUser?.collectAsState() ?: remember { mutableStateOf(null) }
    
    val integrantes = if (uiState is UiState.Success) (uiState as UiState.Success).data.integrantes else emptyList()
    
    // Normalização rigorosa e limpeza de caracteres não imprimíveis (Senior Fix)
    val cleanDataEscala = escala.data.trim().replace(Regex("[^0-9/]"), "")
    
    val allLinks = (uiState as? UiState.Success)?.data?.linkLouvores ?: emptyList()
    val detailedPraises = allLinks.filter { 
        // Correção Final: O Logcat provou que a data vem no campo 'data' e não em 'dataEscala'
        val rawData = if (!it.data.isNullOrBlank()) it.data else (it.dataEscala ?: "")
        
        // Normalização Inteligente
        val cleanItemData = rawData.trim().replace(Regex("[^0-9/]"), "")
        val normalizedItemData = if (cleanItemData.length == 8 && cleanItemData.count { c -> c == '/' } == 2) {
            val parts = cleanItemData.split("/")
            "${parts[0]}/${parts[1]}/20${parts[2]}"
        } else cleanItemData
        
        normalizedItemData == cleanDataEscala 
    }.sortedBy { it.ordem ?: 0 }

    // Log para depuração sênior (Ajustado para o novo diagnóstico)
    LaunchedEffect(uiState, cleanDataEscala) {
        if (uiState is UiState.Success) {
            val successData = (uiState as UiState.Success).data
            android.util.Log.d("ESCALA_DEBUG", "--- Diagnóstico de Vínculo ---")
            android.util.Log.d("ESCALA_DEBUG", "Escala Alvo (Limpa): '$cleanDataEscala'")
            
            successData.linkLouvores.forEach { link ->
                val rawData = if (!link.data.isNullOrBlank()) link.data else (link.dataEscala ?: "")
                val cleanLinkData = rawData.trim().replace(Regex("[^0-9/]"), "")
                val normalizedLinkData = if (cleanLinkData.length == 8 && cleanLinkData.count { c -> c == '/' } == 2) {
                    val parts = cleanLinkData.split("/")
                    "${parts[0]}/${parts[1]}/20${parts[2]}"
                } else cleanLinkData
                
                val match = normalizedLinkData == cleanDataEscala
                android.util.Log.d("ESCALA_DEBUG", "Item: '${link.louvor}' | Data Original: '$rawData' | Data Normalizada: '$normalizedLinkData' | Match: $match")
            }
        }
    }
    
    val isLider = currentUser?.funcao?.uppercase()?.contains("LIDER") == true
    val isDirigente = currentUser?.funcao?.uppercase()?.contains("DIRIGENTE") == true
    
    var showEditDialog by remember { mutableStateOf(false) }
    var showPraiseEditDialog by remember { mutableStateOf(false) }
    var fieldToEdit by remember { mutableStateOf("") }
    var initialValue by remember { mutableStateOf("") }

    var showSolicitarTroca by remember { mutableStateOf(false) }

    if (showEditDialog) {
        EditFieldDialog(
            title = "Editar ${fieldToEdit.replaceFirstChar { it.uppercase() }}",
            initialValue = initialValue,
            onDismiss = { showEditDialog = false },
            onSave = { novoValor ->
                viewModel?.updateEscalaField(
                    data = escala.data,
                    campo = fieldToEdit.lowercase(),
                    novoValor = novoValor,
                    onSuccess = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        showEditDialog = false
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    if (showPraiseEditDialog) {
        EditPraisesDialog(
            escalaData = escala.data,
            currentPraises = detailedPraises,
            legacyPraises = escala.louvores,
            onDismiss = { showPraiseEditDialog = false },
            onSave = { list ->
                val json = com.google.gson.Gson().toJson(list)
                val summary = list.filter { (it.louvor ?: "").isNotBlank() }.joinToString(", ") { it.louvor ?: "" }
                
                // Feedback imediato de carregamento
                Toast.makeText(context, "Atualizando links...", Toast.LENGTH_SHORT).show()
                
                viewModel?.updateDetailedPraises(
                    data = escala.data,
                    resumo = summary,
                    detalhesJson = json,
                    onSuccess = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        showPraiseEditDialog = false
                        // Forçamos o fechamento do diálogo de detalhes para ver a atualização na lista/card
                        onDismiss()
                    },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                )
            }
        )
    }

    if (showSolicitarTroca && currentUser != null) {
        SolicitacaoTrocaDialog(
            escala = escala,
            currentUser = currentUser!!,
            integrantes = integrantes,
            onDismiss = { showSolicitarTroca = false },
            onConfirm = { substituto, motivo ->
                viewModel?.createSolicitacao(
                    dataEscala = escala.data,
                    substituto = substituto,
                    motivo = motivo,
                    onSuccess = {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        showSolicitarTroca = false
                        onDismiss()
                    },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                )
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Text(text = "Detalhes da Escala", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = escala.data, style = MaterialTheme.typography.titleMedium, color = PrimaryOrange)
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.DarkGray)

                Column(
                    modifier = Modifier.heightIn(max = 550.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val myRole = getMyRole(escala, myName)

                    // PRIORIDADE: LOUVORES Detalhados (links do YouTube)
                    PraiseDisplaySection(
                        detailedPraises = detailedPraises, 
                        legacyPraises = escala.louvores,
                        hasEditPermission = isLider || isDirigente,
                        onEdit = { showPraiseEditDialog = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.DarkGray.copy(alpha = 0.5f))
                    
                    // DIRIGENTE (Líder pode editar)
                    InfoRowEditable(label = "Dirigente", value = escala.dirigente, hasEditPermission = isLider, onEdit = { fieldToEdit = "dirigente"; initialValue = escala.dirigente; showEditDialog = true })
                    
                    // VOCAL (Líder pode editar)
                    InfoRowEditable(label = "Vocal", value = escala.vocal, hasEditPermission = isLider, onEdit = { fieldToEdit = "vocal"; initialValue = escala.vocal; showEditDialog = true })
                    
                    // MÚSICOS (Líder pode editar)
                    val musicosComInstrumento = formatMusiciansWithInstrument(escala.musicos, integrantes)
                    InfoRowEditable(label = "Músicos", value = musicosComInstrumento, hasEditPermission = isLider, onEdit = { fieldToEdit = "musicos"; initialValue = escala.musicos; showEditDialog = true })
                    
                    // MESÁRIO (Líder pode editar)
                    InfoRowEditable(label = "Mesário", value = escala.mesario, hasEditPermission = isLider, onEdit = { fieldToEdit = "mesario"; initialValue = escala.mesario; showEditDialog = true })

                    // UNIFORME (Líder ou Dirigente pode editar)
                    InfoRowEditable(label = "Uniforme", value = escala.uniforme, hasEditPermission = isLider || isDirigente, onEdit = { fieldToEdit = "uniforme"; initialValue = escala.uniforme; showEditDialog = true })
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (getMyRole(escala, currentUser?.nome) != null) {
                    Button(
                        onClick = { showSolicitarTroca = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = PrimaryOrange)
                        Spacer(Modifier.width(8.dp))
                        Text("Solicitar Troca", color = PrimaryOrange, fontWeight = FontWeight.Bold)
                    }
                }

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange), shape = RoundedCornerShape(16.dp)) {
                    Text("Fechar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PraiseDisplaySection(
    detailedPraises: List<LouvorItem>, 
    legacyPraises: String,
    hasEditPermission: Boolean = false,
    onEdit: () -> Unit = {}
) {
    val context = LocalContext.current
    
    Column(modifier = Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val allLinks = (detailedPraises + emptyList<LouvorItem>()) // Apenas para garantir escopo se necessário, mas passaremos como parâmetro se precisar de debug total

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Louvores", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            if (hasEditPermission) {
                TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = PrimaryOrange)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar", style = MaterialTheme.typography.labelSmall, color = PrimaryOrange)
                }
            }
        }
        
        // Depuração: Se detailedPraises estiver vazio, mostra um aviso visual (Senior Debug)
        if (detailedPraises.isNotEmpty()) {
            detailedPraises.forEach { item ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    val nomeLouvor = item.louvor ?: "Sem nome"
                    val ordemLouvor = item.ordem ?: 0
                    Text(text = "$ordemLouvor. $nomeLouvor", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    
                    val youtubeLink = (item.linkYoutube ?: "").trim()
                    if (youtubeLink.isNotBlank()) {
                        TextButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeLink))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro ao abrir link.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ver no YouTube", color = PrimaryOrange, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        } else {
            if (legacyPraises.isNotBlank()) {
                Text(text = legacyPraises, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(text = "Nenhum louvor cadastrado", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SolicitacaoTrocaDialog(
    escala: Escala,
    currentUser: Integrante,
    integrantes: List<Integrante>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val context = LocalContext.current
    var substituto by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    val myRole = getMyRole(escala, currentUser.nome) ?: ""
    val myInstrument = getMyInstrument(escala, currentUser.nome, integrantes) ?: ""

    // Filtrar integrantes compatíveis
    val options = integrantes.filter { it.nome != currentUser.nome }.filter { integrante ->
        when (myRole) {
            "Dirigente" -> integrante.funcao.contains("Dirigente", ignoreCase = true) || integrante.funcao.contains("Lider", ignoreCase = true)
            "Vocal" -> integrante.funcao.contains("Vocal", ignoreCase = true) || integrante.funcao.contains("Integrante", ignoreCase = true)
            "Músico" -> {
                if (myInstrument.isNotBlank()) {
                    integrante.instrumento.contains(myInstrument, ignoreCase = true) || integrante.funcao.contains("Musico", ignoreCase = true)
                } else {
                    integrante.funcao.contains("Musico", ignoreCase = true)
                }
            }
            "Mesário" -> integrante.funcao.contains("Mesário", ignoreCase = true) || integrante.funcao.contains("Mesario", ignoreCase = true)
            else -> true
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("Solicitar Troca", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Data: ${escala.data}", color = PrimaryOrange)
                Text("Sua função: $myRole", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                if (myInstrument.isNotBlank()) {
                    Text("Instrumento: $myInstrument", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Substituto:", color = PrimaryOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                SingleSelectionField(label = "Substituto", value = substituto, options = options) { substituto = it }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Motivo:", color = PrimaryOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    placeholder = { Text("Informe o motivo...", color = Color.DarkGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = PrimaryOrange, unfocusedBorderColor = Color.Gray)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (substituto.isBlank()) {
                                Toast.makeText(context, "Selecione um substituto.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (motivo.isBlank()) {
                                Toast.makeText(context, "Informe o motivo da solicitação.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            // Verificar disponibilidade do substituto
                            if (getMyRole(escala, substituto) != null) {
                                Toast.makeText(context, "Este integrante já está escalado nesta data.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            onConfirm(substituto, motivo)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        enabled = substituto.isNotBlank() && motivo.isNotBlank()
                    ) {
                        Text("Enviar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun formatMusiciansWithInstrument(musicos: String?, integrantes: List<Integrante>): String {
    if (musicos.isNullOrBlank()) return ""
    return musicos.split(", ").map { nome ->
        val integrante = integrantes.find { it.nome.equals(nome, ignoreCase = true) }
        if (integrante != null && integrante.instrumento.isNotBlank()) {
            "$nome — ${integrante.instrumento}"
        } else {
            nome
        }
    }.joinToString("\n• ", prefix = "• ")
}

@Composable
fun InfoRowEditable(label: String, value: String, hasEditPermission: Boolean, onEdit: () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            if (hasEditPermission) {
                TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = PrimaryOrange)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar", style = MaterialTheme.typography.labelSmall, color = PrimaryOrange)
                }
            }
        }
        Text(text = if (value.isBlank()) "Não definido" else value, style = MaterialTheme.typography.bodyLarge, color = Color.White)
    }
}

@Composable
fun EditFieldDialog(title: String, initialValue: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initialValue) }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark), modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = PrimaryOrange, unfocusedBorderColor = Color.Gray, cursorColor = PrimaryOrange))
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(text) }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)) {
                        Text("Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditPraisesDialog(
    escalaData: String,
    currentPraises: List<LouvorItem>,
    legacyPraises: String,
    onDismiss: () -> Unit,
    onSave: (List<LouvorItem>) -> Unit
) {
    // Se a lista detalhada estiver vazia, tenta criar itens a partir do texto legado
    val initialList = if (currentPraises.isEmpty() && legacyPraises.isNotBlank()) {
        legacyPraises.split(",").mapIndexed { index, s -> 
            LouvorItem(dataEscala = escalaData.trim(), ordem = index + 1, louvor = s.trim()) 
        }
    } else currentPraises

    var tempPraises by remember { mutableStateOf(initialList) }

    // Ao salvar, garante que todos os itens tenham a data correta (sem espaços extras)
    val onSaveWithData = { list: List<LouvorItem> ->
        val updatedList = list.map { it.copy(dataEscala = escalaData.trim()) }
        onSave(updatedList)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).heightIn(max = 500.dp).verticalScroll(rememberScrollState())) {
                Text("Gerenciar Louvores", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text(escalaData, color = PrimaryOrange, style = MaterialTheme.typography.labelSmall)
                
                Spacer(modifier = Modifier.height(16.dp))

                PraiseListEditor(
                    praises = tempPraises,
                    isAllowedToEdit = true,
                    onPraisesChanged = { tempPraises = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("CANCELAR", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSaveWithData(tempPraises) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SALVAR LINKS", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRowShared(label: String, value: String, isHighlighted: Boolean) {
    if (value.isNotBlank()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = if (isHighlighted) PrimaryOrange else Color.Gray)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, color = if (isHighlighted) PrimaryOrange else Color.White, fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

fun getMyRole(escala: Escala, myName: String?): String? {
    if (myName == null) return null
    val name = myName.trim().lowercase()
    return when {
        escala.dirigente.lowercase().contains(name) -> "Dirigente"
        escala.vocal.lowercase().contains(name) -> "Vocal"
        escala.musicos.lowercase().contains(name) -> "Músico"
        escala.mesario.lowercase().contains(name) -> "Mesário"
        else -> null
    }
}

fun getMyInstrument(escala: Escala, myName: String?, integrantes: List<Integrante>): String? {
    if (myName == null) return null
    val name = myName.trim().lowercase()
    
    // Se for músico, tentar achar o instrumento na string de músicos ou no perfil
    if (escala.musicos.lowercase().contains(name)) {
        // Exemplo de formato: "Jadson — Violino, Pedro — Teclado"
        val part = escala.musicos.split(", ").find { it.lowercase().contains(name) }
        if (part?.contains(" — ") == true) {
            return part.split(" — ").getOrNull(1)
        }
        // Se não achou na escala, pega do integrante
        return integrantes.find { it.nome.equals(myName, ignoreCase = true) }?.instrumento
    }
    return null
}
