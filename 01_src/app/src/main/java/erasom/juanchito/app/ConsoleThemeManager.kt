package erasom.juanchito.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable

object ConsoleThemeManager {

    enum class PadShape(val label: String, val radiusRatio: Float) {
        ROUND("Redondeado", 0.5f),
        SQUIRCLE("Squircle", 0.25f),
        SQUARE("Cuadrado", 0.05f)
    }

    data class ColorTheme(
        val name: String,
        val bg: Int,
        val panel: Int,
        val accentOrange: Int,
        val accentYellow: Int,
        val text: Int,
        val border: Int
    )

    val THEMES = listOf(
        ColorTheme(
            name = "Atari 2600 Wood",
            bg = Color.parseColor("#0E0B07"),
            panel = Color.parseColor("#2B170B"),
            accentOrange = Color.parseColor("#E06A26"),
            accentYellow = Color.parseColor("#F5C542"),
            text = Color.parseColor("#F5E6D3"),
            border = Color.parseColor("#8C4B18")
        ),
        ColorTheme(
            name = "NES Famicom",
            bg = Color.parseColor("#150305"),
            panel = Color.parseColor("#691419"),
            accentOrange = Color.parseColor("#C41E24"),
            accentYellow = Color.parseColor("#E6B800"),
            text = Color.parseColor("#F7EFE2"),
            border = Color.parseColor("#3B090C")
        ),
        ColorTheme(
            name = "GameBoy Classic",
            bg = Color.parseColor("#0F380F"),
            panel = Color.parseColor("#306230"),
            accentOrange = Color.parseColor("#8BAC0F"),
            accentYellow = Color.parseColor("#9BBC0F"),
            text = Color.parseColor("#E0F8D0"),
            border = Color.parseColor("#0F380F")
        ),
        ColorTheme(
            name = "Cyber Neon",
            bg = Color.parseColor("#080812"),
            panel = Color.parseColor("#14142B"),
            accentOrange = Color.parseColor("#00E5FF"),
            accentYellow = Color.parseColor("#FF007F"),
            text = Color.parseColor("#FFFFFF"),
            border = Color.parseColor("#7D00FF")
        )
    )

    fun createButtonDrawable(
        bgColor: Int,
        strokeColor: Int,
        shape: PadShape,
        sizeDp: Int,
        density: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(bgColor)
            setStroke((2 * density).toInt(), strokeColor)
            val px = sizeDp * density
            val r = px * shape.radiusRatio
            cornerRadius = r
        }
    }
}
