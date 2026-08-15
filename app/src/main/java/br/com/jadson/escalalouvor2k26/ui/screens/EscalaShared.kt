package br.com.jadson.escalalouvor2k26.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
    
    val isLider = currentUser?.funcao?.uppercase()?.contains("LIDER") == true
    val isDirigente = currentUser?.funcao?.uppercase()?.contains("DIRIGENTE") == true
    
    var showEditDialog by remember { mutableStateOf(false) }
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
                    modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val myRole = getMyRole(escala, myName)
                    
                    // DIRIGENTE (Líder pode editar)
                    InfoRowEditable(label = "Dirigente", value = escala.dirigente, hasEditPermission = isLider, onEdit = { fieldToEdit = "dirigente"; initialValue = escala.dirigente; showEditDialog = true })
                    
                    // VOCAL (Líder pode editar)
                    InfoRowEditable(label = "Vocal", value = escala.vocal, hasEditPermission = isLider, onEdit = { fieldToEdit = "vocal"; initialValue = escala.vocal; showEditDialog = true })
                    
                    // MÚSICOS (Líder pode editar)
                    val musicosComInstrumento = formatMusiciansWithInstrument(escala.musicos, integrantes)
                    InfoRowEditable(label = "Músicos", value = musicosComInstrumento, hasEditPermission = isLider, onEdit = { fieldToEdit = "musicos"; initialValue = escala.musicos; showEditDialog = true })
                    
                    // MESÁRIO (Líder pode editar)
                    InfoRowEditable(label = "Mesário", value = escala.mesario, hasEditPermission = isLider, onEdit = { fieldToEdit = "mesario"; initialValue = escala.mesario; showEditDialog = true })
                    
                    // LOUVORES (Líder ou Dirigente pode editar)
                    InfoRowEditable(label = "Louvores", value = escala.louvores, hasEditPermission = isLider || isDirigente, onEdit = { fieldToEdit = "louvores"; initialValue = escala.louvores; showEditDialog = true })

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
