package br.com.jadson.escalalouvor2k26.service

import android.util.Log
import br.com.jadson.escalalouvor2k26.data.repository.EscalaRepository
import br.com.jadson.escalalouvor2k26.data.session.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val repository = EscalaRepository()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Novo token recebido: $token")
        
        val sessionManager = SessionManager(applicationContext)
        val user = sessionManager.getUser()
        
        if (user != null) {
            serviceScope.launch {
                repository.atualizarTokenFcm(user.nome, user.senha, token)
            }
        }
    }
}
