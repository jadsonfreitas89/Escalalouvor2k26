package br.com.jadson.escalalouvor2k26.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange), shape = RoundedCornerShape(16.dp)) {
                    Text("Fechar", color = Color.Black, fontWeight = FontWeight.Bold)
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
