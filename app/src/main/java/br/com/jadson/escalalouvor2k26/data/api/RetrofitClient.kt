package br.com.jadson.escalalouvor2k26.data.api

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object RetrofitClient {
    private val requestCounter = AtomicInteger(0)
    // ... rest of constants
    // A URL base deve terminar com barra
    const val API_BASE_URL = "https://script.google.com/macros/s/AKfycbyK1dC5cjUtK0YZRN2FFp2wGuJpiLHU_g4rajI-SkMv2gDsbrKt2XgptQg_olu2tcs/"
    
    // O endpoint é apenas o 'exec'
    const val API_ENDPOINT = "exec"
    
    // Link da planilha mestre para o Líder
    const val MASTER_SPREADSHEET_URL = "https://docs.google.com/spreadsheets/d/1gUSw3B--ysm8xhhtIvAiV1e8tbrMb7q394QUU13G-F0/edit"

    private val gson = GsonBuilder()
        .setLenient() // Permite aceitar respostas que não sejam JSON puro (ajuda no redirecionamento)
        .create()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val count = requestCounter.incrementAndGet()
            val request = chain.request()
            Log.d("API_DEBUG", "Chamada #$count INICIO: ${request.method} ${request.url}")
            try {
                val response = chain.proceed(request)
                Log.d("API_DEBUG", "Chamada #$count FIM: Status=${response.code} (Redirect: ${response.isRedirect})")
                response
            } catch (e: Exception) {
                Log.e("API_DEBUG", "Chamada #$count ERRO: ${e.javaClass.simpleName} - ${e.message}")
                throw e
            }
        }
        .build()

    val instance: EscalaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(EscalaApiService::class.java)
    }
}
