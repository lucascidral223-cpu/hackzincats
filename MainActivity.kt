package com.lucas.catsbot
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 40) }
        fun t(x: String) = l.addView(TextView(this).apply { text = x; textSize = 15f })
        fun e(hint: String, v: String, num: Boolean = false) = EditText(this).apply {
            this.hint = hint; setText(v); if (num) inputType = InputType.TYPE_CLASS_NUMBER
        }.also { l.addView(it) }
        fun b(x: String, a: () -> Unit) = l.addView(Button(this).apply { text = x; setOnClickListener { a() } })

        t("USUARIO (use o mesmo nome + chave no emulador e no celular)")
        val name = e("Nome (ex: lucas)", Store.name(this))
        val key = e("Chave secreta (ex: x7k2q9)", Store.key(this))
        val lim = e("Alertar apos N campeonatos sem bau", Store.limit(this).toString(), true)
        val tm = e("Minutos max. por campeonato", Store.timeoutMin(this).toString(), true)
        val info = TextView(this); l.addView(info)
        fun refresh() { info.text = "Topico: ${Store.topic(this)}" }
        refresh()
        b("Salvar usuario / config") {
            Store.save(this, name.text.toString().trim(), key.text.toString().trim())
            Store.setLimits(this, lim.text.toString().toIntOrNull() ?: 3, tm.text.toString().toIntOrNull() ?: 8)
            refresh(); Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show()
        }
        t("\nNO EMULADOR (bot)")
        b("1. Ativar servico de Acessibilidade") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        t("2. Abra o jogo, use 'Pontos' e 'Ref bau' no overlay, depois Play.")
        t("\nNO SEU CELULAR (receber alertas)")
        b("Iniciar monitor") { startForegroundService(Intent(this, MonitorService::class.java)) }
        b("Parar monitor") { stopService(Intent(this, MonitorService::class.java)) }
        setContentView(ScrollView(this).apply { addView(l) })
    }
}
