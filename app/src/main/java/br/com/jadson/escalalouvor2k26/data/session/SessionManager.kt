package br.com.jadson.escalalouvor2k26.data.session

import android.content.Context
import android.content.SharedPreferences
import br.com.jadson.escalalouvor2k26.data.model.Integrante

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_FUNCTION = "user_function"
        private const val KEY_USER_PASSWORD = "user_password"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun saveSession(integrante: Integrante) {
        prefs.edit().apply {
            putString(KEY_USER_NAME, integrante.nome)
            putString(KEY_USER_FUNCTION, integrante.funcao)
            putString(KEY_USER_PASSWORD, integrante.senha)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
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
        prefs.edit().clear().apply()
    }
}
