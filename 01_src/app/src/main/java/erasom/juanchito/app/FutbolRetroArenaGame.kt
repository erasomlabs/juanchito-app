package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * FutbolRetroArena: reconstrucción fiel de Soccer 3D (mob.ua, 2008).
 *
 * Mecánica original:
 *  · Fútbol con vista 3D isométrica.
 *  · Controlar jugador, pasar, disparar.
 *  · Torneo con múltiples equipos.
 *  · IA para equipo rival.
 *  · High score.
 */
class FutbolRetroArenaGame : Game {
    override val name = "Futbol Retro Arena"
    override val genre = "Deportes"

    private var w = 0
    private var h = 0

    // Estado
    private enum class State { PLAYING, GOAL, GAME_OVER, MATCH_END }
    private var state = State.PLAYING

    // Jugadores
    private data class Player(val team: Int, var x: Float, var y: Float, var hasBall: Boolean = false)
    private val players = mutableListOf<Player>()
    private var selectedPlayer = 0
    private var ballX = 0f
    private var ballY = 0f
    private var ballVx = 0f
    private var ballVy = 0f
    private var ballHolder = -1  // índice del jugador que tiene la bola

    // Marcador
    private var playerScore = 0
    private var aiScore = 0
    private var matchTime = 0f
    private val matchDuration = 120f  // 2 minutos

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0

    // Animación
    private var goalTimer = 0f
    private var goalText = ""

