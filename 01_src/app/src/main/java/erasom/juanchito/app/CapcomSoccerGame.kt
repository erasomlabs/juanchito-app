package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class CapcomSoccerGame : Game {
    override val name = "Fútbol Retro Arena"
    override val genre = "Deportes"
    private var w = 0; private var h = 0
    private val players = mutableListOf<Player>()
    private var ballX = 0f; private var ballY = 0f
    private var ballVx = 0f; private var ballVy = 0f
    private var scoreP = 0; private var scoreAI = 0
    private var timeLeft = 180f
    private var activePlayer = 0
    private var hasBall = -1 // index of player with ball, -1 none
    override var isGameOver = false
    override var score = 0
    private var highScore = 0
    // Estados: 0 = Prólogo Copa Retro, 1 = Gameplay
    private var gameState = 0
    private var goalCelebration = 0f
    private var lastGoalMsg = ""

    private data class Player(var x: Float, var y: Float, val team: Int, var hasBall: Boolean)

    private val playerPaint = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
    private val aiPaint = Paint().apply { color = Color.parseColor("#D82800"); isAntiAlias = false }
    private val ballPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); isAntiAlias = false }
    private val fieldPaint = Paint().apply { color = Color.parseColor("#00A800"); isAntiAlias = false }
    private val linePaint = Paint().apply { color = Color.parseColor("#F8F8F8"); strokeWidth = 2f; isAntiAlias = false }
    private val goalPaint = Paint().apply { color = Color.parseColor("#F8D878"); strokeWidth = 4f; isAntiAlias = false }
    private val textPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 14f; isAntiAlias = false; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 11f; isAntiAlias = false; textAlign = Paint.Align.CENTER }

    override fun init(width: Int, height: Int) { w = width; h = height; reset() }

    override fun reset() {
        players.clear()
        for (i in 0 until 5) players.add(Player(w * (0.15f + i * 0.17f), h * 0.72f, 0, false))
        for (i in 0 until 5) players.add(Player(w * (0.15f + i * 0.17f), h * 0.28f, 1, false))
        ballX = w / 2f; ballY = h / 2f; ballVx = 0f; ballVy = 0f
        scoreP = 0; scoreAI = 0; timeLeft = 180f; activePlayer = 0; hasBall = -1
        isGameOver = false; score = 0
        gameState = 0; goalCelebration = 0f; lastGoalMsg = ""
    }

    override fun update(dt: Float, input: GameInput) {
        if (gameState == 0) {
            if (input.a || input.start) gameState = 1
            input.a = false; input.start = false
            return
        }
        if (isGameOver) { if (input.start) reset(); return }
        goalCelebration = (goalCelebration - dt).coerceAtLeast(0f)
        timeLeft -= dt
        if (timeLeft <= 0f) { isGameOver = true; score = scoreP * 100; if (score > highScore) highScore = score; return }
        // move active player
        val p = players[activePlayer]
        if (input.up) p.y -= 220f * dt
        if (input.down) p.y += 220f * dt
        if (input.left) p.x -= 220f * dt
        if (input.right) p.x += 220f * dt
        p.x = p.x.coerceIn(20f, w - 20f); p.y = p.y.coerceIn(h * 0.5f, h - 20f)
        if (input.select) { activePlayer = (activePlayer + 1) % 5; input.select = false }
        // claim ball if near
        for (i in 0 until 5) {
            val pl = players[i]
            val dx = ballX - pl.x; val dy = ballY - pl.y
            if (dx * dx + dy * dy < 30f * 30f && hasBall == -1) { hasBall = i; pl.hasBall = true }
        }
        // pass (A)
        if (input.a) {
            if (hasBall in 0..4) {
                val pl = players[hasBall]
                pl.hasBall = false
                var pvx = 0f; var pvy = 0f
                if (input.up) pvy = -1f
                if (input.down) pvy = 1f
                if (input.left) pvx = -1f
                if (input.right) pvx = 1f
                if (pvx == 0f && pvy == 0f) { pvx = if (ballX < w / 2f) 1f else -1f; pvy = -1f }
                val len = kotlin.math.sqrt(pvx * pvx + pvy * pvy)
                ballVx = pvx / len * 400f; ballVy = pvy / len * 400f
                hasBall = -1
            }
            input.a = false
        }
        // shoot (B)
        if (input.b) {
            if (hasBall in 0..4) {
                val pl = players[hasBall]
                pl.hasBall = false
                ballVx = 0f; ballVy = -700f
                hasBall = -1
            }
            input.b = false
        }
        // if hasBall, ball follows player
        if (hasBall in 0..4) {
            val pl = players[hasBall]
            ballX = pl.x; ballY = pl.y - 12f
            ballVx = 0f; ballVy = 0f
        } else {
            ballX += ballVx * dt; ballY += ballVy * dt
            ballVx *= 0.97f; ballVy *= 0.97f
            ballX = ballX.coerceIn(10f, w - 10f)
            if (ballY < 10f) { ballY = 10f; ballVy = -ballVy }
            if (ballY > h - 10f) { ballY = h - 10f; ballVy = -ballVy }
        }
        // AI movement
        for (i in 5 until 10) {
            val pl = players[i]
            val dx = ballX - pl.x; val dy = ballY - pl.y
            if (dx * dx + dy * dy > 20f * 20f) {
                pl.x += (dx).coerceIn(-1f, 1f) * 130f * dt
                pl.y += (dy).coerceIn(-1f, 1f) * 130f * dt
            }
            pl.x = pl.x.coerceIn(20f, w - 20f); pl.y = pl.y.coerceIn(20f, h * 0.5f)
        }
        // AI claim ball
        for (i in 5 until 10) {
            val pl = players[i]
            val dx = ballX - pl.x; val dy = ballY - pl.y
            if (dx * dx + dy * dy < 25f * 25f && hasBall == -1) { hasBall = i; pl.hasBall = true }
        }
        // AI shoot/pass
        if (hasBall in 5..9) {
            val pl = players[hasBall]
            if (Random.nextFloat() < 0.015f) {
                pl.hasBall = false
                if (pl.y > h * 0.35f || Random.nextFloat() < 0.3f) {
                    ballVx = (if (ballX > w / 2f) 1 else -1) * 150f
                    ballVy = 500f
                } else {
                    val mate = players[5 + Random.nextInt(5)]
                    val dx = mate.x - pl.x; val dy = mate.y - pl.y
                    val len = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (len > 0f) { ballVx = dx / len * 350f; ballVy = dy / len * 350f }
                }
                hasBall = -1
            }
        }
        // goals
        if (ballY < 22f && ballX in w * 0.4f..w * 0.6f) {
            scoreP++; score = scoreP * 100
            goalCelebration = 2f; lastGoalMsg = "¡GOL de Valdoria FC! ¡La grada estalla!"
            resetBall()
        }
        if (ballY > h - 22f && ballX in w * 0.4f..w * 0.6f) {
            scoreAI++; goalCelebration = 2f; lastGoalMsg = "Gol de la Selección Furia... silencio en la tribuna"
            resetBall()
        }
    }

    private fun resetBall() { ballX = w / 2f; ballY = h / 2f; ballVx = 0f; ballVy = 0f; hasBall = -1; for (p in players) p.hasBall = false }

    override fun draw(canvas: Canvas) {
        if (gameState == 0) {
            // Prólogo: la historia de la Copa Retro
            NesDialogueRenderer.drawPrologueScreen(
                canvas,
                w,
                h,
                gameTitle = "FÚTBOL RETRO ARENA",
                missionTitle = "COPA RETRO 1994: LA FINAL DE VALDORIA",
                prologueLines = listOf(
                    "La Copa Retro reúne a las selecciones más",
                    "acrobáticas del planeta en el Estadio Nacional.",
                    "",
                    "Pixel capitanea al Valdoria FC, el equipo",
                    "de la estrella dorada. Enfrente espera la",
                    "Selección Furia, famosa por sus chilenas",
                    "imposibles y su defensa de hierro.",
                    "",
                    "Tres minutos por tiempo, pases de toque",
                    "rápido y remates de volea. Solo uno levantará",
                    "el trofeo ante 60.000 hinchas.",
                    "",
                    "¡Que ruede el balón, capitán!"
                ),
                primaryColor = Color.parseColor("#007800")
            )
            return
        }
        // sky + field
        canvas.drawColor(Color.parseColor("#5C94FC"))
        canvas.drawRect(0f, h * 0.08f, w.toFloat(), h.toFloat(), fieldPaint)
        // grass stripes
        val stripePaint = Paint().apply { color = Color.parseColor("#008000"); isAntiAlias = false }
        var yy = h * 0.08f
        while (yy < h) { canvas.drawRect(0f, yy, w.toFloat(), yy + 8f, stripePaint); yy += 16f }
        // field lines
        canvas.drawRect(40f, h * 0.08f, w - 40f, h.toFloat(), Paint().apply { color = Color.parseColor("#00A800"); style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = false })
        canvas.drawLine(40f, h / 2f, w - 40f, h / 2f, linePaint)
        // center circle
        val cp = Paint().apply { color = Color.parseColor("#F8F8F8"); style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = false }
        canvas.drawCircle(w / 2f, h / 2f, 40f, cp)
        // goals
        canvas.drawLine(w * 0.4f, h * 0.08f, w * 0.6f, h * 0.08f, goalPaint)
        canvas.drawLine(w * 0.4f, h - 8f, w * 0.6f, h - 8f, goalPaint)
        // goal posts
        canvas.drawRect(w * 0.4f - 4f, h * 0.08f - 12f, w * 0.4f, h * 0.08f, goalPaint)
        canvas.drawRect(w * 0.6f, h * 0.08f - 12f, w * 0.6f + 4f, h * 0.08f, goalPaint)
        canvas.drawRect(w * 0.4f - 4f, h - 8f, w * 0.4f, h + 4f, goalPaint)
        canvas.drawRect(w * 0.6f, h - 8f, w * 0.6f + 4f, h + 4f, goalPaint)
        // players
        for (p in players) {
            PixelSprite.draw(canvas, p.x, p.y, 24f, facingRight = p.team == 0, hatColor = if (p.team == 0) 0xD4A017 else 0xD82800)
            if (p.hasBall) {
                val bp = Paint().apply { color = Color.parseColor("#F8F8F8"); isAntiAlias = false }
                canvas.drawCircle(p.x, p.y - 14f, 5f, bp)
            }
        }
        // ball
        canvas.drawCircle(ballX, ballY, 7f, ballPaint)
        // HUD
        textPaint.textSize = 16f
        canvas.drawText("$scoreP - $scoreAI", w / 2f, 20f, textPaint)
        smallPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Tiempo: ${timeLeft.toInt()}s", 8f, 14f, smallPaint)
        canvas.drawText("A: Pase  B: Tiro  SELECT: Cambiar", 8f, h - 8f, smallPaint)
        smallPaint.textAlign = Paint.Align.CENTER
        // Relato en vivo del narrador
        if (goalCelebration > 0f) {
            val bannerPaint = Paint().apply { color = Color.parseColor("#000000"); alpha = 170; isAntiAlias = false }
            canvas.drawRect(0f, h * 0.35f, w.toFloat(), h * 0.45f, bannerPaint)
            textPaint.textSize = 14f; textPaint.color = Color.parseColor("#F8D878")
            canvas.drawText(lastGoalMsg, w / 2f, h * 0.41f, textPaint)
            textPaint.color = Color.parseColor("#F8F8F8")
        }
        // Resultado final con cierre de historia
        if (isGameOver) {
            val msg = if (scoreP > scoreAI) "¡GANASTE!" else if (scoreP < scoreAI) "PERDISTE" else "EMPATE"
            val col = if (scoreP > scoreAI) Color.parseColor("#00A800") else if (scoreP < scoreAI) Color.parseColor("#D82800") else Color.parseColor("#F8D878")
            textPaint.textSize = 32f; textPaint.color = col
            canvas.drawText(msg, w / 2f, h / 2f, textPaint)
            textPaint.textSize = 11f; textPaint.color = Color.parseColor("#F8F8F8")
            val story = when {
                scoreP > scoreAI -> "Pixel levanta la Copa Retro entre confeti dorado"
                scoreP < scoreAI -> "La Selección Furia se lleva el trofeo... habrá revancha"
                else -> "Empate épico: la Copa se queda en casa hasta el próximo torneo"
            }
            canvas.drawText(story, w / 2f, h / 2f + 20f, textPaint)
            textPaint.textSize = 12f
            canvas.drawText("Pulsa START", w / 2f, h / 2f + 42f, smallPaint.apply { textSize = 14f; color = Color.parseColor("#F8F8F8"); textAlign = Paint.Align.CENTER })
        }
    }
}