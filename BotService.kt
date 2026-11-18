package com.lucas.catsbot
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.*
import kotlin.math.abs

class BotService : AccessibilityService() {
    private lateinit var wm: WindowManager
    private val h = Handler(Looper.getMainLooper())
    private var running = false
    private var tick = 0
    private var lastOk = 0L
    private var lastSeen = 0L
    private var fails = 0
    private var chests = 0
    private var bar: View? = null
    private lateinit var status: TextView
    private val names = listOf("botao FIGHT / Campeonato", "botao >> (pular luta)", "centro (bau / continuar)")

    override fun onServiceConnected() { wm = getSystemService(WINDOW_SERVICE) as WindowManager; showBar() }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { running = false; bar?.let { wm.removeView(it) }; super.onDestroy() }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun showBar() {
        val l = LinearLayout(this).apply { setBackgroundColor(0xCC000000.toInt()); gravity = Gravity.CENTER_VERTICAL }
        status = TextView(this).apply { setTextColor(Color.WHITE); textSize = 11f; text = "parado" }
        l.addView(status)
        fun b(t: String, a: () -> Unit) = l.addView(Button(this).apply { text = t; textSize = 10f; setOnClickListener { a() } })
        b("Play/Pause") { toggle() }; b("Pontos") { calibrate() }; b("Ref bau") { saveRef() }
        val lp = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT)
        lp.gravity = Gravity.TOP or Gravity.START
        wm.addView(l, lp); bar = l
    }

    private fun toggle() {
        running = !running
        if (running) { lastOk = System.currentTimeMillis(); fails = 0; h.post(loop) }
        status.text = if (running) "rodando | baus: $chests" else "parado | baus: $chests"
        if (running && Store.ref(this) == null) toast("Sem 'Ref bau': nao ha como detectar derrotas!")
    }

    private fun calibrate() {
        var i = 0
        val dm = resources.displayMetrics
        val v = View(this).apply { setBackgroundColor(0x33FFFF00) }
        toast("Toque em: ${names[0]}")
        v.setOnTouchListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_DOWN) {
                Store.setPoint(this, i, ev.rawX / dm.widthPixels, ev.rawY / dm.heightPixels); i++
                if (i < 3) toast("Toque em: ${names[i]}") else { wm.removeView(v); toast("Pontos salvos") }
            }
            true
        }
        wm.addView(v, WindowManager.LayoutParams(-1, -1, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT))
    }

    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            val dm = resources.displayMetrics
            val p = Store.point(this@BotService, tick % 3); tick++
            tap(p.first * dm.widthPixels, p.second * dm.heightPixels)
            snap { onSig(it) }
            h.postDelayed(this, 1200)
        }
    }

    private fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 60)).build(), null, null)
    }

    private fun snap(cb: (IntArray) -> Unit) {
        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(r: ScreenshotResult) {
                val hw = Bitmap.wrapHardwareBuffer(r.hardwareBuffer, r.colorSpace) ?: return
                val s = Bitmap.createScaledBitmap(hw.copy(Bitmap.Config.ARGB_8888, false), 16, 9, true)
                r.hardwareBuffer.close()
                cb(IntArray(144) { val c = s.getPixel(it % 16, it / 16); (Color.red(c) * 3 + Color.green(c) * 6 + Color.blue(c)) / 10 })
            }
            override fun onFailure(code: Int) {}
        })
    }

    private fun saveRef() = snap { Store.setRef(this, it); toast("Referencia do bau salva") }

    // Compara a tela atual com a referencia do bau. Viu o bau = ciclo vencido.
    private fun onSig(sig: IntArray) {
        if (!running) return
        val ref = Store.ref(this) ?: return
        val now = System.currentTimeMillis()
        val diff = ref.indices.sumOf { abs(ref[it] - sig[it]) } / 144
        if (diff < 22) {
            if (now - lastSeen > 20000) chests++
            lastSeen = now; lastOk = now; fails = 0
        } else if (now - lastOk > Store.timeoutMin(this) * 60000L) {
            fails++; lastOk = now
            val lim = Store.limit(this)
            if (fails >= lim && fails % lim == 0)
                Notifier.send(Store.topic(this), "CATS Bot", "${Store.name(this)}: perdendo em loop! $fails campeonatos sem bau.")
        }
        status.text = "rodando | baus: $chests | falhas: $fails"
    }
}
