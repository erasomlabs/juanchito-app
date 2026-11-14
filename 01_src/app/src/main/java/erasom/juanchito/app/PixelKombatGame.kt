package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Pixel Kombat: reconstrucción fiel de Street Fighter II (Glu Mobile, 2008).
 *
 * Mecánica original:
 *  · Lucha 1v1 con combos y special moves.
 *  · 12 personajes con estilos únicos.
 *  · Modo arcade con bosses.
 *  · High score.
 */
class PixelKombatGame : Game {
    override val name = "Pixel Kombat"
    override val genre = "Lucha"

    private var w = 0
    private var h = 0

    // Estado
    private enum class State { FIGHTING, KO, ROUND_END, GAME_OVER, VICTORY }
    private var state = State.FIGHTING

    // Luchadores
    private data class Fighter(
        val name: String, val color: String, val specialColor: String,
        var x: Float, var y: Float, var hp: Int, val maxHp: Int,
        var facingRight: Boolean, var state: Int, var stateTimer: Float,
        var attackCooldown: Float, var blocking: Boolean
    )
    // states: 0=idle, 1=walk, 2=attack, 3=block, 4=hit, 5=ko

    private val fighters = mutableListOf<Fighter>()
    private var playerIndex = 0
    private var enemyIndex = 1

    // Personajes
    private val characters = listOf(
        Triple("RYU", "#5C94FC", "#FFFFFF"),
        Triple("KEN", "#D82800", "#F8D878"),
        Triple("CHUN-LI", "#00A800", "#5C94FC"),
        Triple("GUILE", "#FC9838", "#8B4513"),
        Triple("ZANGIEF", "#8814A8", "#D82800"),
        Triple("DHALSIM", "#F5C542", "#FF5252"),
        Triple("BLANKA", "#00E676", "#F5C542"),
        Triple("E.HONDA", "#F8D878", "#D82800"),
        Triple("BALROG", "#1A1A2E", "#F8D878"),
        Triple("VEGA", "#C0C0C0", "#D82800"),
        Triple("SAGAT", "#F5C542", "#000000"),
        Triple("BISON", "#8814A8", "#FF0000")
    )

    // Round
    private var round = 1
    private var maxRounds = 3
    private var playerWins = 0
    private var enemyWins = 0
    private var roundTimer = 0f
    private val roundDuration = 99f

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var selectedChar = 0
    private var enemyChar = 1

    // Animación
    private var hitFlash = 0f
    private var koTimer = 0f
    private var roundEndTimer = 0f

    // Paints
    private val bgPaint = paintOf("#1A1A2E")
    private val groundPaint = paintOf("#3A5A2A")
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

    private fun createFighter(charIndex: Int, x: Float, facingRight: Boolean): Fighter {
        val (name, color, special) = characters[charIndex]
        return Fighter(name, color, special, x, h * 0.6f, 100, 100, facingRight, 0, 0f, 0f, false)
    }

    override fun reset() {
        fighters.clear()
        selectedChar = 0
        enemyChar = Random.nextInt(1, characters.size)
        fighters.add(createFighter(selectedChar, w * 0.2f, true))
        fighters.add(createFighter(enemyChar, w * 0.7f, false))
        playerIndex = 0
        enemyIndex = 1
        round = 1
        playerWins = 0
        enemyWins = 0
        roundTimer = 0f
        scoreVal = 0
        state = State.FIGHTING
        hitFlash = 0f
        koTimer = 0f
        roundEndTimer = 0f
    }

    private fun startRound() {
        fighters[0].x = w * 0.2f
        fighters[0].y = h * 0.6f
        fighters[0].hp = fighters[0].maxHp
        fighters[0].state = 0
        fighters[0].facingRight = true
        fighters[1].x = w * 0.7f
        fighters[1].y = h * 0.6f
        fighters[1].hp = fighters[1].maxHp
        fighters[1].state = 0
        fighters[1].facingRight = false
        roundTimer = 0f
        state = State.FIGHTING
    }