    // Paints
    private val bgPaint = paintOf("#228B22")
    private val fieldPaint = paintOf("#2E8B2E")
    private val linePaint = paintOf("#FFFFFF")
    private val playerPaint = paintOf("#5C94FC")
    private val aiPaint = paintOf("#D82800")
    private val ballPaint = paintOf("#FFFFFF")
    private val textPaint = paintOf("#F8F8F8").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val titlePaint = paintOf("#F5C542").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallPaint = paintOf("#FFFFFF").apply { textAlign = Paint.Align.CENTER }
    private val hudPaint = paintOf("#F5C542").apply { textAlign = Paint.Align.LEFT; isFakeBoldText = true }
    private val greenPaint = paintOf("#00E676").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val redPaint = paintOf("#FF5252").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    private fun paintOf(hex: String): Paint = Paint().apply {
        setColor(Color.parseColor(hex)); isAntiAlias = true
    }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        reset()
    }

    override fun reset() {
        players.clear()
        // Jugador (azul) - 5 jugadores
        for (i in 0 until 5) {
            players.add(Player(0, w * 0.2f + i * 30f, h * 0.3f + i * 40f))
        }
        // IA (rojo) - 5 jugadores
        for (i in 0 until 5) {
            players.add(Player(1, w * 0.7f + i * 30f, h * 0.3f + i * 40f))
        }
        selectedPlayer = 0
        players[0].hasBall = true
        ballHolder = 0
        ballX = players[0].x
        ballY = players[0].y
        ballVx = 0f
        ballVy = 0f
        playerScore = 0
        aiScore = 0
        matchTime = 0f
        scoreVal = 0
        state = State.PLAYING
        goalTimer = 0f
    }

    override fun update(dt: Float, input: GameInput) {
        if (state != State.PLAYING) {
            if (state == State.GOAL) {
                goalTimer -= dt
                if (goalTimer <= 0f) state = State.PLAYING
                return
            }
            if (state == State.MATCH_END) {
                if (input.start || input.a) reset()
                return
            }
            return
        }

        matchTime += dt
        if (matchTime >= matchDuration) {
            state = State.MATCH_END
            scoreVal = playerScore * 100 - aiScore * 50
            if (scoreVal < 0) scoreVal = 0
            if (scoreVal > best) best = scoreVal
            return
        }

        // Movimiento del jugador seleccionado
        val sel = players[selectedPlayer]
        if (input.left) sel.x = (sel.x - 200f * dt).coerceAtLeast(20f)
        if (input.right) sel.x = (sel.x + 200f * dt).coerceAtMost(w - 20f)
        if (input.up) sel.y = (sel.y - 200f * dt).coerceAtLeast(20f)
        if (input.down) sel.y = (sel.y + 200f * dt).coerceAtMost(h - 20f)

        // Si tiene la bola, la bola sigue al jugador
        if (ballHolder == selectedPlayer) {
            ballX = sel.x
            ballY = sel.y
        } else {
            // Bola libre - mover con física
            ballX += ballVx * dt
            ballY += ballVy * dt
            ballVx *= 0.95f
            ballVy *= 0.95f
        }

        // IA simple: mover jugadores hacia la bola
        for (i in 5 until players.size) {
            val ai = players[i]
            val dx = ballX - ai.x
            val dy = ballY - ai.y
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            if (dist > 5f) {
                ai.x += (dx / dist) * 120f * dt
                ai.y += (dy / dist) * 120f * dt
            }
            // Si la IA tiene la bola, mover hacia la portería
            if (ballHolder == i) {
                val goalX = 20f
                val goalY = h / 2f
                val gdx = goalX - ai.x
                val gdy = goalY - ai.y
                val gdist = kotlin.math.sqrt(gdx * gdx + gdy * gdy)
                if (gdist > 5f) {
                    ai.x += (gdx / gdist) * 150f * dt
                    ai.y += (gdy / gdist) * 150f * dt
                }
            }
        }

        // Cambiar de jugador (B)
        if (input.b) {
            // Encontrar el jugador más cercano a la bola
            var closest = 0
            var closestDist = Float.MAX_VALUE
            for (i in 0 until 5) {
                val p = players[i]
                val dx = p.x - ballX; val dy = p.y - ballY
                val dist = dx * dx + dy * dy
                if (dist < closestDist) { closestDist = dist; closest = i }
            }
            selectedPlayer = closest
            input.b = false
        }

        // Pasar (A cuando tiene bola)
        if (input.a && ballHolder == selectedPlayer) {
            // Pasar al jugador más cercano hacia la portería rival
            var target = 0
            var targetDist = Float.MAX_VALUE
            for (i in 0 until 5) {
                if (i == selectedPlayer) continue
                val p = players[i]
                val dx = (w * 0.8f) - p.x
                val dist = dx * dx + (p.y - ballY) * (p.y - ballY)
                if (dist < targetDist) { targetDist = dist; target = i }
            }
            val tp = players[target]
            ballVx = (tp.x - ballX) * 3f
            ballVy = (tp.y - ballY) * 3f
            ballHolder = -1
            input.a = false
        }

        // Disparar (A cuando tiene bola y está cerca de la portería)
        if (input.start && ballHolder == selectedPlayer) {
            val sel2 = players[selectedPlayer]
            if (sel2.x > w * 0.6f) {
                // Disparar a portería
                ballVx = -500f
                ballVy = (h / 2f - ballY) * 2f
                ballHolder = -1
            }
            input.start = false
        }

        // Verificar si la bola entró en la portería
        // Portería jugador (izquierda)
        if (ballX < 20f && ballY in (h / 2f - 40f)..(h / 2f + 40f)) {
            aiScore++
            state = State.GOAL
            goalTimer = 2f
            goalText = "¡GOL DEL RIVAL!"
            // Reset
            players[0].hasBall = true
            ballHolder = 0
            ballX = players[0].x
            ballY = players[0].y
        }
        // Portería IA (derecha)
        if (ballX > w - 20f && ballY in (h / 2f - 40f)..(h / 2f + 40f)) {
            playerScore++
            scoreVal = playerScore * 100
            if (scoreVal > best) best = scoreVal
            state = State.GOAL
            goalTimer = 2f
            goalText = "¡GOL!"
            players[0].hasBall = true
            ballHolder = 0
            ballX = players[0].x
            ballY = players[0].y
        }

        // IA puede marcar (si tiene la bola y está cerca de la portería)
        for (i in 5 until players.size) {
            if (ballHolder == i) {
                val ai = players[i]
                if (ai.x < w * 0.3f) {
                    ballVx = 500f
                    ballVy = (h / 2f - ballY) * 2f
                    ballHolder = -1
                }
            }
        }

        // Verificar si la IA recogió la bola
        if (ballHolder == -1) {
            for (i in 0 until players.size) {
                val p = players[i]
                val dx = p.x - ballX; val dy = p.y - ballY
                if (dx * dx + dy * dy < 400f) {
                    ballHolder = i
                    ballVx = 0f
                    ballVy = 0f
                    break
                }
            }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#228B22"))

        // Campo
        val fieldPaint = paintOf("#2E8B2E")
        canvas.drawRect(20f, 20f, w - 20f, h - 20f, fieldPaint)

        // Líneas del campo
        val linePaint = paintOf("#FFFFFF")
        linePaint.strokeWidth = 2f
        linePaint.style = Paint.Style.STROKE
        canvas.drawRect(20f, 20f, w - 20f, h - 20f, linePaint)
        canvas.drawLine(w / 2f, 20f, w / 2f, h - 20f, linePaint)
        // Círculo central
        canvas.drawCircle(w / 2f, h / 2f, 40f, linePaint)
        // Porterías
        canvas.drawRect(15f, h / 2f - 40f, 25f, h / 2f + 40f, linePaint)
        canvas.drawRect(w - 25f, h / 2f - 40f, w - 15f, h / 2f + 40f, linePaint)

        // Jugadores
        for ((i, p) in players.withIndex()) {
            val paint = if (p.team == 0) playerPaint else aiPaint
            canvas.drawCircle(p.x, p.y, 12f, paint)
            // Indicador de selección
            if (i == selectedPlayer && p.team == 0) {
                val selPaint = paintOf("#F5C542")
                selPaint.style = Paint.Style.STROKE
                selPaint.strokeWidth = 2f
                canvas.drawCircle(p.x, p.y, 16f, selPaint)
            }
            // Indicador de bola
            if (ballHolder == i) {
                val ballInd = paintOf("#FFFFFF")
                ballInd.textSize = 10f
                ballInd.textAlign = Paint.Align.CENTER
                canvas.drawText("●", p.x, p.y - 18f, ballInd)
            }
        }

        // Bola
        canvas.drawCircle(ballX, ballY, 6f, ballPaint)
        val shine = paintOf("#FFFFFF")
        shine.alpha = 200
        canvas.drawCircle(ballX - 2f, ballY - 2f, 2f, shine)

        // HUD
        hudPaint.textSize = 24f
        canvas.drawText("$playerScore - $aiScore", w / 2f, 20f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT
        smallPaint.textSize = 14f
        canvas.drawText("TIEMPO: ${(matchDuration - matchTime).toInt()}s", w / 2f, 45f, smallPaint.apply { textAlign = Paint.Align.CENTER })
        smallPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("SCORE: $scoreVal  ·  BEST: $best", w / 2f, h - 15f, smallPaint)

        // Overlay de gol
        if (state == State.GOAL) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 120; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 48f
            canvas.drawText(goalText, w / 2f, h / 2f, titlePaint)
        }

        // Overlay de fin de partido
        if (state == State.MATCH_END) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 42f
            canvas.drawText("FIN DEL PARTIDO", w / 2f, h / 2f - 40f, titlePaint)
            smallPaint.textSize = 28f
            canvas.drawText("$playerScore - $aiScore", w / 2f, h / 2f + 10f, smallPaint)
            val resultText = when {
                playerScore > aiScore -> "¡VICTORIA!"
                playerScore < aiScore -> "DERROTA"
                else -> "¡EMPATE!"
            }
            smallPaint.textSize = 22f
            canvas.drawText(resultText, w / 2f, h / 2f + 45f, smallPaint)
            if (scoreVal >= best && scoreVal > 0) {
                titlePaint.textSize = 22f
                canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 80f, titlePaint)
            }
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 120f, greenPaint)
        }

        // Instrucciones
        if (state == State.PLAYING) {
            smallPaint.textSize = 12f
            smallPaint.color = Color.parseColor("#FFFFFF")
            canvas.drawText("D-pad: mover  ·  B: cambiar jugador  ·  A: pasar  ·  START: disparar", w / 2f, h - 10f, smallPaint)
        }
    }

    override val isGameOver: Boolean get() = state == State.MATCH_END
}
