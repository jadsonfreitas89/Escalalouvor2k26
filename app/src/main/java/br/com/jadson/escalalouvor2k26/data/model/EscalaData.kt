package br.com.jadson.escalalouvor2k26.data.model

data class EscalaData(
    val escala: List<Escala> = emptyList(),
    val integrantes: List<Integrante> = emptyList(),
    val solicitacoes: List<Solicitacao> = emptyList(),
    val recados: List<Recado> = emptyList()
)
