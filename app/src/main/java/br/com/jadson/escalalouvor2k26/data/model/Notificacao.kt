package br.com.jadson.escalalouvor2k26.data.model

data class Notificacao(
    val id: String,
    val destinatario: String,
    val titulo: String,
    val mensagem: String,
    val tipo: String,
    val data: String,
    val lida: String
)

data class NotificacaoResponse(
    val sucesso: Boolean,
    val mensagem: String,
    val notificacoes: List<Notificacao> = emptyList()
)
