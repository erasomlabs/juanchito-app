package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

// 14. Pixel Tictactoe: Tres en raya contra IA básica
class PixelTictactoeGame : Game {
    override val name = "Pixel Tictactoe"
    override val genre = "Estrategia"
    private var w = 0; private var h = 0
    private var board = IntArray(9) // 0 vacio, 1 jugador, 2 IA
    private var cur = 4
    override var isGameOver = false
    override var score = 0
    private val paint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private var moved = false

    override fun init(width: Int, height: Int) { w = width; h = height }
    override fun reset() {
        board = IntArray(9)
        cur = 4; isGameOver = false; score = 0
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) return
        if (!input.left && !input.right && !input.up && !input.down && !input.a) { moved = false; return }
        if (moved) return

        if (input.left && cur % 3 > 0) { cur--; moved = true }
        if (input.right && cur % 3 < 2) { cur++; moved = true }
        if (input.up && cur >= 3) { cur -= 3; moved = true }
        if (input.down && cur < 6) { cur += 3; moved = true }

        if (input.a && board[cur] == 0) {
            moved = true
            board[cur] = 1
            if (checkWin(1)) { score += 200; isGameOver = true; return }
            val free = board.indices.filter { board[it] == 0 }
            if (free.isNotEmpty()) {
                val aiPick = free.random()
                board[aiPick] = 2
                if (checkWin(2)) { isGameOver = true }
            } else {
                isGameOver = true
            }
        }
    }

    private fun checkWin(p: Int): Boolean {
        val lines = listOf(
            listOf(0,1,2), listOf(3,4,5), listOf(6,7,8),
            listOf(0,3,6), listOf(1,4,7), listOf(2,5,8),
            listOf(0,4,8), listOf(2,4,6)
        )
        return lines.any { l -> l.all { board[it] == p } }
    }

    override fun draw(canvas: Canvas) {
        // Parque: tres en raya al aire libre
        NesScenery.drawSkyBands(canvas, w, h.toFloat(), "#5C94FC", "#A8D8FC", 5)
        NesScenery.drawClouds(canvas, w, h * 0.25f)
        NesScenery.drawHills(canvas, w, h * 0.85f, "#00A800", "#008800")
        val size = 65f
        val startX = (w - size * 3) / 2f
        val startY = (h - size * 3) / 2f
        paint.textSize = 36f

        for (i in 0..8) {
            val r = i / 3; val c = i % 3
            val rect = RectF(startX + c * size + 4, startY + r * size + 4, startX + (c + 1) * size - 4, startY + (r + 1) * size - 4)
            paint.color = if (i == cur) Color.parseColor("#F8D878") else Color.parseColor("#2D3748")
            canvas.drawRoundRect(rect, 6f, 6f, paint)

            if (board[i] == 1) {
                paint.color = Color.parseColor("#5C94FC")
                canvas.drawText("X", rect.centerX(), rect.centerY() + 12, paint)
            } else if (board[i] == 2) {
                paint.color = Color.parseColor("#D82800")
                canvas.drawText("O", rect.centerX(), rect.centerY() + 12, paint)
            }
        }
    }
}

// 15. Pixel Invaders: Arcade shoot 'em up espacial
class PixelInvadersGame : Game {
    override val name = "Pixel Invaders"
    override val genre = "Acción"
    private var w = 0; private var h = 0
    private var shipX = 0f
    private val bullets = mutableListOf<Pair<Float, Float>>()
    private val aliens = mutableListOf<RectF>()
    override var isGameOver = false
    override var score = 0
    private var reload = 0f
    private val paint = Paint().apply { isAntiAlias = true }

    override fun init(width: Int, height: Int) { w = width; h = height }
    override fun reset() {
        shipX = w / 2f; bullets.clear(); aliens.clear()
        isGameOver = false; score = 0; reload = 0f
        for (r in 0..2) {
            for (c in 0..6) {
                aliens.add(RectF(60f + c * 50f, 40f + r * 35f, 95f + c * 50f, 65f + r * 35f))
            }
        }
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) return
        val speed = 320f * dt
        if (input.left) shipX = (shipX - speed).coerceAtLeast(30f)
        if (input.right) shipX = (shipX + speed).coerceAtMost(w - 30f)

        reload -= dt
        if (input.a && reload <= 0f) {
            bullets.add(shipX to h - 60f)
            reload = 0.25f
        }

        val bIt = bullets.listIterator()
        while (bIt.hasNext()) {
            val (bx, by) = bIt.next()
            val ny = by - 450f * dt
            if (ny < 0f) {
                bIt.remove()
            } else {
                bIt.set(bx to ny)
                val aIt = aliens.iterator()
                while (aIt.hasNext()) {
                    val a = aIt.next()
                    if (a.contains(bx, ny)) {
                        aIt.remove()
                        bIt.remove()
                        score += 50
                        if (aliens.isEmpty()) reset()
                        break
                    }
                }
            }
        }

