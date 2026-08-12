package br.com.jadson.escalalouvor2k26.data.api

import br.com.jadson.escalalouvor2k26.data.model.EscalaData
import br.com.jadson.escalalouvor2k26.data.model.UpdateResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface EscalaApiService {
    @GET("exec")
    suspend fun getEscalaData(): EscalaData

    @GET("exec")
    suspend fun updateEscala(
        @Query("action") action: String = "updateEscala",
        @Query("nome") nome: String,
        @Query("senha") senha: String,
        @Query("data") data: String,
        @Query("campo") campo: String,
        @Query("valor") valor: String
    ): UpdateResponse

    @GET("exec")
    suspend fun updateFullEscala(
        @Query("action") action: String = "updateFullEscala",
        @Query("nome") nome: String,
        @Query("senha") senha: String,
        @Query("data") data: String,
        @Query("dirigente") dirigente: String,
        @Query("vocal") vocal: String,
        @Query("musicos") musicos: String,
        @Query("mesario") mesario: String,
        @Query("louvores") louvores: String,
        @Query("uniforme") uniforme: String
    ): UpdateResponse

    @GET("exec")
    suspend fun createEscala(
        @Query("action") action: String = "createEscala",
        @Query("nome") nome: String,
        @Query("senha") senha: String,
        @Query("data") data: String,
        @Query("dirigente") dirigente: String,
        @Query("vocal") vocal: String,
        @Query("musicos") musicos: String,
        @Query("mesario") mesario: String,
        @Query("louvores") louvores: String,
        @Query("uniforme") uniforme: String
    ): UpdateResponse

    @GET("exec")
    suspend fun updateSolicitacao(
        @Query("action") action: String = "updateSolicitacao",
        @Query("nome") nome: String,
        @Query("senha") senha: String,
        @Query("dataEscala") dataEscala: String,
        @Query("quemPediu") quemPediu: String,
        @Query("status") status: String
    ): UpdateResponse

    @GET("exec")
    suspend fun createRecado(
        @Query("action") action: String = "createRecado",
        @Query("nome") nome: String,
        @Query("senha") senha: String,
        @Query("titulo") titulo: String,
        @Query("mensagem") mensagem: String,
        @Query("imagemUrl") imagemUrl: String,
        @Query("imageBase64") imageBase64: String? = null
    ): UpdateResponse

    @GET("exec")
    suspend fun updateRecado(
        @Query("action") action: String = "updateRecado",
        @Query("nome") nome: String,
        @Query("senha") senha: String,
        @Query("id") id: String,
        @Query("titulo") titulo: String,
        @Query("mensagem") mensagem: String,
        @Query("imagemUrl") imagemUrl: String,
        @Query("ativo") ativo: String,
        @Query("imageBase64") imageBase64: String? = null
    ): UpdateResponse
}
