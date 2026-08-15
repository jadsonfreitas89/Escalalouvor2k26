package br.com.jadson.escalalouvor2k26.data.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import br.com.jadson.escalalouvor2k26.data.model.Integrante

class SessionManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "user_session"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_FUNCTION = "user_function"
        private const val KEY_USER_PASSWORD = "user_password"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LAST_DATA = "last_data_notif"
        private const val KEY_WELCOME_SENT_PREFIX = "welcome_sent_v2_" 
    }

    fun debugSession(tag: String) {
        Log.d(tag, "NOTIF_SESSION_DEBUG: INICIANDO RECUPERACAO DA SESSAO")
        Log.d(tag, "NOTIF_SESSION_DEBUG: Arquivo = $PREF_NAME")
        
        val name = prefs.getString(KEY_USER_NAME, null)
        val function = prefs.getString(KEY_USER_FUNCTION, null)
        val password = prefs.getString(KEY_USER_PASSWORD, null)
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

        if (name != null) {
            Log.d(tag, "NOTIF_SESSION_DEBUG: SESSAO ENCONTRADA")
            Log.d(tag, "NOTIF_SESSION_DEBUG: USUARIO = [$name]")
            Log.d(tag, "NOTIF_SESSION_DEBUG: FUNCAO = [$function]")
            Log.d(tag, "NOTIF_SESSION_DEBUG: SENHA PRESENTE = ${!password.isNullOrEmpty()}")
            Log.d(tag, "NOTIF_SESSION_DEBUG: LOGADO = $isLoggedIn")
        } else {
            Log.d(tag, "NOTIF_SESSION_DEBUG: SESSAO NAO ENCONTRADA")
            val allKeys = prefs.all.keys
            Log.d(tag, "NOTIF_SESSION_DEBUG: Chaves disponiveis: $allKeys")
        }
    }

    fun saveSession(integrante: Integrante) {
        prefs.edit().apply {
            putString(KEY_USER_NAME, integrante.nome)
            putString(KEY_USER_FUNCTION, integrante.funcao)
            putString(KEY_USER_PASSWORD, integrante.senha)
            putBoolean(KEY_IS_LOGGED_IN, true)
            commit() // Forçar escrita imediata
        }
    }

    fun isWelcomeSent(userName: String): Boolean {
        val normalized = normalizeName(userName)
        val key = KEY_WELCOME_SENT_PREFIX + normalized
        val sent = prefs.getBoolean(key, false)
        Log.d("NOTIF_WELCOME_DEBUG", "user=$normalized key=$key alreadySent=$sent")
        return sent
    }

    fun setWelcomeSent(userName: String) {
        val normalized = normalizeName(userName)
        val key = KEY_WELCOME_SENT_PREFIX + normalized
        prefs.edit().putBoolean(key, true).commit()
        Log.d("NOTIF_WELCOME_DEBUG", "key=$key persisted=true")
    }

    private fun normalizeName(name: String): String {
        return name.trim().lowercase()
    }

    fun getUser(): Integrante? {
        val name = prefs.getString(KEY_USER_NAME, null) ?: return null
        val function = prefs.getString(KEY_USER_FUNCTION, "") ?: ""
        val password = prefs.getString(KEY_USER_PASSWORD, "") ?: ""
        return Integrante(name, function, password)
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logout() {
        prefs.edit().apply {
            remove(KEY_USER_NAME)
            remove(KEY_USER_FUNCTION)
            remove(KEY_USER_PASSWORD)
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_LAST_DATA)
            apply()
        }
    }

    fun saveLastData(dataJson: String) {
        prefs.edit().putString(KEY_LAST_DATA, dataJson).apply()
    }

    fun getLastData(): String? {
        return prefs.getString(KEY_LAST_DATA, null)
    }
}
