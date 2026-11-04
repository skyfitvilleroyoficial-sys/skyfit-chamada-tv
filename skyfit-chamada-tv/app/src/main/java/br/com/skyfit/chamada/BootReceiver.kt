package br.com.skyfit.chamada

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Inicia o serviço automaticamente quando a TV liga ou o app é atualizado. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Prefs(context).configurada) {
            try { AlertService.iniciar(context) } catch (_: Exception) {}
        }
    }
}
