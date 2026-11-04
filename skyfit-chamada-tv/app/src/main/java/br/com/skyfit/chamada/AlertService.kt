package br.com.skyfit.chamada

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import org.json.JSONObject

/**
 * Serviço em primeiro plano que:
 * 1. escuta /unidades/{unidade}/chamada em tempo real e exibe o aviso;
 * 2. envia a cada minuto o sinal "estou online" para o painel da recepção.
 * Reconecta sozinho com espera progressiva se a internet cair.
 */
class AlertService : Service() {

    private lateinit var prefs: Prefs
    private lateinit var fb: FirebaseRest
    private lateinit var overlay: OverlayController

    @Volatile private var rodando = false
    @Volatile private var chamadaAtualId = ""
    @Volatile private var ultimoErro = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        fb = FirebaseRest(prefs)
        overlay = OverlayController(this)
        iniciarPrimeiroPlano()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACAO_TESTE) {
            overlay.tocar(Chamada.teste())
        }
        if (!rodando && prefs.configurada) {
            rodando = true
            Thread(::loopEscuta, "chamada-escuta").start()
            Thread(::loopHeartbeat, "chamada-heartbeat").start()
        }
        return START_STICKY // o Android recria o serviço se ele for encerrado
    }

    override fun onDestroy() {
        rodando = false
        overlay.parar()
        super.onDestroy()
    }

    // ---------- Escuta das chamadas ----------

    private fun loopEscuta() {
        var espera = 2_000L
        val caminho = "unidades/${prefs.unidade}/chamada"
        while (rodando) {
            try {
                fb.escutar(caminho, { !rodando }) { evento, _ ->
                    // Em qualquer mudança, busca o objeto completo (mais simples e robusto)
                    if (evento == "put" || evento == "patch") processar(fb.get(caminho))
                }
                espera = 2_000L
            } catch (e: FirebaseRest.AuthExpiradoException) {
                fb.invalidarToken()
            } catch (e: Exception) {
                ultimoErro = e.message ?: e.javaClass.simpleName
                Thread.sleep(espera)
                espera = (espera * 2).coerceAtMost(60_000L)
            }
        }
    }

    private fun processar(json: JSONObject?) {
        val c = Chamada.de(json ?: return) ?: return

        if (c.cancelada) {
            if (c.id == chamadaAtualId) overlay.parar()
            prefs.ultimaChamadaId = c.id
            return
        }
        if (c.id == prefs.ultimaChamadaId) return

        prefs.ultimaChamadaId = c.id
        val idade = System.currentTimeMillis() - c.ts
        if (idade > Config.IDADE_MAXIMA_MS) return // chamada antiga: não reexibe

        chamadaAtualId = c.id
        overlay.tocar(c)
    }

    // ---------- Sinal "estou online" ----------

    private fun loopHeartbeat() {
        while (rodando) {
            try {
                val dados = JSONObject()
                    .put("nome", prefs.nomeTv)
                    .put("lastSeen", JSONObject().put(".sv", "timestamp"))
                    .put("versao", versao())
                    .put("sobreposicaoOk", overlay.podeSobrepor())
                    .put("erro", ultimoErro)
                fb.put("unidades/${prefs.unidade}/tvs/${prefs.tvId}", dados)
            } catch (e: FirebaseRest.AuthExpiradoException) {
                fb.invalidarToken()
            } catch (_: Exception) {
            }
            Thread.sleep(Config.HEARTBEAT_MS)
        }
    }

    private fun versao(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
    } catch (_: Exception) { "?" }

    // ---------- Notificação obrigatória do serviço em primeiro plano ----------

    private fun iniciarPrimeiroPlano() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL, "Chamadas da recepção", NotificationManager.IMPORTANCE_MIN)
        )
        val n = Notification.Builder(this, CANAL)
            .setContentTitle("Sky Fit Chamada ativa")
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    companion object {
        private const val CANAL = "chamada"
        const val ACAO_TESTE = "br.com.skyfit.chamada.TESTE"

        fun iniciar(context: Context, acao: String? = null) {
            val i = Intent(context, AlertService::class.java)
            if (acao != null) i.action = acao
            context.startForegroundService(i)
        }

        /** Reinicia o serviço (usado após mudar unidade ou nome). */
        fun reiniciar(context: Context) {
            context.stopService(Intent(context, AlertService::class.java))
            iniciar(context)
        }
    }
}
