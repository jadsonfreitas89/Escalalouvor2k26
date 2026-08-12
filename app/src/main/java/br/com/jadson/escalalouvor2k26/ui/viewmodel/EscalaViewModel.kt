package br.com.jadson.escalalouvor2k26.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.jadson.escalalouvor2k26.data.model.EscalaData
import br.com.jadson.escalalouvor2k26.data.model.Integrante
import br.com.jadson.escalalouvor2k26.data.repository.EscalaRepository
import br.com.jadson.escalalouvor2k26.data.session.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    val pendingSolicitacoesCount: StateFlow<Int> = uiState.map { state ->
        if (state is UiState.Success) {
            state.data.solicitacoes.count { it.status.uppercase() == "PENDENTE" }
        } else {
            0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadData()
        startAutoRefresh()
    }

    fun login(nome: String, senha: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.fetchEscalaData()
                .onSuccess { data ->
                    val user = data.integrantes.find { 
                        it.nome.equals(nome.trim(), ignoreCase = true) && it.senha.trim() == senha.trim() 
                    }
                    if (user != null) {
                        sessionManager?.saveSession(user)
                        _currentUser.value = user
                        _uiState.value = UiState.Success(data)
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

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                fetchDataSilently()
            }
        }
    }

    private suspend fun fetchDataSilently() {
        repository.fetchEscalaData()
            .onSuccess { data ->
                _uiState.value = UiState.Success(data)
            }
    }

    fun loadData() {
        viewModelScope.launch {
            if (_uiState.value !is UiState.Success) {
                _uiState.value = UiState.Loading
            }
            
            repository.fetchEscalaData()
                .onSuccess { data ->
                    _uiState.value = UiState.Success(data)
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
        if (user == null || user.funcao.uppercase().contains("DIRIGENTE").not()) {
            onError("Você não possui permissão para realizar esta alteração.")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.updateEscala(
                nome = user.nome,
                senha = user.senha,
                data = data,
                campo = campo,
                valor = novoValor
            ).onSuccess { response ->
                if (response.sucesso) {
                    onSuccess(response.mensagem)
                    loadData()
                } else {
                    _uiState.value = UiState.Idle
                    onError(response.mensagem)
                }
            }.onFailure {
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
                uniforme = uniforme
            ).onSuccess { response ->
                if (response.sucesso) {
                    onSuccess(response.mensagem)
                    loadData()
                } else {
                    _uiState.value = UiState.Idle
                    onError(response.mensagem)
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
                uniforme = uniforme
            ).onSuccess { response ->
                if (response.sucesso) {
                    onSuccess(response.mensagem)
                    loadData()
                } else {
                    _uiState.value = UiState.Idle
                    onError(response.mensagem)
                }
            }.onFailure {
                _uiState.value = UiState.Idle
                onError("Erro de rede ao criar escala.")
            }
        }
    }

    fun updateSolicitacaoStatus(
        dataEscala: String,
        quemPediu: String,
        status: String,
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
            repository.updateSolicitacao(
                nome = user.nome,
                senha = user.senha,
                dataEscala = dataEscala,
                quemPediu = quemPediu,
                status = status
            ).onSuccess { response ->
                if (response.sucesso) {
                    onSuccess(response.mensagem)
                    loadData()
                } else {
                    _uiState.value = UiState.Idle
                    onError(response.mensagem)
                }
            }.onFailure {
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
                    onSuccess(response.mensagem)
                    loadData()
                } else {
                    _uiState.value = UiState.Idle
                    onError(response.mensagem)
                }
            }.onFailure {
                _uiState.value = UiState.Idle
                onError("Erro ao publicar recado.")
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
                    onSuccess(response.mensagem)
                    loadData()
                } else {
                    _uiState.value = UiState.Idle
                    onError(response.mensagem)
                }
            }.onFailure {
                _uiState.value = UiState.Idle
                onError("Erro ao atualizar recado.")
            }
        }
    }
}
