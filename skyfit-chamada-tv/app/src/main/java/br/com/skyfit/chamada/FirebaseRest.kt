package br.com.skyfit.chamada

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cliente mínimo do Firebase via REST (sem SDK), para manter o app leve.
 * - Login anônimo (Identity Toolkit) com renovação automática do token
 * - Leitura, escrita e escuta em tempo real (Server-Sent Events) do Realtime Database
 */
class FirebaseRest(private val prefs: Prefs) {

    class AuthExpiradoException : Exception("Token expirado")

    // ---------- Autenticação ----------

    /** Devolve um token válido, renovando ou criando login anônimo quando necessário. */
    @Synchronized
    fun token(): String {
        val agora = System.currentTimeMillis()
        if (prefs.idToken.isNotBlank() && prefs.idTokenExpiraEm - agora > 5 * 60_000L) {
            return prefs.idToken
        }
        if (prefs.refreshToken.isNotBlank()) {
            try {
                renovarToken()
                return prefs.idToken
            } catch (e: Exception) {
                // Refresh inválido: cria um novo login anônimo abaixo
                prefs.refreshToken = ""
            }
        }
        loginAnonimo()
        return prefs.idToken
    }

    @Synchronized
    fun invalidarToken() {
        prefs.idTokenExpiraEm = 0L
    }

    private fun loginAnonimo() {
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${Config.API_KEY}"
        val resp = JSONObject(post(url, "{\"returnSecureToken\":true}", "application/json"))
        prefs.idToken = resp.getString("idToken")
        prefs.refreshToken = resp.getString("refreshToken")
        prefs.idTokenExpiraEm = System.currentTimeMillis() + resp.optLong("expiresIn", 3600L) * 1000L
    }

    private fun renovarToken() {
        val url = "https://securetoken.googleapis.com/v1/token?key=${Config.API_KEY}"
        val body = "grant_type=refresh_token&refresh_token=" +
            URLEncoder.encode(prefs.refreshToken, "UTF-8")
        val resp = JSONObject(post(url, body, "application/x-www-form-urlencoded"))
        prefs.idToken = resp.getString("id_token")
        prefs.refreshToken = resp.getString("refresh_token")
        prefs.idTokenExpiraEm = System.currentTimeMillis() + resp.optString("expires_in", "3600").toLong() * 1000L
    }

    // ---------- Banco de dados ----------

    private fun dbUrl(path: String) = "${Config.DB_URL}/$path.json?auth=${token()}"

    /** Lê um nó. Retorna null se não existir. */
    fun get(path: String): JSONObject? {
        val con = abrir(dbUrl(path), "GET")
        val texto = lerResposta(con)
        return if (texto.isBlank() || texto == "null") null else JSONObject(texto)
    }

    /** Grava (substitui) um nó. */
    fun put(path: String, dados: JSONObject) {
        val con = abrir(dbUrl(path), "PUT")
        con.doOutput = true
        con.setRequestProperty("Content-Type", "application/json")
        con.outputStream.use { it.write(dados.toString().toByteArray(Charsets.UTF_8)) }
        lerResposta(con)
    }

    /**
     * Escuta um nó em tempo real. Bloqueia até a conexão cair.
     * onEvento(evento, dados) é chamado para cada evento recebido.
     */
    fun escutar(path: String, deveParar: () -> Boolean, onEvento: (String, String) -> Unit) {
        val con = abrir(dbUrl(path), "GET")
        con.setRequestProperty("Accept", "text/event-stream")
        con.readTimeout = 90_000 // Firebase envia keep-alive a cada ~30s; 90s sem nada = conexão morta
        if (con.responseCode == 401) throw AuthExpiradoException()
        if (con.responseCode !in 200..299) throw Exception("HTTP ${con.responseCode} ao escutar $path")

        BufferedReader(InputStreamReader(con.inputStream, Charsets.UTF_8)).use { leitor ->
            var evento = ""
            while (!deveParar()) {
                val linha = leitor.readLine() ?: break
                when {
                    linha.startsWith("event:") -> evento = linha.substringAfter(":").trim()
                    linha.startsWith("data:") -> {
                        val dados = linha.substringAfter(":").trim()
                        if (evento == "auth_revoked") throw AuthExpiradoException()
                        if (evento == "cancel") throw Exception("Acesso negado pelas regras do Firebase")
                        onEvento(evento, dados)
                    }
                }
            }
        }
        con.disconnect()
    }

    // ---------- HTTP ----------

    private fun abrir(url: String, metodo: String): HttpURLConnection {
        val con = URL(url).openConnection() as HttpURLConnection
        con.requestMethod = metodo
        con.connectTimeout = 15_000
        con.readTimeout = 20_000
        return con
    }

    private fun post(url: String, body: String, contentType: String): String {
        val con = abrir(url, "POST")
        con.doOutput = true
        con.setRequestProperty("Content-Type", contentType)
        con.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        return lerResposta(con)
    }

    private fun lerResposta(con: HttpURLConnection): String {
        val codigo = con.responseCode
        if (codigo == 401) throw AuthExpiradoException()
        val stream = if (codigo in 200..299) con.inputStream else con.errorStream
        val texto = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        con.disconnect()
        if (codigo !in 200..299) throw Exception("HTTP $codigo: $texto")
        return texto
    }
}
