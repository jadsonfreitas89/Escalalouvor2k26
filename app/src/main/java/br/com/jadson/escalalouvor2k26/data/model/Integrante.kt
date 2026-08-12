package br.com.jadson.escalalouvor2k26.data.model

data class Integrante(
    val nome: String,
    val funcao: String,
    val senha: String = "",
    val instrumento: String = ""
)
