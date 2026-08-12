package br.com.jadson.escalalouvor2k26.data.repository

import android.util.Log
import br.com.jadson.escalalouvor2k26.data.api.RetrofitClient
import br.com.jadson.escalalouvor2k26.data.model.EscalaData
import br.com.jadson.escalalouvor2k26.data.model.UpdateResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EscalaRepository {
    suspend fun fetchEscalaData(): Result<EscalaData> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.instance.getEscalaData()
            Result.success(response)
        } catch (e: Exception) {
            Log.e("EscalaRepository", "Erro na leitura: ${e.localizedMessage}")
            Result.failure(e)
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
        uniforme: String
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
                uniforme = uniforme
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
        uniforme: String
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
                uniforme = uniforme
            )
            Result.success(response)
        } catch (e: Exception) {
            Log.e("EscalaRepository", "Erro na criação de escala: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun updateSolicitacao(
        nome: String,
        senha: String,
        dataEscala: String,
        quemPediu: String,
        status: String
    ): Result<UpdateResponse> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.instance.updateSolicitacao(
                nome = nome,
                senha = senha,
                dataEscala = dataEscala,
                quemPediu = quemPediu,
                status = status
            )
            Result.success(response)
        } catch (e: Exception) {
            Log.e("EscalaRepository", "Erro na atualização de solicitação: ${e.localizedMessage}")
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
            Log.e("EscalaRepository", "Erro na criação de recado: ${e.localizedMessage}")
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
            Log.e("EscalaRepository", "Erro na atualização de recado: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
}
