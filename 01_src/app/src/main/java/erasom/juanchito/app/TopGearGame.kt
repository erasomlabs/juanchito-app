package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class TopGearGame : Game {
    override val name = "Turbo Total"
    override val genre = "Carreras"
    private var w = 0; private var h = 0
    private var px = 0f; private var py = 0f
    private var speed = 0f; private var angle = 0f
    private var nitro = 100f; private var fuel = 100f
    private var lap = 1; private val laps = 5
    private var progress = 0f; private val trackLen = 6000f
    private val rivals = mutableListOf<Rival>()
    private var spawnTimer = 0f
    private var weather = 0 // 0 dry,1 rain,2 night
    private var upgrade = 0
    override var isGameOver = false
    override var score = 0
    private var highScore = 0
    private var won = false
    private var crashed = 0f

    private data class Rival(var x: Float, var y: Float, var speed: Float, val color: Int)

    private val carPaint = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
    private val rivalColors = listOf("#D82800", "#5C94FC", "#00A800", "#FC9838")
    private val trackPaint = Paint().apply { color = Color.parseColor("#2D3748"); isAntiAlias = false }
    private val grassPaint = Paint().apply { color = Color.parseColor("#00A800"); isAntiAlias = false }
    private val textPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 12f; isAntiAlias = false; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 11f; isAntiAlias = false; textAlign = Paint.Align.CENTER }

    override fun init(width: Int, height: Int) { w = width; h = height; reset() }

    override fun reset() {
        px = w / 2f; py = h * 0.75f; speed = 0f; angle = 0f
        nitro = 100f; fuel = 100f; lap = 1; progress = 0f; crashed = 0f
        rivals.clear()
        for (i in 0 until 7) rivals.add(Rival(w / 2f + Random.nextInt(-60, 60), h * 0.3f - i * 70f, 220f + Random.nextFloat() * 80f, Random.nextInt(4)))
        weather = Random.nextInt(3); upgrade = 0; score = 0; isGameOver = false; won = false
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) { if (input.start) reset(); return }
        crashed = (crashed - dt).coerceAtLeast(0f)
        fuel = (fuel - (speed / 500f) * 3f * dt).coerceAtLeast(0f)
        if (fuel <= 0f) { speed = 0f }
        // steer
        if (input.left) angle -= 2.5f * dt
        if (input.right) angle += 2.5f * dt
        angle = angle.coerceIn(-0.9f, 0.9f)
        // accelerate / brake
        val maxSpeed = 500f + upgrade * 50f
        if (input.a) speed = (speed + 250f * dt).coerceAtMost(maxSpeed)
        else speed = (speed - 180f * dt).coerceAtLeast(0f)
        // nitro
        if (input.b && nitro > 0f) { speed = (speed + 350f * dt).coerceAtMost(maxSpeed + 200f); nitro -= 25f * dt }
        if (crashed > 0f) speed *= 0.96f
        // weather friction
        val friction = if (weather == 1) 0.97f else 1f
        speed *= friction
        // move
        px += kotlin.math.sin(angle) * speed * dt
        py -= kotlin.math.cos(angle) * speed * dt
        // track bounds
        if (px < 50f) { px = 50f; crashed = 0.3f; speed *= 0.6f }
        if (px > w - 50f) { px = w - 50f; crashed = 0.3f; speed *= 0.6f }
        if (py < 30f) { py = 30f; crashed = 0.3f; speed *= 0.6f }
        if (py > h - 30f) { py = h - 30f; crashed = 0.3f; speed *= 0.6f }
        // progress / lap
        progress += speed * dt
        if (progress >= trackLen) { progress -= trackLen; lap++; if (lap > laps) { won = true; isGameOver = true; score += 2000 } }
        // rivals
        for (r in rivals) {
            r.y += r.speed * dt
            if (r.y > h) { r.y = 0f; r.x = w / 2f + Random.nextInt(-80, 80) }
            if (kotlin.math.abs(r.x - px) < 28f && kotlin.math.abs(r.y - py) < 28f) {
                crashed = 0.5f; speed *= 0.5f
            }
        }
        score = (progress / 10).toInt() + (lap - 1) * 1000 + (if (won) 2000 else 0)
        if (score > highScore) highScore = score
        nitro = (nitro + 8f * dt).coerceAtMost(100f)
        if (input.select) { /* upgrade between races not in-race */ }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(if (weather == 2) Color.parseColor("#000020") else if (weather == 1) Color.parseColor("#2D3748") else Color.parseColor("#5C94FC"))
        // grass
        canvas.drawRect(0f, 0f, 40f, h.toFloat(), grassPaint)
        canvas.drawRect(w - 40f, 0f, w.toFloat(), h.toFloat(), grassPaint)
        // track
        canvas.drawRect(40f, 0f, w - 40f, h.toFloat(), trackPaint)
        // center line
        val lp = Paint().apply { color = Color.parseColor("#F8F8F8"); strokeWidth = 2f }
        var yy = 0f
        while (yy < h) { canvas.drawLine(w / 2f, yy, w / 2f, yy + 15f, lp); yy += 30f }
        // rivals
        for (r in rivals) {
            val p = Paint().apply { color = Color.parseColor(rivalColors[r.color]); isAntiAlias = false }
            canvas.drawRoundRect(RectF(r.x - 12f, r.y - 20f, r.x + 12f, r.y + 20f), 4f, 4f, p)
        }
        // car
        canvas.save()
        canvas.translate(px, py)
        canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat())
        canvas.drawRoundRect(RectF(-12f, -20f, 12f, 20f), 4f, 4f, carPaint)
        canvas.restore()
        // HUD
        textPaint.textSize = 12f
        canvas.drawText("Score: $score  Vuelta: $lap/$laps  Nitro: ${nitro.toInt()}%  Combustible: ${fuel.toInt()}%", w / 2f, 14f, textPaint)
        smallPaint.textAlign = Paint.Align.LEFT
        val wstr = arrayOf("Seco", "Lluvia", "Noche")[weather]
        canvas.drawText("Clima: $wstr", 8f, h - 8f, smallPaint)
        smallPaint.textAlign = Paint.Align.CENTER
        if (isGameOver) {
            val msg = if (won) "¡GANASTE!" else "GAME OVER"
            val col = if (won) Color.parseColor("#00A800") else Color.parseColor("#D82800")
            textPaint.textSize = 32f; textPaint.color = col
            canvas.drawText(msg, w / 2f, h / 2f, textPaint)
            textPaint.textSize = 12f; textPaint.color = Color.parseColor("#F8F8F8")
            canvas.drawText("Pulsa START", w / 2f, h / 2f + 24f, smallPaint.apply { textSize = 14f; color = Color.parseColor("#F8F8F8"); textAlign = Paint.Align.CENTER })
        }
    }
}