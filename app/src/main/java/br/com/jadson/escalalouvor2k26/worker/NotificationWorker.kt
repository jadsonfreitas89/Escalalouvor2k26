package br.com.jadson.escalalouvor2k26.worker

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.jadson.escalalouvor2k26.data.model.Escala
import br.com.jadson.escalalouvor2k26.data.model.Integrante
import br.com.jadson.escalalouvor2k26.data.repository.EscalaRepository
import br.com.jadson.escalalouvor2k26.data.session.SessionManager
import br.com.jadson.escalalouvor2k26.util.CultoUtils
import br.com.jadson.escalalouvor2k26.util.NotificationHelper
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

import br.com.jadson.escalalouvor2k26.data.model.EscalaData
import br.com.jadson.escalalouvor2k26.data.model.Solicitacao
import com.google.gson.Gson
import java.io.IOException

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val repository = EscalaRepository()
    private val sessionManager = SessionManager(context)
    private val timeNotifPrefs: SharedPreferences = context.getSharedPreferences("time_notifications", Context.MODE_PRIVATE)
    private val gson = Gson()

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        Log.d("NOTIF_WORKER_DEBUG", "WORKER INICIO")
        
        sessionManager.debugSession("NOTIF_SESSION_DEBUG")

        val user = sessionManager.getUser()
        if (user == null) {
            Log.d("NOTIF_WORKER_DEBUG", "SESSAO NAO ENCONTRADA")
            Log.d("NOTIF_WORKER_DEBUG", "WORKER FIM")
            return Result.success()
        }
        
        Log.d("NOTIF_WORKER_DEBUG", "SESSAO ENCONTRADA")
        Log.d("NOTIF_WORKER_DEBUG", "USUARIO = [${user.nome}]")

        // 0. Notificação de Boas-vindas
        checkAndSendWelcome(user.nome)
        
        Log.d("NOTIF_WORKER_DEBUG", "BUSCANDO DADOS REAIS")
        val res = repository.fetchEscalaData(user.nome, user.senha)
        
        if (res.isSuccess) {
            val newData = res.getOrNull() ?: return Result.success()
            
            val oldDataJson = sessionManager.getLastData()
            val oldData = if (oldDataJson != null) {
                try { gson.fromJson(oldDataJson, EscalaData::class.java) } catch(e: Exception) { null }
            } else null

            processNotifications(user, newData, oldData)
            checkRemoteNotifications(user)

            sessionManager.saveLastData(gson.toJson(newData))
            Log.d("NOTIF_WORKER_DEBUG", "WORKER FIM (Sucesso em ${System.currentTimeMillis() - startTime}ms)")
            return Result.success()
        } else {
            val error = res.exceptionOrNull()
            Log.e("NOTIF_WORKER_DEBUG", "ERRO AO BUSCAR DADOS: ${error?.message}")
            
            return if (error is IOException) {
                Log.d("NOTIF_WORKER_DEBUG", "ERRO DE INTERNET: Agendando retry")
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun checkAndSendWelcome(userName: String) {
        synchronized(this) {
            if (!sessionManager.isWelcomeSent(userName)) {
                Log.d("NOTIF_WELCOME_DEBUG", "user=$userName alreadySent=false action=SEND")
                val title = "🎵 Bem-vindo ao Escala de Louvor 2K26"
                val message = "Olá, $userName!\n\nSeja bem-vindo ao aplicativo Escala de Louvor 2K26. Que Deus abençoe seu serviço! 🙏"
                
                val success = NotificationHelper.showNotification(
                    context = applicationContext, 
                    id = 1000, 
                    title = title, 
                    message = message,
                    type = NotificationHelper.TYPE_BOAS_VINDAS
                )
                
                if (success) {
                    sessionManager.setWelcomeSent(userName)
                } else {
                    Log.e("NOTIF_WELCOME_DEBUG", "Falha ao disparar notificação. Tentará novamente na próxima execução.")
                }
            } else {
                Log.d("NOTIF_WELCOME_DEBUG", "user=$userName alreadySent=true action=SKIP")
            }
        }
    }

    private fun processNotifications(user: Integrante, newData: EscalaData, oldData: EscalaData?) {
        val isLider = user.funcao.uppercase().contains("LIDER")
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        
        // 1. Notificações do dia do culto
        val escalaHoje = newData.escala.find { it.data == todayStr }
        if (escalaHoje != null && isUserInEscala(escalaHoje, user.nome)) {
            checkAndSendTimeNotifications(user.nome)
        }

        // 2. Prazos de Louvores
        newData.escala.forEach { escala ->
            if (escala.dirigente.contains(user.nome, ignoreCase = true) && escala.louvores.isBlank()) {
                checkPraiseDeadline(escala, user.nome)
            }
        }

        if (oldData == null) return

        // 3. Alteração de Escala
        newData.escala.forEach { newEscala ->
            val oldEscala = oldData.escala.find { it.data == newEscala.data }
            if (oldEscala != null) {
                val wasInEscala = isUserInEscala(oldEscala, user.nome)
                val isInEscala = isUserInEscala(newEscala, user.nome)
                
                if ((wasInEscala || isInEscala) && hasEscalaChanged(newEscala, oldEscala)) {
                    val key = "scale_changed_${newEscala.data}_${user.nome}_${newEscala.hashCode()}"
                    if (!isAlreadyNotified(key)) {
                        notifyEscalaChanged(newEscala, user.nome)
                        markAsNotified(key)
                    }
                }
            }
        }

        // 4. Solicitação de Troca - PENDENTE (Líder)
        if (isLider) {
            newData.solicitacoes.filter { it.status.uppercase() == "PENDENTE" }.forEach { newSol ->
                val key = "swap_request_pendente_${newSol.id}"
                if (!isAlreadyNotified(key)) {
                    notifyNewSwapRequest(newSol)
                    markAsNotified(key)
                }
            }
        }

        // 5. Solicitação RECUSADA (Solicitante)
        newData.solicitacoes.filter { it.status.uppercase() == "RECUSADA" }.forEach { newSol ->
            if (newSol.quemPediu.equals(user.nome, ignoreCase = true)) {
                val key = "swap_refused_${newSol.id}"
                if (!isAlreadyNotified(key)) {
                    notifySwapRefused(newSol)
                    markAsNotified(key)
                }
            }
        }

        // 6. Louvores Cadastrados
        newData.escala.forEach { newEscala ->
            val oldEscala = oldData.escala.find { it.data == newEscala.data }
            if (oldEscala != null && isUserInEscala(newEscala, user.nome)) {
                if (oldEscala.louvores.isBlank() && newEscala.louvores.isNotBlank()) {
                    val key = "praise_registered_${newEscala.data}_${user.nome}"
                    if (!isAlreadyNotified(key)) {
                        notifyPraiseRegistered(newEscala, user.nome)
                        markAsNotified(key)
                    }
                }
            }
        }
    }

    private fun isAlreadyNotified(key: String): Boolean {
        return timeNotifPrefs.getBoolean(key, false)
    }

    private fun markAsNotified(key: String) {
        timeNotifPrefs.edit().putBoolean(key, true).commit()
    }

    private suspend fun checkRemoteNotifications(user: Integrante) {
        val res = repository.getNotificacoes(user.nome, user.senha)
        res.onSuccess { notificacoes ->
            notificacoes.forEach { notif ->
                val notificationKey = "remote_notif_${notif.id}"
                if (!isAlreadyNotified(notificationKey)) {
                    Log.d("NOTIF_WORKER_DEBUG", "EXIBINDO NOTIF REMOTA ID = ${notif.id}")
                    try {
                        NotificationHelper.showNotification(
                            context = applicationContext,
                            id = notif.id.hashCode(),
                            title = notif.titulo,
                            message = notif.mensagem,
                            type = notif.tipo.uppercase(),
                            referenceId = notif.id
                        )
                        markAsNotified(notificationKey)
                    } catch (e: Exception) {
                        Log.e("NOTIF_WORKER_DEBUG", "Falha ao exibir notif remota: ${e.message}")
                    }
                }
            }
        }
    }

    private fun hasEscalaChanged(new: Escala, old: Escala): Boolean {
        return new.dirigente != old.dirigente || new.vocal != old.vocal ||
               new.musicos != old.musicos || new.mesario != old.mesario ||
               new.louvores != old.louvores || new.uniforme != old.uniforme
    }

    private fun notifyEscalaChanged(escala: Escala, userName: String) {
        val titulo = CultoUtils.getTituloCulto(escala.data)
        val message = "Olá, $userName!\n\nA escala do $titulo (${escala.data}) foi atualizada pelo líder. Confira no aplicativo. 🙏"
        NotificationHelper.showNotification(
            context = applicationContext, 
            id = escala.data.hashCode() + 1, 
            title = "🎵 Escala atualizada", 
            message = message,
            type = NotificationHelper.TYPE_ESCALA_ALTERADA
        )
    }

    private fun notifyNewSwapRequest(sol: Solicitacao) {
        val message = "${sol.quemPediu} solicitou uma substituição para a escala de ${sol.dataEscala}. Acesse o aplicativo para analisar. 🙌"
        NotificationHelper.showNotification(
            context = applicationContext, 
            id = sol.id.hashCode(), 
            title = "🔔 Nova solicitação de troca", 
            message = message,
            type = NotificationHelper.TYPE_SOLICITACAO_TROCA
        )
    }

    private fun notifySwapRefused(sol: Solicitacao) {
        val message = "Sua solicitação de substituição para ${sol.dataEscala} foi recusada pelo líder. Confira os detalhes no aplicativo. 🙏"
        NotificationHelper.showNotification(
            context = applicationContext, 
            id = sol.id.hashCode() + 500, 
            title = "🙏 Troca Recusada", 
            message = message,
            type = NotificationHelper.TYPE_TROCA_RECUSADA
        )
    }

    private fun notifyPraiseRegistered(escala: Escala, userName: String) {
        val titulo = CultoUtils.getTituloCulto(escala.data)
        val message = "Olá, $userName!\n\nOs louvores do $titulo (${escala.data}) já foram cadastrados pelo dirigente. Acesse para conferir. 🙏"
        NotificationHelper.showNotification(
            context = applicationContext, 
            id = escala.data.hashCode() + 3, 
            title = "🎶 Louvores Disponíveis", 
            message = message,
            type = NotificationHelper.TYPE_LOUVORES
        )
    }

    private fun isUserInEscala(escala: Escala, nome: String): Boolean {
        val n = nome.lowercase().trim()
        val fields = listOf(escala.dirigente, escala.vocal, escala.musicos, escala.mesario)
        return fields.any { field ->
            field.lowercase().split(",", " x ", " X ").any { part ->
                val nameOnly = part.split("—", "-").first().trim()
                nameOnly == n
            }
        }
    }

    private fun checkAndSendTimeNotifications(userName: String) {
        val now = LocalTime.now()
        val targetTimes = listOf(
            LocalTime.of(9, 0), LocalTime.of(12, 0), LocalTime.of(15, 0),
            LocalTime.of(18, 0), LocalTime.of(19, 0)
        )

        targetTimes.forEach { time ->
            if (now.isAfter(time) && now.isBefore(time.plusMinutes(30))) {
                val notificationKey = "service_${LocalDate.now()}_${time.hour}_$userName"
                if (!isAlreadyNotified(notificationKey)) {
                    val message = "Bom dia, $userName!\n\nHoje é dia de servir na casa de Deus. Você está escalado para o culto de hoje. 🙏"
                    try {
                        NotificationHelper.showNotification(
                            context = applicationContext, 
                            id = time.hour + 5000, 
                            title = "🎶 Hoje é dia de servir!", 
                            message = message,
                            type = NotificationHelper.TYPE_ESCALA_ALTERADA
                        )
                        markAsNotified(notificationKey)
                    } catch (e: Exception) {
                        Log.e("NOTIF_WORKER_DEBUG", "Falha ao enviar notif de horário: ${e.message}")
                    }
                }
            }
        }
    }

    private fun checkPraiseDeadline(escala: Escala, userName: String) {
        val prazo = CultoUtils.getPrazoLouvores(escala.data) ?: return
        val today = LocalDate.now()
        
        if (today.isEqual(prazo) || today.isAfter(prazo)) {
            val notificationKey = "praise_deadline_${escala.data}_$userName"
            if (!isAlreadyNotified(notificationKey)) {
                val tituloCulto = CultoUtils.getTituloCulto(escala.data)
                val message = "Olá, $userName!\n\nOs louvores do $tituloCulto (${escala.data}) ainda não foram cadastrados. 🙏"
                try {
                    NotificationHelper.showNotification(
                        context = applicationContext, 
                        id = escala.data.hashCode() + 6000, 
                        title = "🎶 Cadastro de Louvores", 
                        message = message,
                        type = NotificationHelper.TYPE_LOUVORES
                    )
                    markAsNotified(notificationKey)
                } catch (e: Exception) {
                    Log.e("NOTIF_WORKER_DEBUG", "Falha ao enviar notif de prazo: ${e.message}")
                }
            }
        }
    }
}
