package erasom.juanchito.app

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import java.util.Locale

/**
 * Retos semanales determinísticos: cada semana ISO se generan objetivos fijos
 * por juego (semilla = semana + juego), de modo que todos los jugadores del
 * mundo ven el mismo reto. El premio se reclama al alcanzar el objetivo y se
 * registra en SharedPreferences para no repetirlo.
 */
object WeeklyChallenges {

    data class Challenge(
        val gameName: String,
        val target: Int,      // puntos requeridos (300–1500)
        val reward: Int,      // monedas (50–175)
        val description: String
    )

    private const val PREFS = "weekly_challenges"

    // ---------- Semana ISO-8601 ----------

    /** Clave de la semana actual, p. ej. "2026-W46". */
    fun weekKey(): String {
        val cal = Calendar.getInstance(Locale.US)
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.minimalDaysInFirstWeek = 4
        val year = cal.get(Calendar.WEEK_OF_YEAR).let { week ->
            // Ajuste ISO: si es la semana 1/52-53 de diciembre, decide el año correcto
            if (cal.get(Calendar.MONTH) == Calendar.DECEMBER && week == 1) cal.get(Calendar.YEAR) + 1
            else if (cal.get(Calendar.MONTH) == Calendar.JANUARY && week >= 52) cal.get(Calendar.YEAR) - 1
            else cal.get(Calendar.YEAR)
        }
        return "$year-W${cal.get(Calendar.WEEK_OF_YEAR)}"
    }

    private fun seed(gameName: String): Long {
        var h = 1125899906842597L
        val s = weekKey() + "|" + gameName
        for (c in s) h = 31 * h + c.code
        return h
    }

    // ---------- Reto de la semana ----------

    /** Reto de esta semana para un juego (determinístico, null solo si falla el hash). */
    fun current(gameName: String): Challenge {
        val s = seed(gameName)
        val target = 300 + (Math.abs(s) % 1201).toInt()                   // 300..1500
        val reward = 50 + (Math.abs(s * 7 + 13) % 126).toInt()            // 50..175
        val flavor = FLAVORS[(Math.abs(s / 3) % FLAVORS.size).toInt()]
        return Challenge(gameName, target, reward, flavor)
    }

    private val FLAVORS = listOf(
        "Consigue esta semana los puntos legendarios",
        "El pueblo exige tu mejor puntuación semanal",
        "El ránking arcade semanal te desafía",
        "Demuestra tu dominio esta semana",
        "La estatua de Pixel premia a los mejores"
    )

    // ---------- Reclamación ----------

    fun isClaimed(context: Context, gameName: String): Boolean {
        return prefs(context).getBoolean(claimKey(gameName), false)
    }

    fun markClaimed(context: Context, gameName: String) {
        prefs(context).edit().putBoolean(claimKey(gameName), true).apply()
    }

    /** Limpia reclamaciones de semanas anteriores (llamado al abrir el mundo). */
    fun pruneOldWeeks(context: Context) {
        val prefix = "claimed_${weekKey()}"
        val p = prefs(context)
        val stale = p.all.keys.filter { it.startsWith("claimed_") && !it.startsWith(prefix) }
        if (stale.isNotEmpty()) {
            val e = p.edit()
            stale.forEach { e.remove(it) }
            e.apply()
        }
    }

    private fun claimKey(gameName: String): String = "claimed_${weekKey()}|${gameName.hashCode()}"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
