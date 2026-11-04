package br.com.skyfit.chamada

/**
 * Configuração central do app da TV.
 * ÚNICO arquivo que precisa ser editado antes de gerar o APK.
 */
object Config {
    // Firebase > Configurações do projeto > Geral > Seus apps (Web) > apiKey
    const val API_KEY = "AIzaSyDAA3-VvCtWn-w-p4fIB1RPvUZcguPuwTM"

    // Firebase > Realtime Database > URL no topo (sem barra no final)
    const val DB_URL = "https://skyfit-chamada-default-rtdb.firebaseio.com"

    // Unidades disponíveis: id técnico (usado no banco) to nome exibido
    val UNIDADES = listOf(
        "ville-roy" to "Ville Roy",
        "buritis" to "Buritis"
    )

    // Chamadas mais antigas que isso são ignoradas (evita repetir aviso antigo ao religar a TV)
    const val IDADE_MAXIMA_MS = 90_000L

    // Intervalo do sinal "estou online" enviado ao painel
    const val HEARTBEAT_MS = 60_000L
}
