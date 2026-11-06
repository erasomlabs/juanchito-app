package erasom.juanchito.app

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

class GamificationManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("gamification_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_COINS = "user_coins"
        private const val KEY_LAST_LOGIN = "last_login_date"
        private const val KEY_DAILY_STREAK = "daily_streak"
        private const val KEY_HIGH_SCORE_PREFIX = "hs_"
    }

    fun getCoins(): Int = prefs.getInt(KEY_COINS, 50)

    fun addCoins(amount: Int) {
        val total = getCoins() + amount
        prefs.edit().putInt(KEY_COINS, total).apply()
    }

    fun spendCoins(amount: Int): Boolean {
        val current = getCoins()
        if (current >= amount) {
            prefs.edit().putInt(KEY_COINS, current - amount).apply()
            return true
        }
        return false
    }

    fun getHighScore(gameName: String): Int {
        return prefs.getInt(KEY_HIGH_SCORE_PREFIX + gameName, 0)
    }

    fun checkAndSaveHighScore(gameName: String, score: Int): Boolean {
        val current = getHighScore(gameName)
        if (score > current) {
            prefs.edit().putInt(KEY_HIGH_SCORE_PREFIX + gameName, score).apply()
            return true
        }
        return false
    }

    fun checkDailyLogin(): Pair<Int, Boolean> {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        val todayStr = sdf.format(Date())
        val lastLoginStr = prefs.getString(KEY_LAST_LOGIN, "") ?: ""
        var streak = prefs.getInt(KEY_DAILY_STREAK, 0)

        if (todayStr == lastLoginStr) {
            return Pair(streak, false) // Ya reclamado hoy
        }

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)

        if (lastLoginStr == yesterdayStr) {
            streak++
        } else {
            streak = 1
        }

        val reward = streak * 25
        addCoins(reward)
        prefs.edit()
            .putString(KEY_LAST_LOGIN, todayStr)
            .putInt(KEY_DAILY_STREAK, streak)
            .apply()

        return Pair(streak, true)
    }
}
