package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Pixel Charades: juego de adivinanza de palabras estilo "Heads Up!".
 *
 * Mecánica:
 *  · El jugador se pone el celular en la frente; los demás ven la palabra.
 *  · Deslizar hacia arriba (o A) = correcto (+1 punto, siguiente palabra).
 *  · Deslizar hacia abajo (o B) = incorrecto (siguiente palabra, sin punto).
 *  · Tiempo determinado por ronda (60s). Quien más palabras adivine gana.
 *  · Múltiples personajes/jugadores con nombres personalizados.
 *  · Varias categorías de palabras (animales, películas, países, etc.).
 *  · Pantalla de configuración: elegir jugadores, categoría, tiempo.
 *  · Resultados con ranking y ganador.
 */
class PixelCharadesGame : Game {
    override val name = "Pixel Charades"
    override val genre = "Party"

    private var w = 0
    private var h = 0

    // Estados del juego
    private enum class State { SETUP, PLAYING, RESULT, GAME_OVER }
    private var state = State.SETUP

    // Configuración
    private var numPlayers = 2
    private val playerNames = mutableListOf("Jugador 1", "Jugador 2")
    private var currentPlayer = 0
    private var selectedCategory = 0
    private var roundTime = 60f  // segundos por ronda
    private var timeLeft = 60f

    // Puntuación
    private val scores = mutableListOf(0, 0)
    private var currentWord = ""
    private var wordsGuessed = 0
    private var wordsSkipped = 0
    private var totalRounds = 0
    private val maxRounds = 3  // 3 rondas por jugador

    // Animación de feedback
    private var feedbackTimer = 0f
    private var feedbackText = ""
    private var feedbackColor = Color.WHITE
    private var wordScale = 1f
    private var wordScaleTimer = 0f

    // Categorías de palabras
    private val categories = listOf(
        "Animales" to listOf(
            "ELEFANTE", "JIRAFA", "DELFIN", "PINGUINO", "COCODRILO", "KANGURO", "GORILA",
            "TIGRE", "LEON", "CEBRA", "RINOCERONTE", "HIPOPOTAMO", "OSO", "LOBO", "ZORRO",
            "AGUILA", "BUHO", "FLAMENCO", "PEZ ESPADA", "TORTUGA", "SERPIENTE", "ARAÑA",
            "MARIPOSA", "ABEJA", "HORMIGA", "CANGREJO", "PULPO", "MEDUSA", "CABALLITO DE MAR"
        ),
        "Películas" to listOf(
            "TITANIC", "AVATAR", "STAR WARS", "JURASSIC PARK", "HARRY POTTER", "MATRIX",
            "EL PADRINO", "FORREST GUMP", "EL REY LEON", "TOY STORY", "FROZEN", "COCO",
            "ENCANTO", "MOANA", "UP", "WALL-E", "FINDING NEMO", "SHREK", "KUNG FU PANDA",
            "MADAGASCAR", "ICE AGE", "RATATOUILLE", "CARS", "BRAVE", "TANGLED", "ZOOTOPIA"
        ),
        "Países" to listOf(
            "MEXICO", "BRASIL", "ARGENTINA", "COLOMBIA", "CHILE", "PERU", "VENEZUELA",
            "ECUADOR", "BOLIVIA", "URUGUAY", "PARAGUAY", "ESPAÑA", "FRANCIA", "ITALIA",
            "ALEMANIA", "JAPON", "CHINA", "INDIA", "RUSIA", "AUSTRALIA", "EGIPTO", "CANADA",
            "ESTADOS UNIDOS", "REINO UNIDO", "PORTUGAL", "GRECIA", "TURQUIA", "COREA"
        ),
        "Comida" to listOf(
            "PIZZA", "HAMBURGUESA", "TACOS", "SUSHI", "PAELLA", "LASAÑA", "RAMEN",
            "CEVICHE", "AREPAS", "EMPANADAS", "QUESADILLA", "BURRITO", "NACHOS", "GUACAMOLE",
            "CHURROS", "FLAN", "TIRAMISU", "CHEESECAKE", "BROWNIE", "HELADO", "CHOCOLATE",
            "CROISSANT", "BAGUETTE", "PASTA", "RISOTTO", "CURRY", "PAD THAI"
        ),
        "Profesiones" to listOf(
            "MEDICO", "ABOGADO", "INGENIERO", "PROFESOR", "BOMBERO", "POLICIA", "COCINERO",
            "PILOTO", "ASTRONAUTA", "CIENTIFICO", "ARTISTA", "MUSICO", "ACTOR", "PERIODISTA",
            "ARQUITECTO", "DENTISTA", "VETERINARIO", "PSICOLOGO", "CONTADOR", "PROGRAMADOR",
            "DISEÑADOR", "FOTOGRAFO", "MECANICO", "ELECTRICISTA", "CARPINTERO", "JARDINERO"
        ),
        "Deportes" to listOf(
            "FUTBOL", "BALONCESTO", "TENIS", "GOLF", "NATACION", "BOXEO", "CICLISMO",
            "ATLETISMO", "GIMNASIA", "VOLEIBOL", "BEISBOL", "RUGBY", "HOCKEY", "ESQUI",
            "SURF", "KARATE", "JUDO", "ESGRIMA", "TIRO CON ARCO", "HALTEROFILIA", "REMOTO",
            "TRIATLON", "MARATON", "POLO", "CRICKET", "BADMINTON", "PING PONG"
        )
    )

