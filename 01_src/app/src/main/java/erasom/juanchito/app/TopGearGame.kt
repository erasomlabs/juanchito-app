package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class TopGearGame : Game {
    override val name = "Top Gear 2"
    override val genre = "Carreras"
    private var w = 0; private var h = 0
    private var px = 0f; private var py = 0f
    private var speed = 0f; private var angle = 0f
    private var nitro = 100f
    private var lap = 1; private val laps = 3
    private var progress = 0f
    private var trackLen = 5000f
    private val rivals = mutableListOf<Rival>()
    private var spawnTimer = 0f
    override var isGameOver = false
    override var score = 0
    private var highScore = 0
    private var won = false
    private var crashed = 0f

    private data class Rival(var x: Float, var y: Float, var speed: Float)

    private val carPaint = Paint().apply { color = Color.parseColor("#FECA57"); isAntiAlias = true }
    private val rivalPaint = Paint().apply { color = Color.parseColor("#FF6B6B"); isAntiAlias = true }
    private val trackPaint = Paint().apply { color = Color.parseColor("#2D3748"); isAntiAlias = true }
    private val textPaint = Paint().apply { color = Color.parseColor("#F5F5F5"); textSize = 28f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#48DBFB"); textSize = 20f; isAntiAlias = true; textAlign = Paint.Align.CENTER }

    override fun init(width: Int, height: Int) { w = width; h = height; reset() }

    override fun reset() {
        px = w/2f; py = h*0.7f; speed = 0f; angle = 0f; nitro = 100f
        lap = 1; progress = 0f; rivals.clear(); spawnTimer = 0f; crashed = 0f
        for (i in 0 until 5) rivals.add(Rival(w/2f + Random.nextInt(-100,100), h*0.3f - i*80f, 200f + Random.nextFloat()*100f))
        score = 0; isGameOver = false; won = false
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) { if (input.start) reset(); return }
        crashed = (crashed - dt).coerceAtLeast(0f)
        // steer
        if (input.left) angle -= 3f * dt
        if (input.right) angle += 3f * dt
        angle = angle.coerceIn(-0.8f, 0.8f)
        // accelerate
        if (input.a) speed = (speed + 300f * dt).coerceAtMost(500f)
        else speed = (speed - 200f * dt).coerceAtLeast(0f)
        // nitro
        if (input.b && nitro > 0f) { speed = (speed + 400f * dt).coerceAtMost(700f); nitro -= 30f * dt }
        // friction
        if (crashed > 0f) speed *= 0.95f
        // move
        px += kotlin.math.sin(angle) * speed * dt
        py -= kotlin.math.cos(angle) * speed * dt
        // keep on track (simple bounds)
        px = px.coerceIn(60f, w - 60f)
        if (py < 40f) { py = 40f; crashed = 0.5f; speed *= 0.5f }
        if (py > h - 40f) { py = h - 40f; crashed = 0.5f; speed *= 0.5f }
        // progress
        progress += speed * dt
        if (progress >= trackLen) { progress -= trackLen; lap++; if (lap > laps) { won = true; isGameOver = true; score += 1000 } }
        // rivals
        for (r in rivals) {
            r.y += r.speed * dt
            if (r.y > h) { r.y = 0f; r.x = w/2f + Random.nextInt(-80,80) }
        }
        // collision with rival
        for (r in rivals) {
            if (kotlin.math.abs(r.x - px) < 24f && kotlin.math.abs(r.y - py) < 24f) {
                crashed = 1f; speed *= 0.4f
            }
        }
        score = (progress / 10).toInt() + (lap - 1) * 500
        if (score > highScore) highScore = score
        nitro = (nitro + 5f * dt).coerceAtMost(100f)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))
        // track
        canvas.drawRect(40f, 0f, w - 40f, h.toFloat(), trackPaint)
        // center line
        val lp = Paint().apply { color = Color.parseColor("#48DBFB"); strokeWidth = 2f; alpha = 100 }
        var yy = 0f
        while (yy < h) { canvas.drawLine(w/2f, yy, w/2f, yy + 20f, lp); yy += 40f }
        // rivals
        for (r in rivals) canvas.drawRoundRect(RectF(r.x - 12f, r.y - 20f, r.x + 12f, r.y + 20f), 4f, 4f, rivalPaint)
        // car
        canvas.save()
        canvas.translate(px, py)
        canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat())
        canvas.drawRoundRect(RectF(-12f, -20f, 12f, 20f), 4f, 4f, carPaint)
        canvas.restore()
        // HUD
        textPaint.textSize = 22f
        canvas.drawText("Score: $score  Vuelta: $lap/$laps", w/2f, 30f, textPaint)
        smallPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Nitro: ${nitro.toInt()}%", 20f, 30f, smallPaint)
        smallPaint.textAlign = Paint.Align.CENTER
        if (isGameOver) {
            val msg = if (won) "¡GANASTE!" else "GAME OVER"
            val col = if (won) Color.parseColor("#55EFC4") else Color.parseColor("#FF6B6B")
            canvas.drawText(msg, w/2f, h/2f, textPaint.apply { textSize = 48f; color = col })
            canvas.drawText("Pulsa START", w/2f, h/2f + 60f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}