        // Bajada de invasores lenta
        for (a in aliens) {
            a.top += 12f * dt
            a.bottom += 12f * dt
            if (a.bottom >= h - 70f) isGameOver = true
        }
    }

    override fun draw(canvas: Canvas) {
        // Espacio profundo: estrellas, planeta y ciudad en órbita
        NesScenery.drawSkyBands(canvas, w, h.toFloat(), "#050514", "#101038", 5)
        NesScenery.drawStars(canvas, w, h, 60)
        NesScenery.drawPlanet(canvas, w - 70f, 90f, 40f)
        NesScenery.drawCitySkyline(canvas, w, h.toFloat())
        // Nave de Pixel: casco, alas, cabina y cañón
        val hull = Paint().apply { color = Color.parseColor("#5C94FC"); isAntiAlias = false }
        val hullDark = Paint().apply { color = Color.parseColor("#2A5AA8"); isAntiAlias = false }
        val glass = Paint().apply { color = Color.parseColor("#A8D8FC"); isAntiAlias = false }
        // alas
        canvas.drawRect(shipX - 26f, h - 36f, shipX - 18f, h - 30f, hullDark)
        canvas.drawRect(shipX + 18f, h - 36f, shipX + 26f, h - 30f, hullDark)
        // casco
        canvas.drawRoundRect(RectF(shipX - 18f, h - 50f, shipX + 18f, h - 30f), 4f, 4f, hull)
        // franja inferior oscura
        canvas.drawRect(shipX - 18f, h - 34f, shipX + 18f, h - 30f, hullDark)
        // cañón central
        canvas.drawRect(shipX - 3f, h - 58f, shipX + 3f, h - 50f, hull)
        // cabina de cristal
        canvas.drawRect(shipX - 6f, h - 47f, shipX + 6f, h - 40f, glass)
        // piloto asomado
        PixelSprite.draw(canvas, shipX, h - 46f, 12f, facingRight = true, hatColor = 0x5C94FC)

        // Balas: píldoras con brillo
        paint.color = Color.parseColor("#F8D878")
        for ((bx, by) in bullets) {
            canvas.drawRect(bx - 3f, by - 8f, bx + 3f, by, paint)
            paint.color = Color.parseColor("#F8F8F8")
            canvas.drawRect(bx - 1f, by - 6f, bx + 1f, by - 2f, paint)
            paint.color = Color.parseColor("#F8D878")
        }

        // Alienígenas: tres tipos con antenas, ojos y patas (estilo NES)
        for ((idx, a) in aliens.withIndex()) {
            val row = idx / 7
            val cx = a.centerX(); val cy = a.centerY()
            val body = when (row) {
                0 -> Color.parseColor("#D82800")   // fila superior: calamar rojo
                1 -> Color.parseColor("#8814A8")   // media: pulpo púrpura
                else -> Color.parseColor("#00A800") // baja: cangrejo verde
            }
            val bodyP = Paint().apply { color = body; isAntiAlias = false }
            val eyeP = Paint().apply { color = Color.parseColor("#F8F8F8"); isAntiAlias = false }
            val pupilP = Paint().apply { color = Color.parseColor("#000000"); isAntiAlias = false }
            val limbP = Paint().apply { color = body; isAntiAlias = false }
            val wobble = ((idx % 2 == 0) && (score / 50) % 2 == 0)
            // antenas
            canvas.drawRect(cx - 8f, a.top - 6f, cx - 6f, a.top, bodyP)
            canvas.drawRect(cx + 6f, a.top - 6f, cx + 8f, a.top, bodyP)
            canvas.drawCircle(cx - 7f, a.top - 7f, 2f, eyeP)
            canvas.drawCircle(cx + 7f, a.top - 7f, 2f, eyeP)
            // cuerpo
            canvas.drawRoundRect(a, 3f, 3f, bodyP)
            // ojos
            canvas.drawCircle(cx - 5f, cy - 2f, 3.5f, eyeP)
            canvas.drawCircle(cx + 5f, cy - 2f, 3.5f, eyeP)
            canvas.drawCircle(cx - 5f, cy - 2f, 1.5f, pupilP)
            canvas.drawCircle(cx + 5f, cy - 2f, 1.5f, pupilP)
            // boca
            canvas.drawRect(cx - 4f, cy + 5f, cx + 4f, cy + 7f, pupilP)
            // patas laterales alternadas
            if (wobble) {
                canvas.drawRect(a.left - 5f, cy - 2f, a.left, cy + 2f, limbP)
                canvas.drawRect(a.right, cy - 2f, a.right + 5f, cy + 2f, limbP)
            } else {
                canvas.drawRect(a.left - 5f, cy + 3f, a.left, cy + 7f, limbP)
                canvas.drawRect(a.right, cy + 3f, a.right + 5f, cy + 7f, limbP)
            }
        }
    }
}
