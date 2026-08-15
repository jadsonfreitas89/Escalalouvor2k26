package br.com.jadson.escalalouvor2k26.data.api

import br.com.jadson.escalalouvor2k26.data.model.EscalaData
import br.com.jadson.escalalouvor2k26.data.model.NotificacaoResponse
import br.com.jadson.escalalouvor2k26.data.model.UpdateResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface EscalaApiService {
    @GET("exec")
    suspend fun getEscalaData(
        @Query("action") action: String = "getEscalaData",
        @Query("nome") nome: String? = null,
        @Query("senha") senha: String? = null
    ): EscalaData

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
        @Query("uniforme") uniforme: String,
        @Query("louvores_detalhes") louvoresDetalhes: String? = null
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
        @Query("uniforme") uniforme: String,
        @Query("louvores_detalhes") louvoresDetalhes: String? = null
    ): UpdateResponse

    @GET("exec")
    suspend fun createSolicitacao(
        @Query("action") action: String = "createSolicitacao",
        @Query("nome") nome: String,
        @Query("senha") senha: String,
        @Query("dataEscala") dataEscala: String,
        @Query("substituto") substituto: String,
        @Query("motivo") motivo: String
    ): UpdateResponse

    @GET("exec")
    suspend fun processaSolicitacao(
        @Query("action") action: String = "processaSolicitacao",
        @Query("nome") nome: String,
        @Query("senha") senha: String,
        @Query("dataEscala") dataEscala: String,
        @Query("quemPediu") quemPediu: String,
        @Query("substituto") substituto: String,
        @Query("acao") acao: String,
        @Query("motivoDecisao") motivoDecisao: String? = null
    ): UpdateResponse

    @FormUrlEncoded
    @POST("exec")
    suspend fun createRecado(
        @Field("action") action: String = "createRecado",
        @Field("nome") nome: String,
        @Field("senha") senha: String,
        @Field("titulo") titulo: String,
        @Field("mensagem") mensagem: String,
        @Field("imagemUrl") imagemUrl: String,
        @Field("imageBase64") imageBase64: String? = null
    ): UpdateResponse

    @FormUrlEncoded
    @POST("exec")
    suspend fun updateRecado(
        @Field("action") action: String = "updateRecado",
        @Field("nome") nome: String,
        @Field("senha") senha: String,
        @Field("id") id: String,
        @Field("titulo") titulo: String,
        @Field("mensagem") mensagem: String,
        @Field("imagemUrl") imagemUrl: String,
        @Field("ativo") ativo: String,
        @Field("imageBase64") imageBase64: String? = null
    ): UpdateResponse

    @GET("exec")
    suspend fun deleteRecado(
        @Query("action") action: String = "deleteRecado",
        @Query("id") id: String
    ): UpdateResponse

    @GET("exec")
    suspend fun getNotificacoes(
        @Query("action") action: String = "getNotificacoes",
        @Query("nome") nome: String,
        @Query("senha") senha: String
    ): NotificacaoResponse

    @GET("exec")
    suspend fun marcarComoLida(
        @Query("action") action: String = "marcarNotificacaoComoLida",
        @Query("id") id: String,
        @Query("nome") nome: String,
        @Query("senha") senha: String
    ): UpdateResponse

    @GET("exec")
    suspend fun atualizarTokenFcm(
        @Query("action") action: String = "atualizarTokenFcm",
        @Query("nome") nome: String,
        @Query("senha") senha: String,
        @Query("token") token: String
    ): UpdateResponse
}
