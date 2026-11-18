package com.lucas.catsbot
import android.content.Context

object Store {
    private fun sp(c: Context) = c.getSharedPreferences("cfg", Context.MODE_PRIVATE)
    fun name(c: Context) = sp(c).getString("name", "") ?: ""
    fun key(c: Context) = sp(c).getString("key", "") ?: ""
    fun save(c: Context, name: String, key: String) = sp(c).edit().putString("name", name).putString("key", key).apply()
    // Cada usuario tem um topico proprio: so quem usa o mesmo nome+chave recebe os alertas.
    fun topic(c: Context) = "catsbot-" + name(c).lowercase().filter { it.isLetterOrDigit() } + "-" + key(c).filter { it.isLetterOrDigit() }
    fun limit(c: Context) = sp(c).getInt("limit", 3)
    fun timeoutMin(c: Context) = sp(c).getInt("tmin", 8)
    fun setLimits(c: Context, limit: Int, tmin: Int) = sp(c).edit().putInt("limit", limit).putInt("tmin", tmin).apply()
    private val defX = floatArrayOf(0.90f, 0.97f, 0.50f)
    private val defY = floatArrayOf(0.93f, 0.93f, 0.50f)
    fun point(c: Context, i: Int) = Pair(sp(c).getFloat("x$i", defX[i]), sp(c).getFloat("y$i", defY[i]))
    fun setPoint(c: Context, i: Int, x: Float, y: Float) = sp(c).edit().putFloat("x$i", x).putFloat("y$i", y).apply()
    fun ref(c: Context): IntArray? = sp(c).getString("ref", null)?.split(",")?.map { it.toInt() }?.toIntArray()
    fun setRef(c: Context, r: IntArray) = sp(c).edit().putString("ref", r.joinToString(",")).apply()
}
