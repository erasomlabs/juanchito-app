package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class PitfallGame : Game {
    override val name = "Pitfall"
    override val genre = "Plataformas"
    private var w = 0; private var h = 0

    // World: 255 screens, screen-by-screen (no smooth scroll)
    private val totalScreens = 255
    private var screen = 0
    private var playerX = 0f
    private var playerY = 0f
    private val playerW = 16f; private val playerH = 24f
    private val groundY get() = h * 0.78f
    private val caveY get() = h * 0.92f
    private var inCave = false
    private var vy = 0f
    private var onGround = true
    private var facing = 1
    private var jumping = false
    private var lives = 3
    private var invuln = 0f
    private var timer = 1200f // 20 min
    override var isGameOver = false
    override var score = 2000
    private var highScore = 0
    private var won = false
    private var treasuresLeft = 32
    private var vineSwing = 0f // >0 swinging
    private var vineTarget = 0f

    // hazards for current screen
    private data class Hazard(val type: Int, var x: Float, val y: Float, val w: Float, val hgt: Float, var vx: Float, var timer: Float, var active: Boolean)
    // type: 0 pit(empty),1 pit croc,2 pit snake,3 pit scorpion,4 rolling log,5 tar pit,6 quicksand,7 fire,8 treasure,9 vine
    private val hazards = mutableListOf<Hazard>()
    private var ladderX = -1f
    private var hasLadder = false
    private var hasVine = false
    private var vineX = 0f

    // NES retro palette (crisp, no anti-alias)
    private val skyPaint = Paint().apply { color = Color.parseColor("#5C94FC"); isAntiAlias = false }
    private val groundPaint = Paint().apply { color = Color.parseColor("#00A800"); isAntiAlias = false }
    private val dirtPaint = Paint().apply { color = Color.parseColor("#8B4513"); isAntiAlias = false }
    private val cavePaint = Paint().apply { color = Color.parseColor("#000000"); isAntiAlias = false }
    private val playerPaint = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
    private val hatPaint = Paint().apply { color = Color.parseColor("#D82800"); isAntiAlias = false }
    private val logPaint = Paint().apply { color = Color.parseColor("#8B4513"); isAntiAlias = false }
    private val crocPaint = Paint().apply { color = Color.parseColor("#00A800"); isAntiAlias = false }
    private val snakePaint = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
    private val scorPaint = Paint().apply { color = Color.parseColor("#FC9838"); isAntiAlias = false }
    private val firePaint = Paint().apply { color = Color.parseColor("#D82800"); isAntiAlias = false }
    private val tarPaint = Paint().apply { color = Color.parseColor("#000000"); isAntiAlias = false }
    private val quickPaint = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
    private val treasPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); isAntiAlias = false }
    private val vinePaint = Paint().apply { color = Color.parseColor("#00A800"); isAntiAlias = false }
    private val ladderPaint = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
    private val textPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 16f; isAntiAlias = false }
    private val smallPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 12f; isAntiAlias = false }

    override fun init(width: Int, height: Int) { w = width; h = height; reset() }

    private fun genScreen(idx: Int) {
        hazards.clear()
        hasLadder = (idx % 7 == 0) || (idx % 11 == 0)
        ladderX = if (hasLadder) w * 0.5f else -1f
        hasVine = false
        val r = Random(idx * 997 + 13)
        val type = (idx * 37 + 13) % 9
        when (type) {
            0 -> hazards.add(Hazard(1, w * 0.3f, groundY, 40f, 16f, 0f, 0f, true)) // croc pit
            1 -> hazards.add(Hazard(2, w * 0.35f, groundY, 30f, 12f, 0f, 0f, true)) // snake
            2 -> hazards.add(Hazard(3, w * 0.4f, groundY, 30f, 12f, 0f, 0f, true)) // scorpion
            3 -> hazards.add(Hazard(4, 0f, groundY - 12f, 20f, 12f, 80f + r.nextFloat() * 40f, 0f, true)) // rolling log
            4 -> hazards.add(Hazard(5, w * 0.25f, groundY, 60f, 8f, 0f, 0f, true)) // tar pit
            5 -> hazards.add(Hazard(6, w * 0.45f, groundY, 50f, 8f, 0f, 0f, true)) // quicksand
            6 -> hazards.add(Hazard(7, w * 0.3f, groundY - 16f, 24f, 16f, 0f, 0f, true)) // fire
            7 -> { hasVine = true; vineX = w * 0.4f; hazards.add(Hazard(0, w * 0.35f, groundY, 50f, 20f, 0f, 0f, true)) } // pit + vine
            8 -> hazards.add(Hazard(4, 0f, groundY - 12f, 20f, 12f, 100f, 0f, true)) // rolling log
        }
        // treasure
        if (idx % 8 == 3 || idx % 8 == 6) {
            val tv = intArrayOf(2000, 3000, 4000, 5000)[r.nextInt(4)]
            hazards.add(Hazard(8, w * 0.6f, groundY - 12f, 12f, 12f, tv.toFloat(), 0f, true))
        }
        // cave treasure
        if (inCave && idx % 5 == 0) {
            hazards.add(Hazard(8, w * 0.4f, caveY - 12f, 12f, 12f, 3000f, 0f, true))
        }
    }

    override fun reset() {
        screen = 0; playerX = 40f; playerY = groundY - playerH
        inCave = false; vy = 0f; onGround = true; facing = 1; jumping = false
        lives = 3; invuln = 0f; timer = 1200f; isGameOver = false; won = false
        score = 2000; treasuresLeft = 32; vineSwing = 0f
        genScreen(screen)
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) { if (input.start) reset(); return }
        timer -= dt
        if (timer <= 0f) { isGameOver = true; return }
        invuln = (invuln - dt).coerceAtLeast(0f)
        vineSwing = (vineSwing - dt).coerceAtLeast(0f)

        val levelY = if (inCave) caveY else groundY

        // horizontal movement
        if (input.left) { playerX -= 120f * dt; facing = -1 }
        if (input.right) { playerX += 120f * dt; facing = 1 }

        // climb ladder
        if (hasLadder && kotlin.math.abs(playerX - ladderX) < 12f) {
            if (input.up) { if (!inCave) { inCave = true; playerY = caveY - playerH } }
            if (input.down) { if (inCave) { inCave = false; playerY = groundY - playerH } }
        }

        // jump
        if (input.a && onGround && !inCave) {
            vy = -320f; onGround = false; jumping = true; input.a = false
            // vine grab
            if (hasVine && kotlin.math.abs(playerX - vineX) < 20f) {
                vineSwing = 0.6f; vineTarget = vineX + 60f
            }
        }

        // vine swing
        if (vineSwing > 0f) {
            playerX += (vineTarget - playerX) * 0.1f
            playerY = groundY - 40f + kotlin.math.sin((0.6f - vineSwing) * 5f) * 10f
        } else {
            // gravity
            vy += 900f * dt
            playerY += vy * dt
            if (playerY >= levelY - playerH) { playerY = levelY - playerH; vy = 0f; onGround = true; jumping = false }
            else onGround = false
        }

        // screen transition
        if (playerX > w - 16f) {
            if (screen < totalScreens - 1) { screen++; playerX = 8f; genScreen(screen) } else { won = true; isGameOver = true }
        }
        if (playerX < 0f) {
            if (screen > 0) { screen--; playerX = w - 24f; genScreen(screen) } else playerX = 0f
        }

        // update hazards
        for (hz in hazards) {
            if (!hz.active) continue
            if (hz.type == 4) { // rolling log
                hz.x += hz.vx * dt
                if (hz.x > w + 20f) { hz.x = -20f }
            }
            if (hz.type == 1) { // croc mouth toggle
                hz.timer += dt
                if (hz.timer > 1.2f) { hz.timer = 0f; hz.active = hz.active } // toggle handled in draw via timer
            }
        }

        // collisions
        if (invuln <= 0f) {
            val pr = RectF(playerX, playerY, playerX + playerW, playerY + playerH)
            for (hz in hazards) {
                if (!hz.active) continue
                val hr = RectF(hz.x, hz.y - hz.hgt, hz.x + hz.w, hz.y)
                if (RectF.intersects(pr, hr)) {
                    when (hz.type) {
                        0 -> { // pit: fall = lose life
                            lives--; invuln = 2f
                            if (lives <= 0) isGameOver = true
                            playerX = 40f; playerY = groundY - playerH
                        }
                        1 -> { // croc: die if mouth open (timer phase)
                            val mouthOpen = (hz.timer % 1.2f) > 0.6f
                            if (mouthOpen) { lives--; invuln = 2f; if (lives <= 0) isGameOver = true; playerX = 40f; playerY = groundY - playerH }
                        }
                        2, 3 -> { lives--; invuln = 2f; if (lives <= 0) isGameOver = true; playerX = 40f; playerY = groundY - playerH }
                        4 -> { score -= 100; if (score < 0) score = 0 } // rolling log
                        5, 6 -> { // tar pit / quicksand: sink
                            vy += 200f * dt
                            if (playerY > levelY) { lives--; invuln = 2f; if (lives <= 0) isGameOver = true; playerX = 40f; playerY = groundY - playerH }
                        }
                        7 -> { lives--; invuln = 2f; if (lives <= 0) isGameOver = true; playerX = 40f; playerY = groundY - playerH } // fire
                        8 -> { // treasure
                            score += hz.vx.toInt(); treasuresLeft--; hz.active = false
                            if (treasuresLeft <= 0) { won = true; isGameOver = true }
                        }
                    }
                }
            }
        }

        // bonus for time (score updated at end)
        if (isGameOver && won) {
            score += (timer * 10).toInt()
            if (score > highScore) highScore = score
        }
    }

    override fun draw(canvas: Canvas) {
        // sky
        canvas.drawRect(0f, 0f, w.toFloat(), groundY, skyPaint)
        // ground
        canvas.drawRect(0f, groundY, w.toFloat(), h.toFloat(), groundPaint)
        // cave (if in cave, darken)
        if (inCave) canvas.drawRect(0f, groundY, w.toFloat(), h.toFloat(), cavePaint)
        // ladder
        if (hasLadder) {
            val lx = ladderX
            val top = if (inCave) caveY - 60f else groundY - 60f
            val bot = if (inCave) caveY else groundY
            for (yy in top.toInt() until bot.toInt() step 6) {
                canvas.drawRect(lx - 2f, yy.toFloat(), lx + 2f, yy + 4f, ladderPaint)
                canvas.drawRect(lx - 8f, yy.toFloat(), lx - 6f, yy + 2f, ladderPaint)
                canvas.drawRect(lx + 6f, yy.toFloat(), lx + 8f, yy + 2f, ladderPaint)
            }
        }
        // vine
        if (hasVine) {
            canvas.drawRect(vineX - 2f, 0f, vineX + 2f, groundY - 20f, vinePaint)
            canvas.drawCircle(vineX, groundY - 20f, 6f, vinePaint)
        }
        // hazards
        for (hz in hazards) {
            if (!hz.active) continue
            val hr = RectF(hz.x, hz.y - hz.hgt, hz.x + hz.w, hz.y)
            when (hz.type) {
                0 -> canvas.drawRect(hz.x, hz.y - 4f, hz.x + hz.w, hz.y + 8f, tarPaint) // pit
                1 -> { // croc
                    val mouthOpen = (hz.timer % 1.2f) > 0.6f
                    canvas.drawRect(hr, crocPaint)
                    if (mouthOpen) {
                        val mp = Paint().apply { color = Color.parseColor("#F8F8F8") }
                        canvas.drawRect(hz.x + 4f, hz.y - hz.hgt + 2f, hz.x + hz.w - 4f, hz.y - 4f, mp)
                    }
                }
                2 -> canvas.drawOval(hr, snakePaint)
                3 -> canvas.drawOval(hr, scorPaint)
                4 -> canvas.drawRoundRect(hr, 2f, 2f, logPaint) // log
                5 -> canvas.drawRect(hz.x, hz.y - 4f, hz.x + hz.w, hz.y + 4f, tarPaint)
                6 -> canvas.drawRect(hz.x, hz.y - 4f, hz.x + hz.w, hz.y + 4f, quickPaint)
                7 -> canvas.drawRect(hz.x, hz.y - hz.hgt, hz.x + hz.w, hz.y, firePaint)
                8 -> canvas.drawCircle(hz.x + hz.w/2, hz.y - hz.hgt/2, hz.w/2, treasPaint)
            }
        }
        // player (Juanchito) - NES style pixel character
        if (invuln <= 0f || (invuln * 8).toInt() % 2 == 0) {
            val px = playerX; val py = playerY
            // body
            canvas.drawRect(px + 4f, py + 8f, px + 12f, py + playerH, playerPaint)
            // head
            canvas.drawRect(px + 4f, py, px + 12f, py + 8f, hatPaint)
            // legs
            canvas.drawRect(px + 4f, py + playerH - 4f, px + 8f, py + playerH + 4f, playerPaint)
            canvas.drawRect(px + 8f, py + playerH - 4f, px + 12f, py + playerH + 4f, playerPaint)
        }
        // HUD
        textPaint.textSize = 14f
        canvas.drawText("Score: $score", 8f, 16f, textPaint)
        canvas.drawText("Lives: $lives", w - 60f, 16f, textPaint)
        smallPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Tiempo: ${timer.toInt()}s  Pantalla: ${screen + 1}/255", w/2f, 16f, smallPaint)
        smallPaint.textAlign = Paint.Align.LEFT
        if (isGameOver) {
            val msg = if (won) "¡GANASTE!" else "GAME OVER"
            val col = if (won) Color.parseColor("#00A800") else Color.parseColor("#D82800")
            textPaint.textSize = 32f; textPaint.color = col
            canvas.drawText(msg, w/2f, h/2f, textPaint)
            textPaint.textSize = 14f; textPaint.color = Color.parseColor("#F8F8F8")
            canvas.drawText("Pulsa START", w/2f, h/2f + 24f, smallPaint.apply { textSize = 14f; color = Color.parseColor("#F8F8F8"); textAlign = Paint.Align.CENTER })
        }
    }
}