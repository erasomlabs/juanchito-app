package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.abs

/**
 * Escenarios NES coloridos reutilizables para los juegos Pixel.
 * Cada pieza dibuja formas variadas (colinas, nubes, estrellas, edificios, etc.)
 * con paleta NES para dar ambiente a los fondos en vez de color plano negro.
 */
object NesScenery {

    private fun p(colorHex: String, aa: Boolean = false) = Paint().apply { color = Color.parseColor(colorHex); isAntiAlias = aa }
    private fun pInt(colorInt: Int, aa: Boolean = false) = Paint().apply { color = colorInt; isAntiAlias = aa }

    /** Cielo en bandas horizontales de colores (efecto gradiente NES). */
    fun drawSkyBands(canvas: Canvas, w: Int, h: Float, top: String, bottom: String, bands: Int = 6) {
        val topC = Color.parseColor(top); val botC = Color.parseColor(bottom)
        val bandH = h / bands
        for (i in 0 until bands) {
            val f = i / (bands - 1f).coerceAtLeast(1f)
            val c = Color.rgb(
                ((Color.red(topC) * (1 - f)) + Color.red(botC) * f).toInt(),
                ((Color.green(topC) * (1 - f)) + Color.green(botC) * f).toInt(),
                ((Color.blue(topC) * (1 - f)) + Color.blue(botC) * f).toInt()
            )
            pInt(c).let { canvas.drawRect(0f, i * bandH, w.toFloat(), (i + 1) * bandH, it) }
        }
    }

    /** Campo de estrellas deterministas. */
    fun drawStars(canvas: Canvas, w: Int, h: Int, count: Int = 40) {
        val sp = p("#F8F8F8")
        var seed = 1234567L
        for (i in 0 until count) {
            seed = seed * 1103515245L + 12345L
            val x = abs(seed % w)
            seed = seed * 1103515245L + 12345L
            val y = abs(seed % (h / 2))
            val size = if (i % 5 == 0) 3f else 2f
            canvas.drawCircle(x.toFloat(), y.toFloat(), size, sp)
        }
    }

    /** Sol radiante con anillos. */
    fun drawSun(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val halo = p("#FC9838", true); val core = p("#F8D878", true)
        val ring = p("#D82800").apply { style = Paint.Style.STROKE; strokeWidth = 3f }
        canvas.drawCircle(cx, cy, r, halo)
        canvas.drawCircle(cx, cy, r * 0.7f, core)
        canvas.drawCircle(cx, cy, r, ring)
    }

