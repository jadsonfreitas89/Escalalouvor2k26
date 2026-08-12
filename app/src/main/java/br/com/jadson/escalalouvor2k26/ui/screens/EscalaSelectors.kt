package br.com.jadson.escalalouvor2k26.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.jadson.escalalouvor2k26.data.model.Integrante
import br.com.jadson.escalalouvor2k26.ui.theme.PrimaryOrange
import br.com.jadson.escalalouvor2k26.ui.theme.SurfaceDark
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectorField(label: String, value: String, onDateSelected: (String) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = PrimaryOrange, fontWeight = FontWeight.Bold)
        OutlinedCard(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.DarkGray))
        ) {
            Text(
                text = if (value.isEmpty()) "Selecionar data..." else value,
                modifier = Modifier.padding(16.dp),
                color = if (value.isEmpty()) Color.Gray else Color.White
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        onDateSelected(date.format(formatter))
                    }
                    showDatePicker = false
                }) { Text("CONFIRMAR", color = PrimaryOrange) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("CANCELAR", color = Color.Gray) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun SingleSelectionField(
    label: String,
    value: String,
    options: List<Integrante>,
    onSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = PrimaryOrange, fontWeight = FontWeight.Bold)
        OutlinedCard(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.DarkGray))
        ) {
            Text(
                text = if (value.isEmpty()) "Selecionar..." else value,
                modifier = Modifier.padding(16.dp),
                color = if (value.isEmpty()) Color.Gray else Color.White
            )
        }
    }

    if (showDialog) {
        SelectionDialog(
            title = "Selecionar $label",
            options = options,
            initialSelection = if (value.isEmpty()) emptySet() else setOf(value),
            isMultiple = false,
            onDismiss = { showDialog = false },
            onConfirm = { 
                onSelected(it.firstOrNull() ?: "")
                showDialog = false
            }
        )
    }
}

@Composable
fun MultiSelectionField(
    label: String,
    value: String,
    options: List<Integrante>,
    onSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val currentSelection = if (value.isEmpty()) emptySet() else value.split(", ").filter { it.isNotBlank() }.toSet()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = PrimaryOrange, fontWeight = FontWeight.Bold)
        OutlinedCard(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.DarkGray))
        ) {
            Text(
                text = if (value.isEmpty()) "Selecionar..." else value,
                modifier = Modifier.padding(16.dp),
                color = if (value.isEmpty()) Color.Gray else Color.White,
                maxLines = 2
            )
        }
    }

    if (showDialog) {
        SelectionDialog(
            title = "Selecionar $label",
            options = options,
            initialSelection = currentSelection,
            isMultiple = true,
            onDismiss = { showDialog = false },
            onConfirm = { 
                onSelected(it.joinToString(", "))
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionDialog(
    title: String,
    options: List<Integrante>,
    initialSelection: Set<String>,
    isMultiple: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedItems by remember { mutableStateOf(initialSelection) }

    val filteredOptions = options.filter { it.nome.contains(searchQuery, ignoreCase = true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).heightIn(max = 500.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar integrante...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryOrange,
                        unfocusedBorderColor = Color.DarkGray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (options.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Nenhum integrante cadastrado.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredOptions) { integrante ->
                            val isSelected = selectedItems.contains(integrante.nome)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isMultiple) {
                                            selectedItems = if (isSelected) {
                                                selectedItems - integrante.nome
                                            } else {
                                                selectedItems + integrante.nome
                                            }
                                        } else {
                                            selectedItems = setOf(integrante.nome)
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isMultiple) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = PrimaryOrange)
                                    )
                                } else {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryOrange)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(integrante.nome, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("CANCELAR", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selectedItems) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CONFIRMAR", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
