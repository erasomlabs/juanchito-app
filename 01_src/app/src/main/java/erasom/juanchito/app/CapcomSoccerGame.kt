package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class CapcomSoccerGame : Game {
    override val name = "Arcade Soccer"
    override val genre = "Deportes"
    private var w = 0; private var h = 0
    private val players = mutableListOf<Player>()
    private var ballX = 0f; private var ballY = 0f
    private var ballVx = 0f; private var ballVy = 0f
    private var scoreP = 0; private var scoreAI = 0
    private var timeLeft = 180f
    private var activePlayer = 0
    override var isGameOver = false
    override var score = 0
    private var highScore = 0

    private data class Player(var x: Float, var y: Float, val team: Int) // 0 player, 1 ai

    private val playerPaint = Paint().apply { color = Color.parseColor("#FECA57"); isAntiAlias = true }
    private val aiPaint = Paint().apply { color = Color.parseColor("#FF6B6B"); isAntiAlias = true }
    private val ballPaint = Paint().apply { color = Color.parseColor("#F5F5F5"); isAntiAlias = true }
    private val fieldPaint = Paint().apply { color = Color.parseColor("#2D3748"); isAntiAlias = true }
    private val textPaint = Paint().apply { color = Color.parseColor("#F5F5F5"); textSize = 28f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#48DBFB"); textSize = 20f; isAntiAlias = true; textAlign = Paint.Align.CENTER }

    override fun init(width: Int, height: Int) { w = width; h = height; reset() }

    override fun reset() {
        players.clear()
        // player team (bottom)
        for (i in 0 until 5) players.add(Player(w * (0.1f + i * 0.2f), h * 0.75f, 0))
        // ai team (top)
        for (i in 0 until 5) players.add(Player(w * (0.1f + i * 0.2f), h * 0.25f, 1))
        ballX = w/2f; ballY = h/2f; ballVx = 0f; ballVy = 0f
        scoreP = 0; scoreAI = 0; timeLeft = 180f; activePlayer = 0
        isGameOver = false; score = 0
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) { if (input.start) reset(); return }
        timeLeft -= dt
        if (timeLeft <= 0f) { isGameOver = true; score = scoreP * 100; if (score > highScore) highScore = score; return }
        // move active player
        val p = players[activePlayer]
        if (input.up) p.y -= 200f * dt
        if (input.down) p.y += 200f * dt
        if (input.left) p.x -= 200f * dt
        if (input.right) p.x += 200f * dt
        p.x = p.x.coerceIn(20f, w - 20f); p.y = p.y.coerceIn(h*0.5f, h - 20f)
        // change player
        if (input.select) { activePlayer = (activePlayer + 1) % 5; input.select = false }
        // pass
        if (input.a) {
            val dx = ballX - p.x; val dy = ballY - p.y
            if (dx*dx + dy*dy < 40f*40f) {
                ballVx = ((if (input.right) 1 else 0) - (if (input.left) 1 else 0)) * 300f
                ballVy = ((if (input.down) 1 else 0) - (if (input.up) 1 else 0)) * 300f
                if (ballVx == 0f && ballVy == 0f) ballVy = -400f
            }
            input.a = false
        }
        // shoot
        if (input.b) {
            val dx = ballX - p.x; val dy = ballY - p.y
            if (dx*dx + dy*dy < 40f*40f) {
                ballVx = (if (ballX < w/2f) 1 else -1) * 0f
                ballVy = -600f
            }
            input.b = false
        }
        // ball physics
        ballX += ballVx * dt; ballY += ballVy * dt
        ballVx *= 0.98f; ballVy *= 0.98f
        ballX = ballX.coerceIn(10f, w - 10f)
        if (ballY < 10f) { ballY = 10f; ballVy = -ballVy }
        if (ballY > h - 10f) { ballY = h - 10f; ballVy = -ballVy }
        // AI movement
        for (pl in players.filter { it.team == 1 }) {
            val dx = ballX - pl.x; val dy = ballY - pl.y
            if (dx*dx + dy*dy > 25f*25f) {
                pl.x += (dx).coerceIn(-1f, 1f) * 120f * dt
                pl.y += (dy).coerceIn(-1f, 1f) * 120f * dt
            }
            pl.x = pl.x.coerceIn(20f, w - 20f); pl.y = pl.y.coerceIn(20f, h*0.5f)
        }
        // AI kick if near ball
        for (pl in players.filter { it.team == 1 }) {
            val dx = ballX - pl.x; val dy = ballY - pl.y
            if (dx*dx + dy*dy < 30f*30f && Random.nextFloat() < 0.02f) {
                ballVx = (if (ballX > w/2f) 1 else -1) * 200f
                ballVy = 400f
            }
        }
        // goals
        if (ballY < 20f && ballX in w*0.4f..w*0.6f) { scoreP++; score = scoreP * 100; resetBall(); }
        if (ballY > h - 20f && ballX in w*0.4f..w*0.6f) { scoreAI++; resetBall(); }
    }

    private fun resetBall() { ballX = w/2f; ballY = h/2f; ballVx = 0f; ballVy = 0f }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))
        // field
        canvas.drawRect(40f, 40f, w - 40f, h - 40f, fieldPaint)
        // goals
        val goalPaint = Paint().apply { color = Color.parseColor("#48DBFB"); strokeWidth = 4f }
        canvas.drawLine(w*0.4f, 40f, w*0.6f, 40f, goalPaint)
        canvas.drawLine(w*0.4f, h - 40f, w*0.6f, h - 40f, goalPaint)
        // midline
        val lp = Paint().apply { color = Color.parseColor("#F5F5F5"); strokeWidth = 2f; alpha = 80 }
        canvas.drawLine(40f, h/2f, w - 40f, h/2f, lp)
        // players
        for (p in players) {
            val paint = if (p.team == 0) playerPaint else aiPaint
            canvas.drawCircle(p.x, p.y, 14f, paint)
        }
        // ball
        canvas.drawCircle(ballX, ballY, 8f, ballPaint)
        // HUD
        textPaint.textSize = 32f
        canvas.drawText("$scoreP - $scoreAI", w/2f, 36f, textPaint)
        smallPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Tiempo: ${timeLeft.toInt()}s", 20f, h - 10f, smallPaint)
        smallPaint.textAlign = Paint.Align.CENTER
        if (isGameOver) {
            val msg = if (scoreP > scoreAI) "¡GANASTE!" else "PERDISTE"
            val col = if (scoreP > scoreAI) Color.parseColor("#55EFC4") else Color.parseColor("#FF6B6B")
            canvas.drawText(msg, w/2f, h/2f, textPaint.apply { textSize = 56f; color = col })
            canvas.drawText("Pulsa START", w/2f, h/2f + 60f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}