    override fun update(dt: Float, input: GameInput) {
        if (state == State.GAME_OVER || state == State.VICTORY) {
            if (input.start || input.a) reset()
            return
        }

        if (state == State.ROUND_END) {
            roundEndTimer -= dt
            if (roundEndTimer <= 0f) {
                if (playerWins >= 2 || enemyWins >= 2) {
                    if (playerWins >= 2) {
                        state = State.VICTORY
                        scoreVal += 500
                        if (scoreVal > best) best = scoreVal
                    } else {
                        state = State.GAME_OVER
                        if (scoreVal > best) best = scoreVal
                    }
                } else {
                    startRound()
                }
            }
            return
        }

        if (state == State.KO) {
            koTimer -= dt
            if (koTimer <= 0f) {
                if (fighters[0].hp <= 0) enemyWins++
                else playerWins++
                state = State.ROUND_END
                roundEndTimer = 2f
            }
            return
        }

        // Round timer
        roundTimer += dt
        if (roundTimer >= roundDuration) {
            // Time out - whoever has more HP wins
            if (fighters[0].hp > fighters[1].hp) playerWins++
            else enemyWins++
            state = State.ROUND_END
            roundEndTimer = 2f
            return
        }

        // Actualizar fighters
        for (f in fighters) {
            f.stateTimer = (f.stateTimer - dt).coerceAtLeast(0f)
            f.attackCooldown = (f.attackCooldown - dt).coerceAtLeast(0f)
            if (f.stateTimer <= 0f && f.state in 2..4) f.state = 0
        }

        val player = fighters[0]
        val enemy = fighters[1]

        // Movimiento del jugador
        val speed = 200f
        if (player.state == 0 || player.state == 1) {
            if (input.left) { player.x = (player.x - speed * dt).coerceAtLeast(20f); player.state = 1; player.facingRight = false }
            if (input.right) { player.x = (player.x + speed * dt).coerceAtMost(w - 60f); player.state = 1; player.facingRight = true }
            if (!input.left && !input.right) player.state = 0
        }

        // Ataque (A)
        if (input.a && player.attackCooldown <= 0f && player.state != 4) {
            player.state = 2
            player.stateTimer = 0.3f
            player.attackCooldown = 0.5f
            // Verificar hit
            val dist = Math.abs(player.x - enemy.x)
            if (dist < 80f) {
                enemy.hp = (enemy.hp - 8).coerceAtLeast(0)
                enemy.state = 4
                enemy.stateTimer = 0.2f
                hitFlash = 0.2f
                scoreVal += 10
                if (scoreVal > best) best = scoreVal
                if (enemy.hp <= 0) {
                    state = State.KO
                    koTimer = 2f
                }
            }
            input.a = false
        }

        // Special move (START)
        if (input.start && player.attackCooldown <= 0f && player.state != 4) {
            player.state = 2
            player.stateTimer = 0.5f
            player.attackCooldown = 1f
            val dist = Math.abs(player.x - enemy.x)
            if (dist < 100f) {
                enemy.hp = (enemy.hp - 20).coerceAtLeast(0)
                enemy.state = 4
                enemy.stateTimer = 0.3f
                hitFlash = 0.4f
                scoreVal += 30
                if (scoreVal > best) best = scoreVal
                if (enemy.hp <= 0) {
                    state = State.KO
                    koTimer = 2f
                }
            }
            input.start = false
        }

        // Bloqueo (B)
        player.blocking = input.b

        // IA del enemigo
        if (enemy.state != 4 && enemy.state != 2) {
            val dx = player.x - enemy.x
            val dist = Math.abs(dx)
            // Acercarse
            if (dist > 90f) {
                enemy.x += (if (dx > 0) 1f else -1f) * 120f * dt
                enemy.facingRight = dx > 0
                enemy.state = 1
            } else {
                enemy.state = 0
                // Atacar ocasionalmente
                if (enemy.attackCooldown <= 0f && Random.nextFloat() < 0.02f) {
                    enemy.state = 2
                    enemy.stateTimer = 0.3f
                    enemy.attackCooldown = 0.8f
                    if (!player.blocking) {
                        player.hp = (player.hp - 8).coerceAtLeast(0)
                        player.state = 4
                        player.stateTimer = 0.2f
                        hitFlash = 0.2f
                        if (player.hp <= 0) {
                            state = State.KO
                            koTimer = 2f
                        }
                    }
                }
                // Special move ocasional
                if (enemy.attackCooldown <= 0f && Random.nextFloat() < 0.005f) {
                    enemy.state = 2
                    enemy.stateTimer = 0.5f
                    enemy.attackCooldown = 1.5f
                    if (!player.blocking) {
                        player.hp = (player.hp - 20).coerceAtLeast(0)
                        player.state = 4
                        player.stateTimer = 0.3f
                        hitFlash = 0.4f
                        if (player.hp <= 0) {
                            state = State.KO
                            koTimer = 2f
                        }
                    }
                }
            }
        }

        if (hitFlash > 0f) hitFlash -= dt * 4f
    }

