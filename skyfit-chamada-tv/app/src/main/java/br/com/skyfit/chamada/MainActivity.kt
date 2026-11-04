package br.com.skyfit.chamada

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Tela de configuração da TV (usada só na instalação).
 * Navegável pelo controle remoto (setas + OK).
 */
class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var status: TextView
    private lateinit var campoNome: EditText
    private val botoesUnidade = mutableMapOf<String, Button>()
    private var unidadeEscolhida = ""

    private val vermelho = Color.parseColor("#D7141A")
    private val fundo = Color.parseColor("#151515")
    private val cinza = Color.parseColor("#2A2A2A")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        unidadeEscolhida = prefs.unidade
        setContentView(construirTela())
        if (prefs.configurada) AlertService.iniciar(this)
    }

    override fun onResume() {
        super.onResume()
        atualizarStatus() // volta da tela de permissão com o status atualizado
    }

    private fun construirTela(): View {
        val coluna = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(48), dp(32), dp(48), dp(32))
            setBackgroundColor(fundo)
        }

        coluna.addView(TextView(this).apply {
            text = "Sky Fit Chamada"
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        })

        status = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.parseColor("#CCCCCC"))
            setPadding(0, dp(8), 0, dp(20))
        }
        coluna.addView(status)

        coluna.addView(rotulo("1. Unidade desta TV"))
        val linhaUnidades = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        Config.UNIDADES.forEach { (id, nome) ->
            val b = botao(nome) {
                unidadeEscolhida = id
                pintarUnidades()
            }
            botoesUnidade[id] = b
            linhaUnidades.addView(b)
        }
        coluna.addView(linhaUnidades)
        pintarUnidades()

        coluna.addView(rotulo("2. Nome desta TV (ex.: Musculação 1)"))
        campoNome = EditText(this).apply {
            setText(prefs.nomeTv)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            hint = "Digite o nome"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setSingleLine()
        }
        coluna.addView(campoNome, LinearLayout.LayoutParams(dp(420), -2))

        coluna.addView(rotulo("3. Ações"))
        val acoes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        acoes.addView(botao("Salvar e ativar") { salvar() })
        acoes.addView(botao("Liberar sobreposição") { abrirPermissao() })
        acoes.addView(botao("Testar aviso") { testar() })
        coluna.addView(acoes)

        return ScrollView(this).apply { addView(coluna) }
    }

    private fun salvar() {
        val nome = campoNome.text.toString().trim()
        if (unidadeEscolhida.isBlank()) return aviso("Escolha a unidade")
        if (nome.length < 3) return aviso("Digite o nome da TV")
        prefs.unidade = unidadeEscolhida
        prefs.nomeTv = nome
        AlertService.reiniciar(this)
        aviso("Ativado. Esta TV já recebe chamadas.")
        atualizarStatus()
    }

    private fun abrirPermissao() {
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            } catch (e2: ActivityNotFoundException) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
                aviso("Procure: Apps > Acesso especial > Sobrepor a outros apps")
            }
        }
    }

    private fun testar() {
        if (!Settings.canDrawOverlays(this)) return aviso("Libere a sobreposição primeiro")
        AlertService.iniciar(this, AlertService.ACAO_TESTE)
        // Volta para a tela inicial para ver o aviso por cima de outro app
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
    }

    private fun atualizarStatus() {
        val sobrepor = if (Settings.canDrawOverlays(this)) "✅ liberada" else "❌ NÃO liberada"
        val unidade = Config.UNIDADES.firstOrNull { it.first == prefs.unidade }?.second ?: "não definida"
        status.text = "Sobreposição: $sobrepor\nUnidade: $unidade   •   TV: ${prefs.nomeTv.ifBlank { "sem nome" }}   •   ID: ${prefs.tvId}"
    }

    // ---------- Componentes visuais ----------

    private fun pintarUnidades() {
        botoesUnidade.forEach { (id, b) ->
            b.text = (if (id == unidadeEscolhida) "● " else "") +
                Config.UNIDADES.first { it.first == id }.second
        }
    }

    private fun rotulo(t: String) = TextView(this).apply {
        text = t
        textSize = 18f
        setTextColor(Color.WHITE)
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(18), 0, dp(8))
    }

    private fun botao(t: String, acao: () -> Unit) = Button(this).apply {
        text = t
        isAllCaps = false
        textSize = 17f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        isFocusable = true
        // Destaque forte no foco: essencial para usar com o controle remoto
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), forma(vermelho, Color.WHITE))
            addState(intArrayOf(), forma(cinza, cinza))
        }
        setPadding(dp(24), dp(12), dp(24), dp(12))
        layoutParams = LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(12) }
        setOnClickListener { acao() }
    }

    private fun forma(cor: Int, borda: Int) = GradientDrawable().apply {
        setColor(cor)
        cornerRadius = dp(8).toFloat()
        setStroke(dp(2), borda)
    }

    private fun aviso(t: String) = Toast.makeText(this, t, Toast.LENGTH_LONG).show()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
