package br.com.skyfit.chamada

import android.content.Context
import java.util.UUID

/** Armazenamento local simples das configurações da TV e do login anônimo. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("skyfit_chamada", Context.MODE_PRIVATE)

    var unidade: String
        get() = sp.getString("unidade", "") ?: ""
        set(v) = sp.edit().putString("unidade", v).apply()

    var nomeTv: String
        get() = sp.getString("nomeTv", "") ?: ""
        set(v) = sp.edit().putString("nomeTv", v).apply()

    /** Identificador fixo desta TV, gerado uma única vez. */
    val tvId: String
        get() {
            var id = sp.getString("tvId", null)
            if (id == null) {
                id = "tv-" + UUID.randomUUID().toString().take(8)
                sp.edit().putString("tvId", id).apply()
            }
            return id
        }

    var idToken: String
        get() = sp.getString("idToken", "") ?: ""
        set(v) = sp.edit().putString("idToken", v).apply()

    var idTokenExpiraEm: Long
        get() = sp.getLong("idTokenExp", 0L)
        set(v) = sp.edit().putLong("idTokenExp", v).apply()

    var refreshToken: String
        get() = sp.getString("refreshToken", "") ?: ""
        set(v) = sp.edit().putString("refreshToken", v).apply()

    /** Última chamada já exibida (evita mostrar o mesmo aviso duas vezes). */
    var ultimaChamadaId: String
        get() = sp.getString("ultimaChamada", "") ?: ""
        set(v) = sp.edit().putString("ultimaChamada", v).apply()

    val configurada: Boolean
        get() = unidade.isNotBlank() && nomeTv.isNotBlank()
}