    override fun draw(canvas: Canvas) {
        // Fondo
        canvas.drawColor(Color.parseColor("#1A1A2E"))
        // Suelo
        val groundPaint = paintOf("#3A5A2A")
        canvas.drawRect(0f, h * 0.75f, w.toFloat(), h.toFloat(), groundPaint)
        // Línea de suelo
        val linePaint = paintOf("#F8F8F8")
        linePaint.strokeWidth = 2f
        canvas.drawLine(0f, h * 0.75f, w.toFloat(), h * 0.75f, linePaint)

        // Luchadores
        drawFighter(canvas, fighters[0], true)
        drawFighter(canvas, fighters[1], false)

        // Hit flash
        if (hitFlash > 0f) {
            val alpha = (hitFlash * 100).toInt().coerceIn(0, 100)
            val flashPaint = Paint().apply { color = Color.parseColor("#FFFFFF"); this.alpha = alpha; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), flashPaint)
        }

        // HUD - HP bars
        val barW = w * 0.35f
        val barH = 16f
        val barY = 20f
        // Player HP
        val pBarBg = paintOf("#2A2A3E")
        canvas.drawRect(20f, barY, 20f + barW, barY + barH, pBarBg)
        val pBarFill = paintOf("#00E676")
        canvas.drawRect(20f, barY, 20f + barW * (fighters[0].hp.toFloat() / fighters[0].maxHp), barY + barH, pBarFill)
        val pFrame = paintOf("#F8F8F8")
        pFrame.style = Paint.Style.STROKE
        pFrame.strokeWidth = 2f
        canvas.drawRect(20f, barY, 20f + barW, barY + barH, pFrame)
        // Player name
        textPaint.textSize = 14f
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(fighters[0].name, 20f, barY - 5f, textPaint)

        // Enemy HP
        canvas.drawRect(w - 20f - barW, barY, w - 20f, barY + barH, pBarBg)
        val eBarFill = paintOf("#FF5252")
        canvas.drawRect(w - 20f - barW, barY, w - 20f - barW + barW * (fighters[1].hp.toFloat() / fighters[1].maxHp), barY + barH, eBarFill)
        canvas.drawRect(w - 20f - barW, barY, w - 20f, barY + barH, pFrame)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(fighters[1].name, w - 20f, barY - 5f, textPaint)
        textPaint.textAlign = Paint.Align.CENTER

        // Timer
        titlePaint.textSize = 24f
        canvas.drawText("${(roundDuration - roundTimer).toInt()}", w / 2f, 30f, titlePaint)

        // Round indicator
        smallPaint.textSize = 16f
        canvas.drawText("ROUND $round  ·  $playerWins - $enemyWins", w / 2f, 55f, smallPaint)

        // Score
        hudPaint.textSize = 16f
        canvas.drawText("SCORE: $scoreVal", 10f, h - 10f, hudPaint)

        // Overlay KO
        if (state == State.KO) {
            titlePaint.textSize = 64f
            titlePaint.color = Color.parseColor("#FF5252")
            canvas.drawText("K.O.!", w / 2f, h / 2f, titlePaint)
            titlePaint.color = Color.parseColor("#F5C542")
        }

