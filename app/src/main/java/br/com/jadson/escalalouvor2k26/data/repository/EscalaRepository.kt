package br.com.jadson.escalalouvor2k26.data.repository

import android.util.Log
import br.com.jadson.escalalouvor2k26.data.api.RetrofitClient
import br.com.jadson.escalalouvor2k26.data.model.EscalaData
import br.com.jadson.escalalouvor2k26.data.model.Notificacao
import br.com.jadson.escalalouvor2k26.data.model.UpdateResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class EscalaRepository {
    private val apiSemaphore = Semaphore(2) // Limita a 2 chamadas simultâneas

    suspend fun fetchEscalaData(nome: String? = null, senha: String? = null): Result<EscalaData> = withContext(Dispatchers.IO) {
        apiSemaphore.withPermit {
            try {
                val response = RetrofitClient.instance.getEscalaData(nome = nome, senha = senha)
                Result.success(response)
            } catch (e: Exception) {
                Log.e("EscalaRepository", "Erro na leitura: ${e.localizedMessage}")
                Result.failure(e)
            }
        }
    }

    suspend fun updateEscala(
        nome: String,
        senha: String,
        data: String,
        campo: String,
        valor: String
    ): Result<UpdateResponse> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.instance.updateEscala(
                nome = nome,
                senha = senha,
                data = data,
                campo = campo,
                valor = valor
            )
            Result.success(response)
        } catch (e: Exception) {
            Log.e("EscalaRepository", "Erro na atualização: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun updateFullEscala(
        nome: String,
        senha: String,
        data: String,
        dirigente: String,
        vocal: String,
        musicos: String,
        mesario: String,
        louvores: String,
        uniforme: String,
        louvoresDetalhes: String? = null
    ): Result<UpdateResponse> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.instance.updateFullEscala(
                nome = nome,
                senha = senha,
                data = data,
                dirigente = dirigente,
                vocal = vocal,
                musicos = musicos,
                mesario = mesario,
                louvores = louvores,
                uniforme = uniforme,
                louvoresDetalhes = louvoresDetalhes
            )
            Result.success(response)
        } catch (e: Exception) {
            Log.e("EscalaRepository", "Erro na atualização completa: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun createEscala(
        nome: String,
        senha: String,
        data: String,
        dirigente: String,
        vocal: String,
        musicos: String,
        mesario: String,
        louvores: String,
        uniforme: String,
        louvoresDetalhes: String? = null
    ): Result<UpdateResponse> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.instance.createEscala(
                nome = nome,
                senha = senha,
                data = data,
                dirigente = dirigente,
                vocal = vocal,
                musicos = musicos,
                mesario = mesario,
                louvores = louvores,
                uniforme = uniforme,
                louvoresDetalhes = louvoresDetalhes
            )
            Result.success(response)
        } catch (e: Exception) {
            Log.e("EscalaRepository", "Erro na criação de escala: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun createSolicitacao(
        nome: String,
        senha: String,
        dataEscala: String,
        substituto: String,
        motivo: String
    ): Result<UpdateResponse> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.instance.createSolicitacao(
                nome = nome,
                senha = senha,
                dataEscala = dataEscala,
                substituto = substituto,
                motivo = motivo
            )
            Result.success(response)
        } catch (e: Exception) {
            Log.e("EscalaRepository", "Erro na criação de solicitação: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun processaSolicitacao(
        nome: String,
        senha: String,
        dataEscala: String,
        quemPediu: String,
        substituto: String,
        acao: String,
        motivoDecisao: String? = null
    ): Result<UpdateResponse> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.instance.processaSolicitacao(
                nome = nome,
                senha = senha,
                dataEscala = dataEscala,
                quemPediu = quemPediu,
                substituto = substituto,
                acao = acao,
                motivoDecisao = motivoDecisao
            )
            Result.success(response)
        } catch (e: Exception) {
            Log.e("EscalaRepository", "Erro ao processar solicitação: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun createRecado(
        nome: String,
        senha: String,
        titulo: String,
        mensagem: String,
        imagemUrl: String = "",
        imageBase64: String? = null
    ): Result<UpdateResponse> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.instance.createRecado(
                nome = nome,
                senha = senha,
                titulo = titulo,
                mensagem = mensagem,
                imagemUrl = imagemUrl,
                imageBase64 = imageBase64
            )
            Result.success(response)
        } catch (e: Exception) {
            Log.e("EscalaRepository", "Erro na criação de recado. Tipo: ${e.javaClass.simpleName}, Mensagem: ${e.message}")
            if (e is retrofit2.HttpException) {
                Log.e("EscalaRepository", "Código HTTP: ${e.code()}. Resposta: ${e.response()?.errorBody()?.string()}")
            }
            Result.failure(e)
        }
    }

    suspend fun updateRecado(
        nome: String,
        senha: String,
        id: String,
        titulo: String,
        mensagem: String,
        imagemUrl: String,
        ativo: String,
        imageBase64: String? = null
    ): Result<UpdateResponse> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.instance.updateRecado(
                nome = nome,
                senha = senha,
                id = id,
                titulo = titulo,
                mensagem = mensagem,
                imagemUrl = imagemUrl,
                ativo = ativo,
                imageBase64 = imageBase64
            )
            Result.success(response)
        } catch (e: Exception) {
            Log.e("EscalaRepository", "Erro na atualização de recado. Tipo: ${e.javaClass.simpleName}, Mensagem: ${e.message}")
            if (e is retrofit2.HttpException) {
                Log.e("EscalaRepository", "Código HTTP: ${e.code()}. Resposta: ${e.response()?.errorBody()?.string()}")
            }
            Result.failure(e)
        }
    }

    suspend fun deleteRecado(id: String): Result<UpdateResponse> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.instance.deleteRecado(id = id)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("EscalaRepository", "Erro na exclusão de recado: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun getNotificacoes(nome: String, senha: String): Result<List<Notificacao>> = withContext(Dispatchers.IO) {
        try {
            Log.d("NOTIF_DEBUG", "Buscando notificações para: $nome")
            val response = RetrofitClient.instance.getNotificacoes(nome = nome, senha = senha)
            Log.d("NOTIF_DEBUG", "Resposta recebida: sucesso=${response.sucesso}")
            if (response.sucesso) {
                Log.d("NOTIF_DEBUG", "Quantidade recebida: ${response.notificacoes.size}")
                response.notificacoes.forEach { n ->
                    Log.d("NOTIF_DEBUG", "Notificação recebida: id=${n.id}, destinatario=${n.destinatario}, titulo=${n.titulo}, lida=${n.lida}")
                }
                Result.success(response.notificacoes)
            } else {
                Log.w("NOTIF_DEBUG", "Falha no backend: ${response.mensagem}")
                Result.failure(Exception(response.mensagem))
            }
        } catch (e: Exception) {
            Log.e("NOTIF_DEBUG", "Erro na chamada Retrofit: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun marcarComoLida(id: String, nome: String, senha: String): Result<UpdateResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("NOTIFICACAO", "Enviando para servidor: ID=$id, Usuario=$nome")
            val response = RetrofitClient.instance.marcarComoLida(id = id, nome = nome, senha = senha)
            Log.d("NOTIFICACAO", "Resposta do servidor: sucesso=${response.sucesso}, mensagem=${response.mensagem}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e("NOTIFICACAO", "ERRO AO MARCAR COMO LIDA: ID=$id, Erro=${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun atualizarTokenFcm(nome: String, senha: String, token: String): Result<UpdateResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("FCM_TOKEN", "Enviando token para servidor: Usuario=$nome")
            val response = RetrofitClient.instance.atualizarTokenFcm(nome = nome, senha = senha, token = token)
            Log.d("FCM_TOKEN", "Resposta do servidor: sucesso=${response.sucesso}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e("FCM_TOKEN", "ERRO AO ATUALIZAR TOKEN FCM: Erro=${e.localizedMessage}")
            Result.failure(e)
        }
    }
}
