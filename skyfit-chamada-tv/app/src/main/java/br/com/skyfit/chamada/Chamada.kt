package br.com.skyfit.chamada

import org.json.JSONObject

/** Dados de uma chamada enviada pela recepção. */
data class Chamada(
    val id: String,
    val modelo: String,
    val cor: String,
    val placa: String,
    val ts: Long,
    val exibicoes: Int,
    val duracaoSeg: Int,
    val intervaloSeg: Int,
    val som: Boolean,
    val cancelada: Boolean
) {
    /** Placa no formato de leitura: ABC-1234 (antiga) ou ABC1D23 (Mercosul). */
    val placaFormatada: String
        get() {
            val p = placa.uppercase().replace("-", "").trim()
            val antiga = p.length == 7 && p[4].isDigit()
            return if (antiga) p.substring(0, 3) + "-" + p.substring(3) else p
        }

    companion object {
        fun de(json: JSONObject): Chamada? {
            val id = json.optString("id")
            if (id.isBlank()) return null
            return Chamada(
                id = id,
                modelo = json.optString("modelo"),
                cor = json.optString("cor"),
                placa = json.optString("placa"),
                ts = json.optLong("ts", 0L),
                // Limites de segurança, mesmo que o painel envie valores errados
                exibicoes = json.optInt("exibicoes", 3).coerceIn(1, 5),
                duracaoSeg = json.optInt("duracaoSeg", 10).coerceIn(5, 30),
                intervaloSeg = json.optInt("intervaloSeg", 20).coerceIn(5, 120),
                som = json.optBoolean("som", true),
                cancelada = json.optBoolean("cancelada", false)
            )
        }

        /** Chamada fictícia para o botão "Testar aviso" na TV. */
        fun teste() = Chamada(
            id = "teste-" + System.currentTimeMillis(),
            modelo = "Onix", cor = "Prata", placa = "ABC1D23",
            ts = System.currentTimeMillis(), exibicoes = 1, duracaoSeg = 10,
            intervaloSeg = 20, som = true, cancelada = false
        )
    }
}
