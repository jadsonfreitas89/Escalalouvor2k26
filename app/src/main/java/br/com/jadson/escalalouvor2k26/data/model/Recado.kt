package br.com.jadson.escalalouvor2k26.data.model

import com.google.gson.annotations.SerializedName

data class Recado(
    val id: String,
    val titulo: String,
    val mensagem: String,
    @SerializedName("imagem_url") val imagemUrl: String?,
    val ativo: String,
    @SerializedName("data_criacao") val dataCriacao: String,
    @SerializedName("data_atualizacao") val dataAtualizacao: String
)
