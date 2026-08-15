package br.com.jadson.escalalouvor2k26.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.jadson.escalalouvor2k26.data.model.Solicitacao
import br.com.jadson.escalalouvor2k26.ui.components.ErrorScreen
import br.com.jadson.escalalouvor2k26.ui.components.LoadingScreen
import br.com.jadson.escalalouvor2k26.ui.theme.PrimaryOrange
import br.com.jadson.escalalouvor2k26.ui.theme.SurfaceDark
import br.com.jadson.escalalouvor2k26.ui.viewmodel.EscalaViewModel
import br.com.jadson.escalalouvor2k26.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSolicitacoesScreen(viewModel: EscalaViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerenciar Solicitações", fontWeight = FontWeight.Bold) },
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
                                AdminSolicitacaoCard(
                                    solicitacao = solicitacao,
                                    onAuthorize = {
                                        viewModel.processaSolicitacao(
                                            dataEscala = solicitacao.dataEscala,
                                            quemPediu = solicitacao.quemPediu,
                                            substituto = solicitacao.substituto,
                                            acao = "APROVAR",
                                            onSuccess = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                                            onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                        )
                                    },
                                    onRefuse = {
                                        viewModel.processaSolicitacao(
                                            dataEscala = solicitacao.dataEscala,
                                            quemPediu = solicitacao.quemPediu,
                                            substituto = solicitacao.substituto,
                                            acao = "RECUSAR",
                                            onSuccess = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                                            onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                        )
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
fun AdminSolicitacaoCard(
    solicitacao: Solicitacao,
    onAuthorize: () -> Unit,
    onRefuse: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = solicitacao.dataEscala, fontWeight = FontWeight.Bold, color = PrimaryOrange)
                StatusBadge(solicitacao.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Quem pediu: ${solicitacao.quemPediu}", color = Color.White)
            Text(text = "Substituto: ${solicitacao.substituto}", color = Color.White)
            Text(text = "Motivo: ${solicitacao.motivo}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            
            if (!solicitacao.motivoDecisao.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Resposta: ${solicitacao.motivoDecisao}", color = PrimaryOrange, style = MaterialTheme.typography.bodySmall)
            }

            if (!solicitacao.dataDecisao.isNullOrBlank()) {
                Text(text = "Decidido em: ${solicitacao.dataDecisao}", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
            
            if (solicitacao.status.uppercase() == "PENDENTE") {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onRefuse,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("RECUSAR", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onAuthorize,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("AUTORIZAR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