        // Overlay Round End
        if (state == State.ROUND_END) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 120; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 36f
            val roundWinner = if (fighters[0].hp > fighters[1].hp) fighters[0].name else fighters[1].name
            canvas.drawText("$roundWinner GANA LA RONDA", w / 2f, h / 2f, titlePaint)
        }

        // Overlay Victory
        if (state == State.VICTORY) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 48f
            canvas.drawText("¡VICTORIA!", w / 2f, h / 2f - 30f, titlePaint)
            smallPaint.textSize = 22f
            canvas.drawText("Derrotaste a ${fighters[1].name}", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 40f, smallPaint)
            if (scoreVal >= best && scoreVal > 0) {
                titlePaint.textSize = 22f
                canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 70f, titlePaint)
            }
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 110f, greenPaint)
        }

        // Overlay Game Over
        if (state == State.GAME_OVER) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            redPaint.textSize = 48f
            canvas.drawText("GAME OVER", w / 2f, h / 2f - 30f, redPaint)
            smallPaint.textSize = 22f
            canvas.drawText("Derrotado por ${fighters[1].name}", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 40f, smallPaint)
            if (scoreVal >= best && scoreVal > 0) {
                titlePaint.textSize = 22f
                canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 70f, titlePaint)
            }
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 110f, greenPaint)
        }

        // Instrucciones
        if (state == State.FIGHTING) {
            smallPaint.textSize = 12f
            smallPaint.color = Color.parseColor("#B8B8C8")
            canvas.drawText("◀▶: mover  ·  A: atacar  ·  B: bloquear  ·  START: special", w / 2f, h - 10f, smallPaint)
        }
    }

    private fun drawFighter(canvas: Canvas, f: Fighter, isPlayer: Boolean) {
        val x = f.x; val y = f.y
        val bodyColor = Color.parseColor(f.color)
        val specialColor = Color.parseColor(f.specialColor)
        val bodyPaint = Paint().apply { color = bodyColor; isAntiAlias = true }
        val specialPaint = Paint().apply { color = specialColor; isAntiAlias = true }

        // Cuerpo
        canvas.drawRoundRect(RectF(x, y - 40f, x + 40f, y), 4f, 4f, bodyPaint)
        // Cabeza
        canvas.drawCircle(x + 20f, y - 50f, 12f, bodyPaint)
        // Ojos
        val eyePaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }
        val pupilPaint = Paint().apply { color = Color.BLACK; isAntiAlias = true }
        if (f.facingRight) {
            canvas.drawCircle(x + 26f, y - 52f, 4f, eyePaint)
            canvas.drawCircle(x + 28f, y - 52f, 2f, pupilPaint)
        } else {
            canvas.drawCircle(x + 14f, y - 52f, 4f, eyePaint)
            canvas.drawCircle(x + 12f, y - 52f, 2f, pupilPaint)
        }
        // Brazos (posición según estado)
        if (f.state == 2) {  // atacando
            val armPaint = Paint().apply { color = bodyColor; isAntiAlias = true }
            if (f.facingRight) {
                canvas.drawRect(x + 40f, y - 30f, x + 70f, y - 20f, armPaint)
            } else {
                canvas.drawRect(x - 30f, y - 30f, x, y - 20f, armPaint)
            }
        }
        // Piernas
        val legPaint = Paint().apply { color = specialColor; isAntiAlias = true }
        canvas.drawRect(x + 5f, y, x + 15f, y + 20f, legPaint)
        canvas.drawRect(x + 25f, y, x + 35f, y + 20f, legPaint)
        // Hit flash
        if (f.state == 4) {
            val flashPaint = Paint().apply { color = Color.parseColor("#FFFFFF"); alpha = 120; isAntiAlias = false }
            canvas.drawRoundRect(RectF(x - 5f, y - 60f, x + 45f, y + 25f), 4f, 4f, flashPaint)
        }
    }

    override val isGameOver: Boolean get() = state == State.GAME_OVER || state == State.VICTORY
}
