package erasom.juanchito.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.sin

/**
 * Intro cinematográfica: caminata en primera persona por un salón arcade
 * de los 90 hasta una consola retro. La consola enciende y espera [START].
 * Se puede omitir con el botón "OMITIR".
 */
class IntroActivity : AppCompatActivity() {

    private lateinit var surface: SurfaceView
    private lateinit var btnSkip: Button
    private lateinit var hint: TextView
    private var thread: Thread? = null
    @Volatile private var running = false
    @Volatile private var launched = false

    // Estado de la cinemática
    private var t = 0f
    private var fastForward = false
    private var powered = false

    private val cam = MiniCam(0f, -430f, 32f, 0f)
    private val walkEndY = -150f
    private val walkStartY = -430f
    private val walkSpeed = 58f

    private val textPaint = Paint().apply {
        color = Color.parseColor("#F5C542")
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = false
    }
    private val screenPaint = Paint().apply { isAntiAlias = false }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        surface = findViewById(R.id.introSurface)
        btnSkip = findViewById(R.id.btnSkipIntro)
        hint = findViewById(R.id.tvIntroHint)

        btnSkip.setOnClickListener { enterApp() }

        // Tocar la pantalla: durante la caminata acelera; en la consola, enciende/entra
        surface.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (cameraArrived()) {
                    if (powered) enterApp() else powered = true
                } else {
                    fastForward = true
                }
                true
            } else false
        }

        surface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                running = true
                thread = Thread { loop(holder) }
                thread?.start()
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                running = false
                try { thread?.join() } catch (_: InterruptedException) {}
            }
        })
    }

    private fun cameraArrived(): Boolean = cam.y >= walkEndY - 1f

    private fun enterApp() {
        if (launched) return
        launched = true
        running = false
        startActivity(Intent(this, MainActivity::class.java).putExtra("enter_world", true))
        finish()
    }

    private fun loop(holder: SurfaceHolder) {
        var last = System.nanoTime()
        while (running) {
            val canvas = holder.lockCanvas() ?: continue
            val now = System.nanoTime()
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
            last = now
            try {
                update(dt)
                render(canvas)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    private fun update(dt: Float) {
        t += dt
        if (!cameraArrived()) {
            val speed = if (fastForward) walkSpeed * 6f else walkSpeed
            cam.y += speed * dt
            cam.z = 32f + 2.5f * sin(t * 7f)   // paso
            if (cam.y >= walkEndY) {
                cam.y = walkEndY
                powered = true
            }
        } else {
            // Cámara quieta frente a la consola con vaivén sutil
            cam.z = 40f + 1.5f * sin(t * 1.2f)
            cam.x = 6f * sin(t * 0.45f)
        }
    }

    private fun render(canvas: Canvas) {
        val w = canvas.width
        val h = canvas.height
        // Interior oscuro: cielo=techo oscuro, suelo alfombra base
        Mini3D.drawGroundAndSky(canvas, w, h,
            skyColor = Color.parseColor("#0A0812"),
            groundColor = Color.parseColor("#241A32"))

        // ---- Escena: salón arcade ----
        val faces = ArrayList<Mini3D.Face>(48)

        // Alfombra del salón (quad a ras de suelo)
        Mini3D.addQuad(faces, Color.parseColor("#332244"), 0f, 0f, 1f,
            -240f, -560f, 0.4f, 240f, -560f, 0.4f, 240f, 40f, 0.4f, -240f, 40f, 0.4f)
        // Alfombra roja central
        Mini3D.addQuad(faces, Color.parseColor("#6A1A2A"), 0f, 0f, 1f,
            -46f, -560f, 0.6f, 46f, -560f, 0.6f, 46f, 40f, 0.6f, -46f, 40f, 0.6f)

        // Paredes y techo
        Mini3D.box(-252f, -260f, 55f, 14f, 640f, 110f, 0f, Color.parseColor("#181222"), faces)
        Mini3D.box(252f, -260f, 55f, 14f, 640f, 110f, 0f, Color.parseColor("#181222"), faces)
        Mini3D.box(0f, -260f, 116f, 520f, 640f, 12f, 0f, Color.parseColor("#100C18"), faces)
        // Neones de pared
        Mini3D.box(-244f, -260f, 98f, 4f, 620f, 4f, 0f, Color.parseColor("#F578B8"), faces)
        Mini3D.box(244f, -260f, 98f, 4f, 620f, 4f, 0f, Color.parseColor("#5C94FC"), faces)

        // Muebles arcade a los lados
        val cabColors = listOf("#4A1A5A", "#1A4A5A", "#5A4A1A", "#1A5A3A", "#5A2A2A", "#2A2A5A")
        var side = -1
        for (row in 0..1) {
            for (i in 0..4) {
                val cy = -380f + i * 105f
                val cx = side * 185f
                val col = Color.parseColor(cabColors[(i + row * 3) % cabColors.size])
                Mini3D.box(cx, cy, 36f, 58f, 40f, 72f, 0f, col, faces)
                // pantalla del mueble (frontal hacia el pasillo)
                Mini3D.addQuad(faces,
                    if (t > 1.5f && ((t * 2 + i).toInt() % 3 != 0)) Mini3D.lighten(col, 90) else Mini3D.darken(col, 40),
                    if (side < 0) 1f else -1f, 0f, 0f,
                    cx + side * -29.5f, cy - 16f, 44f,
                    cx + side * -29.5f, cy + 16f, 44f,
                    cx + side * -29.5f, cy + 16f, 60f,
                    cx + side * -29.5f, cy - 16f, 60f)
            }
            side = 1
        }

        // ---- La consola de los 90 ----
        // TV/CRT
        Mini3D.box(0f, -36f, 56f, 118f, 66f, 82f, 0f, Color.parseColor("#2A2A32"), faces)
        // Pantalla de la TV (cara sur, mira al jugador)
        val screenLit = if (powered) 1f else 0f
        val flicker = if (powered && (t * 9).toInt() % 7 == 0) 0.6f else 1f
        val screenCol = if (powered) Mini3D.lighten(Color.parseColor("#8FD0F8"), (40 * flicker).toInt()) else Color.parseColor("#0A0A12")
        Mini3D.addQuad(faces, screenCol, 0f, -1f, 0f,
            -42f, -69.4f, 34f, 42f, -69.4f, 34f, 42f, -69.4f, 80f, -42f, -69.4f, 80f)
        screenPaint.color = Color.parseColor("#C41E24")
        // Consola bajo la TV
        Mini3D.box(0f, -60f, 10f, 104f, 40f, 20f, 0f, Color.parseColor("#4A4A5A"), faces)
        // LED de encendido
        Mini3D.box(46f, -80f, 10f, 5f, 3f, 3f, 0f,
            if (powered) Color.parseColor("#FF3050") else Color.parseColor("#5A1020"), faces)
        // Joystick y botones arcade frente a la consola
        Mini3D.box(-30f, -110f, 5f, 26f, 20f, 10f, 0f, Color.parseColor("#1A1A22"), faces)
        Mini3D.box(30f, -110f, 5f, 26f, 20f, 10f, 0f, Color.parseColor("#1A1A22"), faces)

        Mini3D.render(canvas, cam, w, h, faces)

        // ---- Overlays 2D ----
        var alpha = 0f
        var label = ""
        when {
            t < 2.4f -> { label = "ERASO M LABS PRESENTA"; alpha = blinkIn(t, 2.4f) }
            t < 4.6f -> { label = "★ PLAYZONE ★"; alpha = 1f }
            cameraArrived() && powered -> {
                label = "▶  PULSA START  ◀"
                alpha = if ((t * 2).toInt() % 2 == 0) 1f else 0.35f
            }
            cameraArrived() -> { label = "ENCENDIENDO..."; alpha = 0.8f }
            else -> { label = "★ PLAYZONE: MUNDO JUANCHITO ★"; alpha = 1f }
        }
        if (label.isNotEmpty()) {
            textPaint.textSize = (h * 0.06f).coerceIn(18f, 40f)
            textPaint.color = Color.argb((255 * alpha).toInt(), 245, 197, 66)
            canvas.drawText(label, w / 2f, h * (if (cameraArrived()) 0.2f else 0.5f), textPaint)
            // Sombra del texto
            textPaint.color = Color.argb((120 * alpha).toInt(), 0, 0, 0)
            canvas.drawText(label, w / 2f + 3f, h * (if (cameraArrived()) 0.2f else 0.5f) + 3f, textPaint)
        }
        if (cameraArrived() && powered) {
            textPaint.textSize = (h * 0.035f).coerceIn(12f, 22f)
            textPaint.color = Color.argb(200, 143, 208, 248)
            canvas.drawText("toca la pantalla para entrar al mundo", w / 2f, h * 0.28f, textPaint)
        }
    }

    private fun blinkIn(time: Float, duration: Float): Float {
        val f = (time / duration).coerceIn(0f, 1f)
        return if (f < 0.2f) f / 0.2f else 1f
    }
}
