package br.com.jadson.escalalouvor2k26.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.jadson.escalalouvor2k26.data.model.Recado
import br.com.jadson.escalalouvor2k26.ui.components.ErrorScreen
import br.com.jadson.escalalouvor2k26.ui.components.LoadingScreen
import br.com.jadson.escalalouvor2k26.ui.navigation.Screen
import br.com.jadson.escalalouvor2k26.ui.theme.PrimaryOrange
import br.com.jadson.escalalouvor2k26.ui.theme.SurfaceDark
import br.com.jadson.escalalouvor2k26.ui.viewmodel.EscalaViewModel
import br.com.jadson.escalalouvor2k26.ui.viewmodel.UiState
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecadosScreen(navController: NavController, viewModel: EscalaViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLider = currentUser?.funcao?.uppercase()?.contains("LIDER") == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recados", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            /* Botão movido para dentro da lista para melhor organização */
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is UiState.Loading -> LoadingScreen()
                is UiState.Error -> ErrorScreen(state.message) { viewModel.loadData() }
                is UiState.Success -> {
                    val activeRecados = remember(state.data.recados) {
                        state.data.recados.filter { it.ativo.uppercase() == "SIM" }
                    }
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        if (isLider) {
                            item {
                                Button(
                                    onClick = { navController.navigate(Screen.CreateRecado.route) },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                    Spacer(Modifier.width(8.dp))
                                    Text("NOVO RECADO", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (activeRecados.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                    Text("Nenhum recado no momento.", color = Color.Gray)
                                }
                            }
                        } else {
                            items(activeRecados, key = { it.id }) { recado ->
                                RecadoCard(
                                    recado = recado,
                                    isLider = isLider,
                                    onEditClick = {
                                        navController.navigate(Screen.EditRecado.createRoute(recado.id))
                                    },
                                    onImageClick = { id ->
                                        navController.navigate(Screen.ImageViewer.createRoute(id))
                                    }
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
fun RecadoCard(
    recado: Recado,
    isLider: Boolean = false,
    onEditClick: () -> Unit = {},
    onImageClick: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column {
            if (!recado.imagemUrl.isNullOrBlank()) {
                AsyncImage(
                    model = recado.imagemUrl,
                    contentDescription = "Tocar para ampliar",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clickable { onImageClick(recado.id) },
                    contentScale = ContentScale.Crop,
                    error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image),
                    placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery)
                )
            }
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = recado.titulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange,
                        modifier = Modifier.weight(1f)
                    )
                    if (isLider) {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar Recado",
                                tint = PrimaryOrange
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = recado.dataCriacao, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = recado.mensagem, style = MaterialTheme.typography.bodyLarge, color = Color.White, lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2)
            }
        }
    }
}
