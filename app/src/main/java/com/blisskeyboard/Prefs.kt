package com.blisskeyboard

import android.content.Context

object Prefs {
    private const val P = "bliss"
    fun get(c: Context, k: String, d: String = "") = c.getSharedPreferences(P, 0).getString(k, d) ?: d
    fun put(c: Context, k: String, v: String) = c.getSharedPreferences(P, 0).edit().putString(k, v).apply()
    fun color(c: Context): Int = try { android.graphics.Color.parseColor(get(c,"color","#BFE9E7")) } catch (_: Exception) { 0xffbfe9e7.toInt() }
}
