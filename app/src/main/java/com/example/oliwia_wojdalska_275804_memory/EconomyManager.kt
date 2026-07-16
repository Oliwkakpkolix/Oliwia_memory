package com.example.oliwia_wojdalska_275804_memory

import android.content.Context
// sklep
object EconomyManager {

    private const val PREFS_NAME = "MemoryGamePrefs"
    private const val KEY_CURRENCY = "player_currency"

    // aktualne saldo
    fun getCurrency(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_CURRENCY, 0)
    }
    // dodanie waluty po ukończeniu poziomu
    fun addCurrency(context: Context, amount: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_CURRENCY, 0)
        prefs.edit().putInt(KEY_CURRENCY, current + amount).apply()
    }
    // odejmowanie waluty
    fun spendCurrency(context: Context, amount: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_CURRENCY, 0)
        return if (current >= amount) {
            prefs.edit().putInt(KEY_CURRENCY, current - amount).apply()
            true
        } else {
            false
        }
    }
    // zapisanie zakupu
    fun setPurchased(context: Context, itemId: String, purchased: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("purchased_$itemId", purchased).apply()
    }
    // czy kupiono
    fun isPurchased(context: Context, itemId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("purchased_$itemId", false)
    }
    fun resetEconomy(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
    // tymczasowo dodaje 10 000 do testów
    fun addTestMoney(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_CURRENCY, 0)
       prefs.edit().putInt(KEY_CURRENCY, current + 10000).apply()
    }
}