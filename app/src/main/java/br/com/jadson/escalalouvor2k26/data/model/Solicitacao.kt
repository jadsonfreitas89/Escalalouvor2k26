package br.com.jadson.escalalouvor2k26.data.model

import com.google.gson.annotations.SerializedName

data class Solicitacao(
    @SerializedName("data_escala") val dataEscala: String,
    @SerializedName("quem_pediu") val quemPediu: String,
    val substituto: String,
    val motivo: String,
    val status: String
)
