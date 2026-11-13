package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class PitfallGame : Game {
    override val name = "Jungla Perdida"
    override val genre = "Plataformas"
    private var w = 0; private var h = 0

    // Estados: 0=Prólogo expedición, 1=Gameplay selva, 2=Diálogo diario, 3=Victoria
    private var gameState = 0
    override var isGameOver = false
    override var score = 0

    private var currentAct = 1
    private var dialogueIndex = 0
    private var dialogueQueue = mutableListOf<Pair<String, String>>()

    // Pixel el explorador
    private var px = 60f; private var py = 0f
    private var vy = 0f
    private var onGround = true
    private var onVine = false
    private var vineAngle = 0f
    private var lives = 3
    private var treasuresFound = 0
    private val totalTreasures = 8
    private var timeRemaining = 120f // 2 minutos por expedición

    // Nivel y pantalla de jungla
    private var currentScreen = 0
    private val groundY get() = h * 0.72f

    // Obstáculos (Troncos rodantes, pozos de brea, cocodrilos en pantano)
    private var logX = 0f
    private var logVx = -140f
    private var swampOpen = false
    private var swampTimer = 0f

    // Input debounce
    private var btnAPressed = false
    private var btnBPressed = false
    private var btnStartPressed = false

    private val pText = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 11f; isAntiAlias = false }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        reset()
    }

    override fun reset() {
        gameState = 0
        score = 0
        isGameOver = false
        lives = 3
        currentAct = 1
        treasuresFound = 0
        timeRemaining = 120f
        currentScreen = 0
        px = 60f
        py = groundY
        vy = 0f
        onVine = false
        logX = w + 50f
    }

    override fun update(dt: Float, input: GameInput) {
        val aDown = input.a && !btnAPressed
        val bDown = input.b && !btnBPressed
        val startDown = input.start && !btnStartPressed
        btnAPressed = input.a
        btnBPressed = input.b
        btnStartPressed = input.start

        when (gameState) {
            0 -> { // Prólogo
                if (aDown || startDown) {
                    gameState = 2
                    dialogueQueue.clear()
                    dialogueQueue.add("PITFALL HARRY" to "Me adentro en el corazón de la cuenca prohibida maya.")
                    dialogueQueue.add("DIARIO DE EXPEDICIÓN" to "Debes cruzar las fosas de brea colgado de las lianas y saltar troncos rodantes.")
                    dialogueQueue.add("HARRY" to "Hay 8 tesoros dorados antes del anochecer. ¡En marcha!")
                    dialogueIndex = 0
                }
            }
            2 -> { // Diálogo
                if (aDown) {
                    dialogueIndex++
                    if (dialogueIndex >= dialogueQueue.size) {
                        gameState = 1
                    }
                }
            }
            1 -> { // Gameplay
                updateGameplay(dt, input, aDown, bDown)
            }
            3 -> { // Victoria
                if (aDown || startDown) {
                    currentAct++
                    reset()
                }
            }
        }
    }

    private fun updateGameplay(dt: Float, input: GameInput, aDown: Boolean, bDown: Boolean) {
        timeRemaining -= dt
        if (timeRemaining <= 0f) {
            isGameOver = true
            return
        }

        // Fosa de brea y cocodrilos con boca oscilante
        swampTimer += dt
        if (swampTimer > 2.5f) {
            swampOpen = !swampOpen
            swampTimer = 0f
        }

        // Tronco rodante
        logX += logVx * dt
        if (logX < -40f) {
            logX = w + 40f
        }

        // Movimiento en Liana
        val vinePivotX = w * 0.5f
        val vinePivotY = 40f
        val vineLength = groundY - vinePivotY - 20f

        if (onVine) {
            vineAngle += dt * 3.5f
            px = vinePivotX + kotlin.math.sin(vineAngle) * (vineLength - 10f)
            py = vinePivotY + kotlin.math.cos(vineAngle) * (vineLength - 10f)

            if (aDown) { // Soltarse de la liana
                onVine = false
                vy = -120f
            }
            return
        }

        // Agarre de liana con botón [B]
        if (bDown && kotlin.math.abs(px - vinePivotX) < 45f && py < groundY) {
            onVine = true
            vineAngle = -0.7f
            return
        }

        // Movimiento a pie
        val speed = 150f
        if (input.left) px -= speed * dt
        if (input.right) px += speed * dt

        // Salto
        if (aDown && onGround) {
            vy = -380f
            onGround = false
        }
        vy += 1000f * dt
        py += vy * dt

        if (py >= groundY) {
            py = groundY
            vy = 0f
            onGround = true
        }

        // Colisión con tronco rodante
        if (kotlin.math.abs(px - logX) < 22f && py >= groundY - 15f) {
            lives--
            px = 60f
            if (lives <= 0) isGameOver = true
        }

        // Caída en fosa de brea si está abierta y no está en liana
        val swampX1 = w * 0.4f
        val swampX2 = w * 0.6f
        if (px in swampX1..swampX2 && py >= groundY && swampOpen) {
            lives--
            px = 60f
            if (lives <= 0) isGameOver = true
        }

        // Recoger tesoro de pantalla
        val treasureX = w * 0.85f
        if (kotlin.math.abs(px - treasureX) < 25f && py >= groundY - 20f) {
            score += 250
            treasuresFound++
            px = 60f
            currentScreen++
            if (treasuresFound >= totalTreasures) {
                gameState = 3 // Victoria expedición
            }
        }

        // Cambio de pantalla al salir por la derecha
        if (px > w + 20f) {
            px = 20f
            currentScreen++
        }
    }

    override fun draw(canvas: Canvas) {
        when (gameState) {
            0 -> {
                NesDialogueRenderer.drawPrologueScreen(
                    canvas, w, h,
                    gameTitle = "PITFALL: MAYAN EXPEDITION",
                    missionTitle = "EXPEDICIÓN I: EL PANTANO SAGRADO",
                    prologueLines = listOf(
                        "• Año 1982. En lo profundo de la selva virgen,",
                        "  Pixel el explorador busca las reliquias de oro puro.",
                        "• Supera los pozos de alquitrán, usa las lianas",
                        "  y esquiva los troncos rodantes.",
                        "• OBJETIVO: Reúne 8 tesoros antes de que el cronómetro llegue a cero.",
                        "• CONTROLES: [A] Saltar / Soltar liana  [B] Agarrar liana"
                    ),
                    primaryColor = Color.parseColor("#805A20")
                )
            }
            2 -> {
                drawJungle(canvas)
                if (dialogueIndex < dialogueQueue.size) {
                    val (speaker, text) = dialogueQueue[dialogueIndex]
                    NesDialogueRenderer.drawDialogueBox(canvas, w, h, speaker, text, speakerColor = Color.parseColor("#E6B800"))
                }
            }
            1 -> {
                drawJungle(canvas)
            }
            3 -> {
                NesDialogueRenderer.drawPrologueScreen(
                    canvas, w, h,
                    gameTitle = "¡EXPEDICIÓN COMPLETADA!",
                    missionTitle = "LOS 8 TESOROS HAN SIDO RESCATADOS",
                    prologueLines = listOf(
                        "• ¡Pixel emerge de la jungla con los lingotes mayas intactos!",
                        "• Tiempo restante bonificado: ${timeRemaining.toInt()} seg.",
                        "• Puntuación de honor: $score pts.",
                        "• Nuevos templos subterráneos aguardan en el Acto II."
                    ),
                    primaryColor = Color.parseColor("#50A000")
                )
            }
        }

        if (isGameOver) {
            val pOver = Paint().apply { color = Color.parseColor("#CC150005"); style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), pOver)
            val pT = Paint().apply { color = Color.parseColor("#E63946"); textSize = 32f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
            canvas.drawText("HARRY QUEDÓ ATRAPADO EN LA SELVA", w / 2f, h / 2f - 10f, pT)
            pT.textSize = 14f; pT.color = Color.parseColor("#F5F5F5")
            canvas.drawText("PULSA [START] O [A] PARA REINTENTAR", w / 2f, h / 2f + 25f, pT)
        }
    }

    private fun drawJungle(canvas: Canvas) {
        // Fondo de copas de árboles y cielo tropical
        canvas.drawColor(Color.parseColor("#002800"))
        val pCanopy = Paint().apply { color = Color.parseColor("#005000") }
        canvas.drawRect(0f, 0f, w.toFloat(), 60f, pCanopy)

        // Tierra y estrato subterráneo
        val pDirt = Paint().apply { color = Color.parseColor("#805A20") }
        canvas.drawRect(0f, groundY, w.toFloat(), h.toFloat(), pDirt)
        val pGrass = Paint().apply { color = Color.parseColor("#00A800"); strokeWidth = 4f }
        canvas.drawLine(0f, groundY, w.toFloat(), groundY, pGrass)

        // Pantano de brea / Cocodrilos en el centro
        val swampX1 = w * 0.4f
        val swampX2 = w * 0.6f
        val pSwamp = Paint().apply { color = if (swampOpen) Color.parseColor("#150A05") else Color.parseColor("#007030") }
        canvas.drawRect(swampX1, groundY, swampX2, groundY + 25f, pSwamp)

        // Liana oscilante
        val vinePivotX = w * 0.5f
        val vinePivotY = 40f
        val vineEndX = if (onVine) px else vinePivotX + kotlin.math.sin(vineAngle) * (groundY - vinePivotY - 20f)
        val vineEndY = if (onVine) py else vinePivotY + kotlin.math.cos(vineAngle) * (groundY - vinePivotY - 20f)
        val pVine = Paint().apply { color = Color.parseColor("#50A000"); strokeWidth = 3f }
        canvas.drawLine(vinePivotX, vinePivotY, vineEndX, vineEndY, pVine)

        // Tronco rodante
        val pLog = Paint().apply { color = Color.parseColor("#5A3010") }
        canvas.drawRoundRect(RectF(logX - 14f, groundY - 14f, logX + 14f, groundY), 4f, 4f, pLog)

        // Tesoro de oro
        val treasureX = w * 0.85f
        val pGold = Paint().apply { color = Color.parseColor("#E6B800") }
        canvas.drawRect(treasureX - 10f, groundY - 16f, treasureX + 10f, groundY, pGold)
        val pSpark = Paint().apply { color = Color.parseColor("#FFFFFF") }
        canvas.drawCircle(treasureX, groundY - 10f, 3f, pSpark)

        // Pixel el explorador
        val pPixel = Paint().apply { color = Color.parseColor("#F5C542") }
        canvas.drawRect(px - 7f, py - 20f, px + 7f, py - 10f, pPixel) // Camisa safari
        val pPants = Paint().apply { color = Color.parseColor("#3C44AA") }
        canvas.drawRect(px - 6f, py - 10f, px + 6f, py, pPants)
        val pHat = Paint().apply { color = Color.parseColor("#805A20") }
        canvas.drawRect(px - 8f, py - 24f, px + 8f, py - 20f, pHat)

        // HUD
        pText.color = Color.parseColor("#F8F8F8")
        canvas.drawText("HARRY  VIDAS: $lives  TESOROS: $treasuresFound/$totalTreasures  TIEMPO: ${timeRemaining.toInt()}s", 12f, 18f, pText)
        pText.color = Color.parseColor("#E6B800")
        canvas.drawText("PANTALLA: $currentScreen | SCORE: $score", 12f, 32f, pText)
    }
}
