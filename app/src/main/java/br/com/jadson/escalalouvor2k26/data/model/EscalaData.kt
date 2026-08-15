package br.com.jadson.escalalouvor2k26.data.model

import com.google.gson.annotations.SerializedName

data class EscalaData(
    val escala: List<Escala> = emptyList(),
    val integrantes: List<Integrante> = emptyList(),
    val solicitacoes: List<Solicitacao> = emptyList(),
    val recados: List<Recado> = emptyList(),
    @SerializedName("link_louvores")
    val linkLouvores: List<LouvorItem> = emptyList()
)
