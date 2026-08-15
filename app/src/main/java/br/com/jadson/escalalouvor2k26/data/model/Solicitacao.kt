package br.com.jadson.escalalouvor2k26.data.model

import com.google.gson.annotations.SerializedName

data class Solicitacao(
    val id: String = "",

    @SerializedName("data_escala")
    val dataEscala: String = "",

    @SerializedName("quem_pediu")
    val quemPediu: String = "",

    val funcao: String = "",
    
    val instrumento: String = "",

    val substituto: String = "",

    val motivo: String = "",

    val status: String = "",

    @SerializedName("data_criacao")
    val dataCriacao: String? = null,

    @SerializedName("data_decisao")
    val dataDecisao: String? = null,

    @SerializedName("decidido_por")
    val decididoPor: String? = null,

    @SerializedName("motivo_decisao")
    val motivoDecisao: String? = null
)