    // Paints
    private val bgPaint = paintOf("#1A1A2E")
    private val cardPaint = paintOf("#2A2A4E")
    private val textPaint = paintOf("#F8F8F8").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val titlePaint = paintOf("#F5C542").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val greenPaint = paintOf("#00E676").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val redPaint = paintOf("#FF5252").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallPaint = paintOf("#B8B8C8").apply { textAlign = Paint.Align.CENTER }
    private val hudPaint = paintOf("#F5C542").apply { textAlign = Paint.Align.LEFT; isFakeBoldText = true }
    private fun paintOf(hex: String): Paint = Paint().apply {
        setColor(Color.parseColor(hex)); isAntiAlias = true
    }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        reset()
    }

    override fun reset() {
        state = State.SETUP
        currentPlayer = 0
        totalRounds = 0
        scores.clear()
        for (i in 0 until numPlayers) scores.add(0)
        timeLeft = roundTime
        wordsGuessed = 0
        wordsSkipped = 0
        currentWord = ""
        feedbackTimer = 0f
        wordScale = 1f
        wordScaleTimer = 0f
    }

    private fun startRound() {
        state = State.PLAYING
        timeLeft = roundTime
        wordsGuessed = 0
        wordsSkipped = 0
        nextWord()
    }

    private fun nextWord() {
        val words = categories[selectedCategory].second
        currentWord = words.random()
        wordScale = 0f
        wordScaleTimer = 0f
    }

    private fun endRound() {
        scores[currentPlayer] += wordsGuessed
        state = State.RESULT
        feedbackTimer = 0f
    }

    private fun nextPlayer() {
        currentPlayer++
        if (currentPlayer >= numPlayers) {
            currentPlayer = 0
            totalRounds++
            if (totalRounds >= maxRounds) {
                state = State.GAME_OVER
            } else {
                state = State.SETUP
            }
        } else {
            state = State.SETUP
        }
    }

    override fun update(dt: Float, input: GameInput) {
        // Animación de escala de palabra
        if (wordScale < 1f) {
            wordScaleTimer += dt
            wordScale = (wordScaleTimer * 3f).coerceAtMost(1f)
        }

        // Feedback timer
        if (feedbackTimer > 0f) {
            feedbackTimer -= dt
        }

        when (state) {
            State.SETUP -> {
                // Navegación en setup
                if (input.up) { selectedCategory = (selectedCategory - 1 + categories.size) % categories.size; input.up = false }
                if (input.down) { selectedCategory = (selectedCategory + 1) % categories.size; input.down = false }
                if (input.left) { numPlayers = (numPlayers - 1).coerceAtLeast(2); adjustPlayers(); input.left = false }
                if (input.right) { numPlayers = (numPlayers + 1).coerceAtMost(6); adjustPlayers(); input.right = false }
                if (input.a || input.start) { startRound(); input.a = false; input.start = false }
            }
            State.PLAYING -> {
                timeLeft -= dt
                if (timeLeft <= 0f) {
                    timeLeft = 0f
                    endRound()
                    return
                }
                // Correcto (swipe up / A)
                if (input.up || input.a) {
                    wordsGuessed++
                    feedbackText = "¡CORRECTO! +1"
                    feedbackColor = Color.parseColor("#00E676")
                    feedbackTimer = 0.8f
                    nextWord()
                    input.up = false; input.a = false
                }
                // Incorrecto (swipe down / B)
                if (input.down || input.b) {
                    wordsSkipped++
                    feedbackText = "SALTAR"
                    feedbackColor = Color.parseColor("#FF5252")
                    feedbackTimer = 0.5f
                    nextWord()
                    input.down = false; input.b = false
                }
            }
            State.RESULT -> {
                if (input.start || input.a) {
                    nextPlayer()
                    input.start = false; input.a = false
                }
            }
            State.GAME_OVER -> {
                if (input.start || input.a) reset()
            }
        }
    }

    private fun adjustPlayers() {
        while (scores.size < numPlayers) scores.add(0)
        while (scores.size > numPlayers) scores.removeAt(scores.size - 1)
        while (playerNames.size < numPlayers) playerNames.add("Jugador ${playerNames.size + 1}")
        while (playerNames.size > numPlayers) playerNames.removeAt(playerNames.size - 1)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))

        when (state) {
            State.SETUP -> drawSetup(canvas)
            State.PLAYING -> drawPlaying(canvas)
            State.RESULT -> drawResult(canvas)
            State.GAME_OVER -> drawGameOver(canvas)
        }
    }

    private fun drawSetup(canvas: Canvas) {
        titlePaint.textSize = 42f
        canvas.drawText("PIXEL CHARADES", w / 2f, 60f, titlePaint)

        smallPaint.textSize = 18f
        canvas.drawText("Configuración de partida", w / 2f, 90f, smallPaint)

        // Jugadores
        hudPaint.textSize = 22f
        canvas.drawText("Jugadores: $numPlayers  (◀ ▶ para cambiar)", w / 2f, 140f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT

        // Nombres de jugadores
        smallPaint.textSize = 16f
        for (i in 0 until numPlayers) {
            canvas.drawText("${i + 1}. ${playerNames[i]}", w / 2f, 170f + i * 22f, smallPaint)
        }

        // Categoría
        titlePaint.textSize = 24f
        canvas.drawText("Categoría: ${categories[selectedCategory].first}", w / 2f, 170f + numPlayers * 22f + 30f, titlePaint)
        smallPaint.textSize = 16f
        canvas.drawText("(▲ ▼ para cambiar)", w / 2f, 170f + numPlayers * 22f + 55f, smallPaint)

        // Tiempo
        canvas.drawText("Tiempo por ronda: ${roundTime.toInt()}s", w / 2f, 170f + numPlayers * 22f + 90f, smallPaint)

        // Instrucciones
        greenPaint.textSize = 20f
        canvas.drawText("PULSA A O START PARA COMENZAR", w / 2f, h - 30f, greenPaint)
        smallPaint.textSize = 14f
        canvas.drawText("▲▼: categoría  ·  ◀▶: jugadores  ·  A/START: jugar", w / 2f, h - 10f, smallPaint)
    }

    private fun drawPlaying(canvas: Canvas) {
        // Fondo de color según feedback
        if (feedbackTimer > 0f) {
            val fbAlpha = (feedbackTimer / 0.8f * 80).toInt().coerceIn(0, 80)
            val fbPaint = Paint().apply { color = feedbackColor; this.alpha = fbAlpha; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), fbPaint)
        }

        // Jugador actual
        titlePaint.textSize = 28f
        canvas.drawText("TURNO DE: ${playerNames[currentPlayer]}", w / 2f, 50f, titlePaint)

        // Tiempo
        val timeColor = if (timeLeft < 10f) "#FF5252" else "#F5C542"
        titlePaint.textSize = 48f
        titlePaint.color = Color.parseColor(timeColor)
        canvas.drawText("${timeLeft.toInt()}s", w / 2f, 110f, titlePaint)
        titlePaint.color = Color.parseColor("#F5C542")

        // Palabra (grande, centrada, escala animada)
        textPaint.textSize = (w * 0.12f * wordScale).coerceAtLeast(20f)
        canvas.drawText(currentWord, w / 2f, h / 2f + 20f, textPaint)

        // Categoría
        smallPaint.textSize = 16f
        canvas.drawText("Categoría: ${categories[selectedCategory].first}", w / 2f, h / 2f + 60f, smallPaint)

        // Puntuación de esta ronda
        smallPaint.textSize = 18f
        canvas.drawText("Adivinadas: $wordsGuessed  ·  Saltadas: $wordsSkipped", w / 2f, h - 60f, smallPaint)

        // Feedback
        if (feedbackTimer > 0f) {
            textPaint.textSize = 32f
            textPaint.color = feedbackColor
            canvas.drawText(feedbackText, w / 2f, h / 2f - 60f, textPaint)
            textPaint.color = Color.parseColor("#F8F8F8")
        }

        // Instrucciones
        smallPaint.textSize = 14f
        canvas.drawText("▲/A: CORRECTO  ·  ▼/B: SALTAR  ·  START: PAUSA", w / 2f, h - 20f, smallPaint)
    }

    private fun drawResult(canvas: Canvas) {
        titlePaint.textSize = 36f
        canvas.drawText("FIN DE RONDA", w / 2f, 60f, titlePaint)

        titlePaint.textSize = 28f
        canvas.drawText("${playerNames[currentPlayer]}", w / 2f, 100f, titlePaint)

        textPaint.textSize = 48f
        textPaint.color = Color.parseColor("#00E676")
        canvas.drawText("$wordsGuessed", w / 2f, 170f, textPaint)
        textPaint.color = Color.parseColor("#F8F8F8")
        textPaint.textSize = 20f
        canvas.drawText("palabras adivinadas", w / 2f, 200f, textPaint)

        smallPaint.textSize = 18f
        canvas.drawText("Saltadas: $wordsSkipped", w / 2f, 230f, smallPaint)

        // Ranking parcial
        titlePaint.textSize = 22f
        canvas.drawText("PUNTUACIONES", w / 2f, 280f, titlePaint)
        for (i in 0 until numPlayers) {
            val marker = if (i == currentPlayer) " ◀" else ""
            smallPaint.textSize = 18f
            canvas.drawText("${playerNames[i]}: ${scores[i]}$marker", w / 2f, 310f + i * 24f, smallPaint)
        }

        greenPaint.textSize = 20f
        canvas.drawText("PULSA A O START PARA CONTINUAR", w / 2f, h - 30f, greenPaint)
    }

    private fun drawGameOver(canvas: Canvas) {
        titlePaint.textSize = 42f
        canvas.drawText("¡JUEGO TERMINADO!", w / 2f, 60f, titlePaint)

        // Ganador
        val maxScore = scores.maxOrNull() ?: 0
        val winnerIndex = scores.indexOf(maxScore)
        titlePaint.textSize = 36f
        titlePaint.color = Color.parseColor("#F5C542")
        canvas.drawText("GANADOR: ${playerNames[winnerIndex]}", w / 2f, 120f, titlePaint)
        titlePaint.color = Color.parseColor("#F5C542")

        textPaint.textSize = 64f
        textPaint.color = Color.parseColor("#00E676")
        canvas.drawText("$maxScore", w / 2f, 200f, textPaint)
        textPaint.color = Color.parseColor("#F8F8F8")
        textPaint.textSize = 20f
        canvas.drawText("palabras adivinadas", w / 2f, 230f, textPaint)

        // Ranking final
        titlePaint.textSize = 22f
        canvas.drawText("RANKING FINAL", w / 2f, 290f, titlePaint)
        val sorted = scores.indices.sortedByDescending { scores[it] }
        for ((pos, i) in sorted.withIndex()) {
            val medal = when (pos) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${pos + 1}." }
            smallPaint.textSize = 18f
            canvas.drawText("$medal ${playerNames[i]}: ${scores[i]}", w / 2f, 320f + pos * 24f, smallPaint)
        }

        greenPaint.textSize = 20f
        canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h - 30f, greenPaint)
    }

    override val isGameOver: Boolean get() = false
    override val score: Int get() = scores.getOrElse(currentPlayer) { 0 }
}
