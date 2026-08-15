package br.com.jadson.escalalouvor2k26.ui.viewmodel

import android.util.Log
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.first
import androidx.work.WorkInfo
import androidx.work.WorkManager
import br.com.jadson.escalalouvor2k26.data.model.EscalaData
import br.com.jadson.escalalouvor2k26.data.model.Integrante
import br.com.jadson.escalalouvor2k26.data.model.Notificacao
import br.com.jadson.escalalouvor2k26.data.repository.EscalaRepository
import br.com.jadson.escalalouvor2k26.data.session.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.google.gson.Gson

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val data: EscalaData) : UiState()
    data class Error(val message: String) : UiState()
}

class EscalaViewModel(
    private val repository: EscalaRepository = EscalaRepository(),
    private val sessionManager: SessionManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<Integrante?>(sessionManager?.getUser())
    val currentUser: StateFlow<Integrante?> = _currentUser.asStateFlow()

    private val _notificacoes = MutableStateFlow<List<Notificacao>>(emptyList())
    val notificacoes: StateFlow<List<Notificacao>> = _notificacoes.asStateFlow()

    private val gson = Gson()

    val pendingSolicitacoesCount: StateFlow<Int> = uiState.map { state ->
        if (state is UiState.Success) {
            state.data.solicitacoes.count { it.status.uppercase() == "PENDENTE" }
        } else {
            0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadData()
        carregarNotificacoes()
    }

    fun checkWorkerStatus(context: Context) {
        viewModelScope.launch {
            try {
                val workManager = androidx.work.WorkManager.getInstance(context)
                val workInfos = workManager.getWorkInfosForUniqueWork("EscalaNotificationWork").get()
                
                Log.d("NOTIF_SCHEDULE_DEBUG", "--- Diagnóstico WorkManager ---")
                if (workInfos.isNullOrEmpty()) {
                    Log.d("NOTIF_SCHEDULE_DEBUG", "Worker registrado? NÃO")
                } else {
                    Log.d("NOTIF_SCHEDULE_DEBUG", "Worker registrado? SIM (Total: ${workInfos.size})")
                    workInfos.forEach { info ->
                        Log.d("NOTIF_SCHEDULE_DEBUG", "Nome único: EscalaNotificationWork")
                        Log.d("NOTIF_SCHEDULE_DEBUG", "ID: ${info.id}")
                        Log.d("NOTIF_SCHEDULE_DEBUG", "Estado atual: ${info.state}")
                        Log.d("NOTIF_SCHEDULE_DEBUG", "Tentativas: ${info.runAttemptCount}")
                        Log.d("NOTIF_SCHEDULE_DEBUG", "Constraints: ${info.constraints}")
                        
                        val nextExecution = if (info.state == androidx.work.WorkInfo.State.ENQUEUED) "Agendado" else "N/A"
                        Log.d("NOTIF_SCHEDULE_DEBUG", "Próxima execução estimada: $nextExecution")
                    }
                }
                Log.d("NOTIF_SCHEDULE_DEBUG", "-------------------------------")
            } catch (e: Exception) {
                Log.e("NOTIF_SCHEDULE_DEBUG", "Erro ao verificar worker: ${e.message}")
            }
        }
    }

    fun runBackgroundTest(context: Context, onScheduled: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val workManager = WorkManager.getInstance(context)
                
                val constraints = androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()

                // Adicionamos um delay inicial de 10 segundos para dar tempo de fechar o app
                val testRequest = androidx.work.OneTimeWorkRequest.Builder(br.com.jadson.escalalouvor2k26.worker.NotificationWorker::class.java)
                    .setConstraints(constraints)
                    .setInitialDelay(15, java.util.concurrent.TimeUnit.SECONDS)
                    .addTag("NOTIF_BACKGROUND_TEST")
                    .build()

                workManager.enqueue(testRequest)

                Log.d("NOTIF_BACKGROUND_TEST", "--- DIAGNÓSTICO FASE 2 ---")
                Log.d("NOTIF_BACKGROUND_TEST", "TESTE ENFILEIRADO COM DELAY DE 15s")
                Log.d("NOTIF_BACKGROUND_TEST", "ID = ${testRequest.id}")

                val initialInfo = workManager.getWorkInfoById(testRequest.id).get()
                Log.d("NOTIF_BACKGROUND_TEST", "ESTADO INICIAL = ${initialInfo?.state}")

                onScheduled("Agendado! FECHE O APP AGORA (você tem 10 segundos).")
                
                workManager.getWorkInfoByIdLiveData(testRequest.id).asFlow().collect { info ->
                    info?.let {
                        Log.d("NOTIF_BACKGROUND_TEST", "STATUS ATUAL = ${it.state}")
                    }
                }
            } catch (e: Exception) {
                Log.e("NOTIF_BACKGROUND_TEST", "Erro ao agendar teste: ${e.message}")
            }
        }
    }

    fun carregarNotificacoes() {
        val user = _currentUser.value ?: return
        Log.d("NOTIF_DEBUG", "getNotificacoes INICIO")
        Log.d("NOTIF_DEBUG", "Usuário enviado: ${user.nome}")
        
        viewModelScope.launch {
            Log.d("NOTIF_DEBUG", "Buscando notificações...")
            repository.getNotificacoes(user.nome, user.senha)
                .onSuccess { list ->
                    // Filtrar apenas o destinatário atual (segurança extra no client) e não lidas
                    val filtradas = list.filter { 
                        it.destinatario.equals(user.nome, ignoreCase = true) && it.lida.uppercase() == "NAO"
                    }
                    Log.d("NOTIF_DEBUG", "Estado atualizado com ${filtradas.size} notificações")
                    _notificacoes.value = filtradas
                }
                .onFailure { error ->
                    Log.e("NOTIF_DEBUG", "Erro no ViewModel: ${error.message}")
                }
            Log.d("NOTIF_DEBUG", "getNotificacoes FIM")
        }
    }

    fun marcarNotificacaoComoLida(id: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val user = _currentUser.value ?: return
        Log.d("NOTIFICACAO", "Marcando como lida: ID=$id")
        viewModelScope.launch {
            repository.marcarComoLida(id, user.nome, user.senha)
                .onSuccess { response ->
                    if (response.sucesso) {
                        Log.d("NOTIFICACAO", "Status persistido com sucesso para ID=$id")
                        // Remove da lista local para atualizar a UI imediatamente
                        _notificacoes.value = _notificacoes.value.filter { it.id != id }
                        onResult(true, response.mensagem ?: "Notificação lida.")
                    } else {
                        Log.e("NOTIFICACAO", "Servidor retornou erro ao persistir ID=$id: ${response.mensagem}")
                        onResult(false, response.mensagem ?: "Erro ao marcar como lida.")
                    }
                }
                .onFailure { error ->
                    Log.e("NOTIFICACAO", "Falha na rede ao persistir status para ID=$id")
                    onResult(false, "Erro de rede: ${error.localizedMessage}")
                }
        }
    }

    fun login(nome: String, senha: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.fetchEscalaData(nome, senha)
                .onSuccess { data ->
                    val user = data.integrantes.find { 
                        it.nome.equals(nome.trim(), ignoreCase = true) && it.senha.trim() == senha.trim() 
                    }
                    if (user != null) {
                        sessionManager?.saveSession(user)
                        _currentUser.value = user
                        _uiState.value = UiState.Success(data)
                        carregarNotificacoes() // Carregar notificações imediatamente após login
                        onSuccess()
                    } else {
                        _uiState.value = UiState.Success(data)
                        onError("Nome ou senha incorretos.")
                    }
                }
                .onFailure { error ->
                    Log.e("EscalaViewModel", "Erro no login", error)
                    val message = when {
                        error is com.google.gson.JsonSyntaxException -> "Erro de formato nos dados. Verifique se o script está publicado corretamente como 'Qualquer pessoa'."
                        error is java.net.SocketTimeoutException -> "Servidor demorou a responder. Tente novamente."
                        else -> "Falha na conexão: ${error.localizedMessage ?: "Erro desconhecido"}"
                    }
                    _uiState.value = UiState.Error(message)
                    onError(message)
                }
        }
    }

    fun logout(onLogout: () -> Unit) {
        sessionManager?.logout()
        _currentUser.value = null
        onLogout()
    }

    fun loadData() {
        viewModelScope.launch {
            if (_uiState.value !is UiState.Success) {
                _uiState.value = UiState.Loading
            }
            
            val user = _currentUser.value
            repository.fetchEscalaData(user?.nome, user?.senha)
                .onSuccess { data ->
                    _uiState.value = UiState.Success(data)
                    sessionManager?.saveLastData(gson.toJson(data))
                    carregarNotificacoes() // Carregar notificações após carregar os dados principais
                }
                .onFailure { error ->
                    Log.e("EscalaViewModel", "Erro ao carregar dados", error)
                    if (_uiState.value !is UiState.Success) {
                        val message = when (error) {
                            is java.net.SocketTimeoutException -> "Tempo esgotado. Tente novamente."
                            is java.net.UnknownHostException -> "Verifique sua internet."
                            else -> "Erro ao carregar: ${error.localizedMessage ?: "Erro de rede"}"
                        }
                        _uiState.value = UiState.Error(message)
                    }
                }
        }
    }

    fun updateEscalaField(
        data: String,
        campo: String,
        novoValor: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null) {
            onError("Usuário não autenticado.")
            return
        }

        val isLider = user.funcao.uppercase().contains("LIDER")
        val isDirigente = user.funcao.uppercase().contains("DIRIGENTE")
        
        // LIDER pode editar tudo. DIRIGENTE pode editar louvores e uniforme.
        val hasPermission = when (campo.lowercase()) {
            "louvores", "uniforme" -> isLider || isDirigente
            else -> isLider
        }

        if (!hasPermission) {
            onError("Você não possui permissão para realizar esta alteração.")
            return
        }

        val startTime = System.currentTimeMillis()
        Log.d("PERF_DEBUG", "INICIO_OPERACAO: updateEscalaField ($campo -> $novoValor)")

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            Log.d("PERF_DEBUG", "ENVIO_REQUEST: Enviando atualização")
            repository.updateEscala(
                nome = user.nome,
                senha = user.senha,
                data = data,
                campo = campo,
                valor = novoValor
            ).onSuccess { response ->
                Log.d("PERF_DEBUG", "RESPOSTA_RECEBIDA: Sucesso=${response.sucesso} em ${System.currentTimeMillis() - startTime}ms")
                if (response.sucesso) {
                    onSuccess(response.mensagem ?: "Campo atualizado com sucesso.")
                    loadData()
                } else {
                    _uiState.value = UiState.Idle
                    onError(response.mensagem ?: "Erro ao atualizar campo.")
                }
                Log.d("PERF_DEBUG", "FIM_OPERACAO: Concluído em ${System.currentTimeMillis() - startTime}ms")
            }.onFailure {
                Log.e("PERF_DEBUG", "ERRO_OPERACAO: Falha após ${System.currentTimeMillis() - startTime}ms")
                _uiState.value = UiState.Idle
                onError("Erro de conexão ao tentar salvar.")
            }
        }
    }

    fun updateFullEscala(
        data: String,
        dirigente: String,
        vocal: String,
        musicos: String,
        mesario: String,
        louvores: String,
        uniforme: String,
        louvoresDetalhes: String? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null || !user.funcao.uppercase().contains("LIDER")) {
            onError("Somente o Líder pode editar a escala completa.")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.updateFullEscala(
                nome = user.nome,
                senha = user.senha,
                data = data,
                dirigente = dirigente,
                vocal = vocal,
                musicos = musicos,
                mesario = mesario,
                louvores = louvores,
                uniforme = uniforme,
                louvoresDetalhes = louvoresDetalhes
            ).onSuccess { response ->
                if (response.sucesso) {
                    onSuccess(response.mensagem ?: "Escala atualizada com sucesso.")
                    loadData()
                } else {
                    _uiState.value = UiState.Idle
                    onError(response.mensagem ?: "Erro ao atualizar escala.")
                }
            }.onFailure {
                _uiState.value = UiState.Idle
                onError("Erro de conexão ao tentar salvar escala.")
            }
        }
    }

    fun createEscala(
        data: String,
        dirigente: String,
        vocal: String,
        musicos: String,
        mesario: String,
        louvores: String,
        uniforme: String,
        louvoresDetalhes: String? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null || !user.funcao.uppercase().contains("LIDER")) {
            onError("Permissão negada.")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.createEscala(
                nome = user.nome,
                senha = user.senha,
                data = data,
                dirigente = dirigente,
                vocal = vocal,
                musicos = musicos,
                mesario = mesario,
                louvores = louvores,
                uniforme = uniforme,
                louvoresDetalhes = louvoresDetalhes
            ).onSuccess { response ->
                if (response.sucesso) {
                    onSuccess(response.mensagem ?: "Escala criada com sucesso.")
                    loadData()
                } else {
                    _uiState.value = UiState.Idle
                    onError(response.mensagem ?: "Erro ao criar escala.")
                }
            }.onFailure {
                _uiState.value = UiState.Idle
                onError("Erro de rede ao criar escala.")
            }
        }
    }

    fun createSolicitacao(
        dataEscala: String,
        substituto: String,
        motivo: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value ?: return onError("Usuário não autenticado.")
        val startTime = System.currentTimeMillis()
        Log.d("PERF_DEBUG", "INICIO_OPERACAO: createSolicitacao para $dataEscala")

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            Log.d("PERF_DEBUG", "ENVIO_REQUEST: Enviando solicitação")
            repository.createSolicitacao(
                nome = user.nome,
                senha = user.senha,
                dataEscala = dataEscala,
                substituto = substituto,
                motivo = motivo
            ).onSuccess { response ->
                Log.d("PERF_DEBUG", "RESPOSTA_RECEBIDA: Sucesso=${response.sucesso} em ${System.currentTimeMillis() - startTime}ms")
                if (response.sucesso) {
                    onSuccess(response.mensagem ?: "Solicitação enviada.")
                    loadData()
                } else {
                    _uiState.value = UiState.Idle
                    onError(response.mensagem ?: "Erro ao enviar solicitação.")
                }
                Log.d("PERF_DEBUG", "FIM_OPERACAO: Concluído em ${System.currentTimeMillis() - startTime}ms")
            }.onFailure {
                Log.e("PERF_DEBUG", "ERRO_OPERACAO: Falha após ${System.currentTimeMillis() - startTime}ms")
                _uiState.value = UiState.Idle
                onError("Erro de rede.")
            }
        }
    }

    fun processaSolicitacao(
        dataEscala: String,
        quemPediu: String,
        substituto: String,
        acao: String, // APROVAR, RECUSAR, CANCELAR
        motivoDecisao: String? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value ?: return onError("Usuário não autenticado.")

        // Validações de permissão no ViewModel (também feitas no Backend)
        if ((acao == "APROVAR" || acao == "RECUSAR") && !user.funcao.uppercase().contains("LIDER")) {
            onError("Somente o Líder pode realizar esta ação.")
            return
        }
        
        if (acao == "CANCELAR" && !user.nome.equals(quemPediu, ignoreCase = true)) {
            onError("Somente o solicitante pode cancelar.")
            return
        }

        val startTime = System.currentTimeMillis()
        Log.d("PERF_DEBUG", "INICIO_OPERACAO: processaSolicitacao ($acao) para $dataEscala")

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            Log.d("PERF_DEBUG", "ENVIO_REQUEST: Enviando processamento")
            repository.processaSolicitacao(
                nome = user.nome,
                senha = user.senha,
                dataEscala = dataEscala,
                quemPediu = quemPediu,
                substituto = substituto,
                acao = acao,
                motivoDecisao = motivoDecisao
            ).onSuccess { response ->
                Log.d("PERF_DEBUG", "RESPOSTA_RECEBIDA: Sucesso=${response.sucesso} em ${System.currentTimeMillis() - startTime}ms")
                if (response.sucesso) {
                    onSuccess(response.mensagem ?: "Operação realizada.")
                    loadData()
                } else {
                    _uiState.value = UiState.Idle
                    onError(response.mensagem ?: "Erro ao processar solicitação.")
                }
                Log.d("PERF_DEBUG", "FIM_OPERACAO: Concluído em ${System.currentTimeMillis() - startTime}ms")
            }.onFailure {
                Log.e("PERF_DEBUG", "ERRO_OPERACAO: Falha após ${System.currentTimeMillis() - startTime}ms")
                _uiState.value = UiState.Idle
                onError("Erro de rede.")
            }
        }
    }

    fun createRecado(
        titulo: String,
        mensagem: String,
        imagemUrl: String = "",
        imageBase64: String? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null || !user.funcao.uppercase().contains("LIDER")) {
            onError("Permissão negada.")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.createRecado(
                nome = user.nome,
                senha = user.senha,
                titulo = titulo,
                mensagem = mensagem,
                imagemUrl = imagemUrl,
                imageBase64 = imageBase64
            ).onSuccess { response ->
                if (response.sucesso) {
                    onSuccess(response.mensagem ?: "Recado publicado com sucesso.")
                    loadData()
                } else {
                    _uiState.value = UiState.Idle
                    onError(response.mensagem ?: "Erro ao publicar recado.")
                }
            }.onFailure { error ->
                Log.e("EscalaViewModel", "Erro ao publicar recado", error)
                _uiState.value = UiState.Idle
                val msg = when {
                    error is java.net.SocketException && error.message?.contains("Broken pipe") == true -> 
                        "Erro de conexão: Imagem muito grande ou falha no servidor."
                    error is java.net.SocketTimeoutException -> "Tempo esgotado ao enviar. Verifique sua internet."
                    else -> "Erro ao publicar recado: ${error.localizedMessage ?: "Erro de rede"}"
                }
                onError(msg)
            }
        }
    }

    fun updateRecado(
        id: String,
        titulo: String,
        mensagem: String,
        imagemUrl: String,
        ativo: String = "SIM",
        imageBase64: String? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null || !user.funcao.uppercase().contains("LIDER")) {
            onError("Permissão negada.")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.updateRecado(
                nome = user.nome,
                senha = user.senha,
                id = id,
                titulo = titulo,
                mensagem = mensagem,
                imagemUrl = imagemUrl,
                ativo = ativo,
                imageBase64 = imageBase64
            ).onSuccess { response ->
                if (response.sucesso) {
                    onSuccess(response.mensagem ?: "Recado atualizado com sucesso.")
                    loadData()
                } else {
                    _uiState.value = UiState.Idle
                    onError(response.mensagem ?: "Erro ao atualizar recado.")
                }
            }.onFailure { error ->
                Log.e("EscalaViewModel", "Erro ao atualizar recado", error)
                _uiState.value = UiState.Idle
                val msg = when {
                    error is java.net.SocketException && error.message?.contains("Broken pipe") == true -> 
                        "Erro de conexão ao atualizar: Imagem muito grande."
                    error is java.net.SocketTimeoutException -> "Tempo esgotado ao atualizar. Verifique sua internet."
                    else -> "Erro ao atualizar recado: ${error.localizedMessage ?: "Erro de rede"}"
                }
                onError(msg)
            }
        }
    }

    fun updateDetailedPraises(
        data: String,
        resumo: String,
        detalhesJson: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null) return
        
        val isLider = user.funcao.uppercase().contains("LIDER")
        val isDirigente = user.funcao.uppercase().contains("DIRIGENTE")

        if (!isLider && !isDirigente) {
            onError("Sem permissão para gerenciar louvores.")
            return
        }

        // Obtemos a escala atual para não sobrescrever outros campos com vazio
        val currentScale = (uiState.value as? UiState.Success)?.data?.escala?.find { it.data == data }
        if (currentScale == null) {
            onError("Dados da escala não encontrados localmente.")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.updateFullEscala(
                nome = user.nome,
                senha = user.senha,
                data = data.trim(),
                dirigente = currentScale.dirigente,
                vocal = currentScale.vocal,
                musicos = currentScale.musicos,
                mesario = currentScale.mesario,
                louvores = resumo,
                uniforme = currentScale.uniforme,
                louvoresDetalhes = detalhesJson
            ).onSuccess { response ->
                if (response.sucesso) {
                    // Sincronização Sênior: Limpa cache e recarrega tudo
                    sessionManager?.saveLastData("")
                    loadData()
                    onSuccess(response.mensagem ?: "Louvores e links atualizados!")
                } else {
                    _uiState.value = UiState.Idle
                    onError(response.mensagem ?: "Erro ao atualizar.")
                }
            }.onFailure {
                _uiState.value = UiState.Idle
                onError("Erro de conexão.")
            }
        }
    }

    fun deleteRecado(
        id: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null || !user.funcao.uppercase().contains("LIDER")) {
            onError("Permissão negada.")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.deleteRecado(id).onSuccess { response ->
                if (response.sucesso) {
                    onSuccess(response.mensagem ?: "Recado excluído com sucesso.")
                    loadData()
                } else {
                    _uiState.value = UiState.Idle
                    onError(response.mensagem ?: "Erro ao excluir recado.")
                }
            }.onFailure {
                _uiState.value = UiState.Idle
                onError("Erro de conexão ao excluir recado.")
            }
        }
    }
}