    fun drawMoon(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        canvas.drawCircle(cx, cy, r, p("#F8F8F8", true))
        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.25f, r * 0.25f, p("#D8D8D8", true))
        canvas.drawCircle(cx + r * 0.3f, cy + r * 0.2f, r * 0.18f, p("#D8D8D8", true))
    }

    /** Nubes redondeadas en varias alturas. */
    fun drawClouds(canvas: Canvas, w: Int, baseY: Float, scroll: Float = 0f) {
        val cp = p("#F8F8F8", true)
        val cs = p("#D8E8F8", true)
        val clouds = listOf(0.15f to 0.6f, 0.45f to 0.45f, 0.75f to 0.7f, 0.95f to 0.5f)
        clouds.forEachIndexed { i, (fx, scale) ->
            val cx = ((fx * w + scroll * (i + 1) * 12f) % (w + 160f)) - 80f
            val cy = baseY * (0.5f + i * 0.12f)
            val s = 26f * scale
            canvas.drawCircle(cx, cy, s, cp)
            canvas.drawCircle(cx + s * 0.9f, cy + s * 0.15f, s * 0.75f, cp)
            canvas.drawCircle(cx - s * 0.9f, cy + s * 0.2f, s * 0.6f, cs)
            canvas.drawCircle(cx + s * 0.3f, cy - s * 0.5f, s * 0.65f, cp)
        }
    }

    /** Colinas en capas estilo NES. */
    fun drawHills(canvas: Canvas, w: Int, horizonY: Float, back: String, front: String) {
        val bp = p(back, true); val fp = p(front, true)
        // capa trasera
        val path = android.graphics.Path()
        path.moveTo(0f, horizonY)
        path.lineTo(w * 0.2f, horizonY - 40f)
        path.lineTo(w * 0.4f, horizonY)
        path.lineTo(w * 0.6f, horizonY - 55f)
        path.lineTo(w * 0.8f, horizonY)
        path.lineTo(w.toFloat(), horizonY - 35f)
        path.lineTo(w.toFloat(), horizonY + 10f)
        path.lineTo(0f, horizonY + 10f)
        path.close()
        canvas.drawPath(path, bp)
        // capa frontal
        val path2 = android.graphics.Path()
        path2.moveTo(0f, horizonY + 15f)
        path2.lineTo(w * 0.25f, horizonY - 20f)
        path2.lineTo(w * 0.5f, horizonY + 20f)
        path2.lineTo(w * 0.75f, horizonY - 25f)
        path2.lineTo(w.toFloat(), horizonY + 10f)
        path2.lineTo(w.toFloat(), horizonY + 40f)
        path2.lineTo(0f, horizonY + 40f)
        path2.close()
        canvas.drawPath(path2, fp)
    }

    /** Pinos sencillos. */
    fun drawPines(canvas: Canvas, xs: List<Float>, baseY: Float) {
        val trunk = p("#8B4513"); val leaf = p("#00A800"); val leaf2 = p("#005000")
        xs.forEachIndexed { i, x ->
            val s = 1f + (i % 3) * 0.25f
            canvas.drawRect(x - 3f, baseY - 12f, x + 3f, baseY, trunk)
            for (j in 0 until 3) {
                val yy = baseY - 12f - j * 14f * s
                val half = (16f - j * 4f) * s
                val paint = if (j % 2 == 0) leaf else leaf2
                canvas.drawRect(x - half, yy, x + half, yy + 14f * s, paint)
            }
        }
    }

    /** Cactus para zonas desérticas. */
    fun drawCacti(canvas: Canvas, xs: List<Float>, baseY: Float) {
        val cp = p("#00A800")
        xs.forEach { x ->
            canvas.drawRect(x - 5f, baseY - 38f, x + 5f, baseY, cp)
            canvas.drawRect(x - 15f, baseY - 26f, x - 5f, baseY - 20f, cp)
            canvas.drawRect(x - 15f, baseY - 26f, x - 9f, baseY - 36f, cp)
            canvas.drawRect(x + 5f, baseY - 20f, x + 13f, baseY - 14f, cp)
        }
    }

    /** Silueta de ciudad nocturna con ventanas iluminadas. */
    fun drawCitySkyline(canvas: Canvas, w: Int, baseY: Float) {
        val bp = p("#1A1A2E")
        val wp = p("#F8D878")
        var x = 0f
        var seed = 42L
        while (x < w) {
            seed = seed * 1103515245L + 12345L
            val bw = 50f + abs(seed % 60)
            seed = seed * 1103515245L + 12345L
            val bh = 70f + abs(seed % 110)
            canvas.drawRect(x, baseY - bh, x + bw, baseY, bp)
            // ventanas
            var wy = baseY - bh + 10f
            while (wy < baseY - 10f) {
                var wx = x + 8f
                while (wx < x + bw - 8f) {
                    seed = seed * 1103515245L + 12345L
                    if (abs(seed % 10) < 4) canvas.drawRect(wx, wy, wx + 6f, wy + 8f, wp)
                    wx += 14f
                }
                wy += 16f
            }
            x += bw + 8f
        }
    }

    /** Pared de ladrillos de colores (para fondos arcade). */
    fun drawBrickWall(canvas: Canvas, w: Int, h: Int, alpha: Int = 255) {
        val rowH = 22f
        val brickW = 44f
        val colors = listOf("#8C3A2B", "#A0522D", "#704214", "#5A3A22")
        var row = 0
        var y = 0f
        while (y < h) {
            val offset = if (row % 2 == 0) 0f else brickW / 2f
            var x = -offset
            var col = 0
            while (x < w) {
                val paint = p(colors[(row + col) % colors.size]).apply { this.alpha = alpha }
                canvas.drawRect(x + 2f, y + 2f, x + brickW - 2f, y + rowH - 2f, paint)
                x += brickW; col++
            }
            y += rowH; row++
        }
    }

    /** Piso cuadriculado en perspectiva simple. */
    fun drawCheckerFloor(canvas: Canvas, w: Int, h: Int, topY: Float, c1: String, c2: String) {
        val s = 32f
        var row = 0
        var y = topY
        while (y < h) {
            var x = 0f
            var col = 0
            while (x < w) {
                val paint = p(if ((row + col) % 2 == 0) c1 else c2)
                canvas.drawRect(x, y, x + s, y + s, paint)
                x += s; col++
            }
            y += s; row++
        }
    }

    /** Rayos de reflector para escenarios de espectáculo. */
    fun drawSpotlights(canvas: Canvas, w: Int, h: Int) {
        val sp = p("#F8F8F8").apply { alpha = 26 }
        canvas.drawRect(0f, 0f, w * 0.33f, h.toFloat(), sp)
        canvas.drawRect(w * 0.33f, 0f, w * 0.66f, h.toFloat(), p("#F8D878").apply { alpha = 20 })
        canvas.drawRect(w * 0.66f, 0f, w.toFloat(), h.toFloat(), sp)
        // luces superiores
        val lp = p("#F5C542", true)
        for (i in 0..5) {
            canvas.drawCircle(w * (i + 0.5f) / 6f, 12f, 6f, lp)
        }
    }

    /** Planeta con anillo para fondos espaciales. */
    fun drawPlanet(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        canvas.drawCircle(cx, cy, r, p("#8814A8", true))
        canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, p("#B85AD8", true))
        val ring = p("#5C94FC", true).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawOval(RectF(cx - r * 1.4f, cy - r * 0.35f, cx + r * 1.4f, cy + r * 0.35f), ring)
    }

    /** Marco neón retro alrededor de la pantalla. */
    fun drawNeonFrame(canvas: Canvas, w: Int, h: Int, color: String = "#5C94FC") {
        val np = p(color, true).apply { style = Paint.Style.STROKE; strokeWidth = 5f }
        canvas.drawRect(4f, 4f, w - 4f, h - 4f, np)
    }

    /** Rayos de sol desde una esquina (para estados de éxito/reflejos). */
    fun drawBurstRays(canvas: Canvas, cx: Float, cy: Float, r: Float, color: String = "#F8D878") {
        val rp = p(color).apply { alpha = 40 }
        for (i in 0 until 12) {
            val a = i * (Math.PI / 6.0)
            val path = android.graphics.Path()
            path.moveTo(cx, cy)
            path.lineTo(cx + (r * Math.cos(a - 0.12)).toFloat(), cy + (r * Math.sin(a - 0.12)).toFloat())
            path.lineTo(cx + (r * Math.cos(a + 0.12)).toFloat(), cy + (r * Math.sin(a + 0.12)).toFloat())
            path.close()
            canvas.drawPath(path, rp)
        }
    }
}
