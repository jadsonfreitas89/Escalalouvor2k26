package br.com.jadson.escalalouvor2k26.data.model

import com.google.gson.annotations.SerializedName

data class LouvorItem(
    @SerializedName(value = "id", alternate = ["ID", "Id"])
    val id: String? = "",
    
    @SerializedName(value = "data_escala", alternate = ["DATA_ESCALA", "Data_Escala", "dataEscala"])
    val dataEscala: String? = "",

    @SerializedName(value = "ordem", alternate = ["ORDEM", "Ordem"])
    val ordem: Int? = 0,
    
    @SerializedName(value = "louvor", alternate = ["LOUVOR", "Louvor"])
    val louvor: String? = "",
    
    @SerializedName(value = "link_youtube", alternate = ["LINK_YOUTUBE", "Link_Youtube", "linkYoutube"])
    val linkYoutube: String? = "",

    val data: String? = ""
)
