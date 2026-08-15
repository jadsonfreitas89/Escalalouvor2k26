package br.com.jadson.escalalouvor2k26.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.jadson.escalalouvor2k26.data.model.LouvorItem
import br.com.jadson.escalalouvor2k26.ui.theme.PrimaryOrange
import br.com.jadson.escalalouvor2k26.ui.theme.SurfaceDark

@Composable
fun PraiseListEditor(
    praises: List<LouvorItem>,
    isAllowedToEdit: Boolean = true,
    onPraisesChanged: (List<LouvorItem>) -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LOUVORES INDIVIDUAIS", color = PrimaryOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            
            if (isAllowedToEdit) {
                TextButton(onClick = {
                    val newOrder = if (praises.isEmpty()) 1 else (praises.maxOf { it.ordem ?: 0 }) + 1
                    val newList = praises + LouvorItem(ordem = newOrder)
                    onPraisesChanged(newList)
                }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryOrange)
                    Text("ADICIONAR", color = PrimaryOrange)
                }
            }
        }

        if (praises.isEmpty()) {
            Text("Nenhum louvor individual cadastrado.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }

        praises.forEachIndexed { index, item ->
            PraiseItemCard(
                item = item,
                isEditable = isAllowedToEdit,
                onUpdate = { updated ->
                    val newList = praises.toMutableList().apply { this[index] = updated }
                    onPraisesChanged(newList)
                },
                onRemove = {
                    val newList = praises.toMutableList().apply { removeAt(index) }
                    // Reordenar
                    val reorderedList = newList.mapIndexed { i, p -> p.copy(ordem = i + 1) }
                    onPraisesChanged(reorderedList)
                },
                onSearchYouTube = { name ->
                    val query = Uri.encode(name)
                    val url = "https://www.youtube.com/results?search_query=$query"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
            )
        }
    }
}

fun isValidYouTubeLink(url: String): Boolean {
    if (url.isBlank()) return true
    val pattern = "^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be|m\\.youtube\\.com)/.+$"
    return url.matches(pattern.toRegex())
}

@Composable
fun PraiseItemCard(
    item: LouvorItem,
    isEditable: Boolean,
    onUpdate: (LouvorItem) -> Unit,
    onRemove: () -> Unit,
    onSearchYouTube: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = PrimaryOrange,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text((item.ordem ?: 0).toString(), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Text("LOUVOR ${item.ordem ?: 0}", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                
                if (isEditable) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remover", tint = Color.Gray)
                    }
                }
            }

            if (isEditable) {
                OutlinedTextField(
                    value = item.louvor ?: "",
                    onValueChange = { onUpdate(item.copy(louvor = it)) },
                    label = { Text("Nome do Louvor") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryOrange,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = item.linkYoutube ?: "",
                        onValueChange = { onUpdate(item.copy(linkYoutube = it)) },
                        label = { Text("Link YouTube (opcional)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        placeholder = { Text("https://...", color = Color.DarkGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryOrange,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    
                    IconButton(
                        onClick = { if ((item.louvor ?: "").isNotBlank()) onSearchYouTube(item.louvor ?: "") },
                        modifier = Modifier
                            .background(PrimaryOrange, RoundedCornerShape(8.dp))
                            .size(48.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Pesquisar no YouTube", tint = Color.Black)
                    }
                }
            } else {
                // Read-only view
                Text(text = item.louvor ?: "", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                
                if ((item.linkYoutube ?: "").isNotBlank()) {
                    val context = LocalContext.current
                    TextButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.linkYoutube ?: ""))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro ao abrir link.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PrimaryOrange)
                        Spacer(Modifier.width(4.dp))
                        Text("Assistir no YouTube", color = PrimaryOrange)
                    }
                }
            }
        }
    }
}
