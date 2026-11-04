package br.com.skyfit.chamada

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Desenha o aviso em tela cheia POR CIMA de qualquer app (inclusive o Indoor),
 * sem tirar o foco dele. Ao terminar, o aviso some e o conteúdo continua normalmente.
 */
class OverlayController(private val context: Context) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())
    private val token = Any() // identifica as tarefas agendadas desta chamada
    private var viewAtual: View? = null
    private val animadores = mutableListOf<ValueAnimator>()

    fun podeSobrepor(): Boolean = Settings.canDrawOverlays(context)

    /** Agenda todas as exibições da chamada (ex.: 3x de 10s com 20s de intervalo). */
    fun tocar(c: Chamada) {
        main.post {
            parar()
            if (!podeSobrepor()) return@post
            val ciclo = (c.duracaoSeg + c.intervaloSeg) * 1000L
            val inicio = SystemClock.uptimeMillis()
            for (i in 0 until c.exibicoes) {
                val t = inicio + i * ciclo
                main.postAtTime({ mostrar(c) }, token, t)
                main.postAtTime({ esconder() }, token, t + c.duracaoSeg * 1000L)
            }
        }
    }

    /** Cancela exibições pendentes e remove o aviso da tela. */
    fun parar() {
        main.removeCallbacksAndMessages(token)
        esconder()
    }

    private fun mostrar(c: Chamada) {
        esconder()
        val view = construirView(c)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )
        try {
            wm.addView(view, params)
            viewAtual = view
            animadores.forEach { it.start() }
            if (c.som) bip()
        } catch (e: Exception) {
            // Sem permissão ou janela recusada: o heartbeat informa o painel
        }
    }

    private fun esconder() {
        animadores.forEach { it.cancel() }
        animadores.clear()
        viewAtual?.let { v ->
            try { wm.removeView(v) } catch (_: Exception) {}
        }
        viewAtual = null
    }

    private fun bip() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 600)
            main.postDelayed({ tg.release() }, 1500)
        } catch (_: Exception) {}
    }

    // ---------- Layout do aviso (feito em código para não depender de XML) ----------

    private fun construirView(c: Chamada): View {
        val altura = context.resources.displayMetrics.heightPixels.toFloat()
        fun px(fracao: Float) = altura * fracao

        val raiz = FrameLayout(context).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#F2B30F14"), Color.parseColor("#F20A0A0A"))
            )
        }

        val coluna = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val condensada = Typeface.create("sans-serif-condensed", Typeface.BOLD)

        fun texto(t: String, frac: Float, cor: Int = Color.WHITE, negrito: Boolean = true) =
            TextView(context).apply {
                text = t
                setTextColor(cor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, px(frac))
                typeface = if (negrito) condensada else Typeface.create("sans-serif-condensed", Typeface.NORMAL)
                gravity = Gravity.CENTER
                letterSpacing = 0.02f
            }

        val titulo = texto("⚠ ATENÇÃO", 0.13f)
        val sub = texto("Proprietário do veículo", 0.055f, Color.parseColor("#E6FFFFFF"), negrito = false)
        val veiculo = texto("${c.modelo} ${c.cor}".uppercase().trim(), 0.085f, Color.parseColor("#FFD54A"))

        // Placa no estilo Mercosul: faixa azul + fundo branco + texto preto
        val placa = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = px(0.015f)
                setStroke(px(0.006f).toInt(), Color.BLACK)
            }
            val faixa = TextView(context).apply {
                text = "BRASIL"
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#0A3DA8"))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, px(0.025f))
                typeface = condensada
                gravity = Gravity.CENTER
            }
            val numero = TextView(context).apply {
                text = c.placaFormatada
                setTextColor(Color.BLACK)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, px(0.12f))
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(px(0.04f).toInt(), 0, px(0.04f).toInt(), 0)
            }
            addView(faixa, LinearLayout.LayoutParams(-1, -2))
            addView(numero, LinearLayout.LayoutParams(-2, -2))
        }

        val instrucao = texto("Por favor, compareça à recepção", 0.065f)

        fun espaco(frac: Float) = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(1, px(frac).toInt())
        }

        coluna.addView(titulo)
        coluna.addView(espaco(0.01f))
        coluna.addView(sub)
        coluna.addView(veiculo)
        coluna.addView(espaco(0.03f))
        coluna.addView(placa, LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.CENTER_HORIZONTAL })
        coluna.addView(espaco(0.04f))
        coluna.addView(instrucao)

        raiz.addView(coluna, FrameLayout.LayoutParams(-1, -1))

        // Barra de tempo restante no rodapé
        val barra = View(context).apply {
            setBackgroundColor(Color.WHITE)
            pivotX = 0f
        }
        raiz.addView(barra, FrameLayout.LayoutParams(-1, px(0.012f).toInt(), Gravity.BOTTOM))
        animadores += ObjectAnimator.ofFloat(barra, "scaleX", 1f, 0f).apply {
            duration = c.duracaoSeg * 1000L
            interpolator = LinearInterpolator()
        }

        // Título pisca suavemente para chamar atenção
        animadores += ObjectAnimator.ofFloat(titulo, "alpha", 1f, 0.35f).apply {
            duration = 500
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }

        return raiz
    }
}
