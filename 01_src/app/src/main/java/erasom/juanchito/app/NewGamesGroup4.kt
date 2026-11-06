package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

// 13. Pixel Charades: Adivinanza de patrones de color
class PixelCharadesGame : Game {
    override val name = "Pixel Charades"
    override val genre = "Trivia"
    private var w = 0; private var h = 0
    private var targetIndex = 0
    private var selectedIndex = 0
    private val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
    private val names = listOf("ROJO", "VERDE", "AZUL", "AMARILLO")
    override var isGameOver = false
    override var score = 0
    private val paint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private var moved = false

    override fun init(width: Int, height: Int) { w = width; h = height }
    override fun reset() {
        targetIndex = Random.nextInt(4)
        selectedIndex = 0
        isGameOver = false; score = 0
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) return
        if (!input.left && !input.right && !input.a) { moved = false; return }
        if (moved) return

        if (input.left && selectedIndex > 0) { selectedIndex--; moved = true }
        if (input.right && selectedIndex < 3) { selectedIndex++; moved = true }

        if (input.a) {
            moved = true
            if (selectedIndex == targetIndex) {
                score += 100
                targetIndex = Random.nextInt(4)
            } else {
                isGameOver = true
            }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#000000"))
        paint.color = Color.WHITE
        paint.textSize = 28f
        canvas.drawText("Elige: ${names[targetIndex]}", w / 2f, h * 0.28f, paint)

        val size = 60f
        val startX = (w - size * 4) / 2f
        for (i in 0..3) {
            paint.color = colors[i]
            val r = RectF(startX + i * size + 6, h / 2f - size / 2f, startX + (i + 1) * size - 6, h / 2f + size / 2f)
            canvas.drawRoundRect(r, 8f, 8f, paint)
            if (i == selectedIndex) {
                val p2 = Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 4f }
                canvas.drawRoundRect(r, 8f, 8f, p2)
            }
        }
    }
}

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
        canvas.drawColor(Color.parseColor("#000000"))
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
        canvas.drawColor(Color.parseColor("#000000"))
        // Nave
        paint.color = Color.parseColor("#5C94FC")
        canvas.drawRoundRect(RectF(shipX - 18f, h - 50f, shipX + 18f, h - 30f), 4f, 4f, paint)

        // Balas
        paint.color = Color.parseColor("#F8D878")
        for ((bx, by) in bullets) {
            canvas.drawRect(bx - 3f, by - 8f, bx + 3f, by, paint)
        }

        // Alienígenas
        paint.color = Color.parseColor("#D82800")
        for (a in aliens) {
            canvas.drawRoundRect(a, 4f, 4f, paint)
        }
    }
}
