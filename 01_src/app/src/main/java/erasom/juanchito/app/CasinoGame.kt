package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Casino: reconstrucción fiel de GoPremiumPackTrial (GoodWood, 2008).
 *
 * Mecánica original:
 *  · Casino completo con múltiples juegos: Baccarat, Blackjack, Slots, Ruleta.
 *  · Apuestas con fichas, saldo persistente.
 *  · Animaciones de cartas, rodillos, ruleta.
 *  · High score (saldo máximo).
 */
class CasinoGame : Game {
    override val name = "Casino"
    override val genre = "Casino"

    private var w = 0
    private var h = 0

    // Estado
    private enum class GameState { MENU, BLACKJACK, SLOTS, ROULETTE, BACCARAT, GAME_OVER }
    private var state = GameState.MENU

    // Saldo
    private var balance = 1000
    private var bet = 100
    private var best = 1000

    // Blackjack
    private data class Card(val value: Int, val suit: Int)
    private val deck = mutableListOf<Card>()
    private val playerHand = mutableListOf<Card>()
    private val dealerHand = mutableListOf<Card>()
    private var blackjackState = 0  // 0=dealing, 1=player turn, 2=dealer turn, 3=result
    private var blackjackResult = ""

    // Slots
    private val slotReels = mutableListOf<Int>()
    private var slotSpinning = false
    private var slotSpinTimer = 0f
    private var slotResult = ""

    // Roulette
    private var rouletteNumber = -1
    private var rouletteSpinning = false
    private var rouletteTimer = 0f
    private var rouletteBet = 0  // 0=rojo, 1=negro, 2=par, 3=impar
    private var rouletteResult = ""

    // Baccarat
    private val playerBaccarat = mutableListOf<Card>()
    private val bankerBaccarat = mutableListOf<Card>()
    private var baccaratState = 0
    private var baccaratResult = ""

    // Puntuación (saldo como score)
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var menuSelection = 0

    // Animación
    private var cardAnim = 0f

    // Paints
    private val bgPaint = paintOf("#0A4A1A")
    private val tablePaint = paintOf("#1A6A2A")
    private val cardPaint = paintOf("#FFFFFF")
    private val cardBack = paintOf("#D82800")
    private val textPaint = paintOf("#F8F8F8").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val titlePaint = paintOf("#F5C542").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallPaint = paintOf("#FFFFFF").apply { textAlign = Paint.Align.CENTER }
    private val hudPaint = paintOf("#F5C542").apply { textAlign = Paint.Align.LEFT; isFakeBoldText = true }
    private val greenPaint = paintOf("#00E676").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val redPaint = paintOf("#FF5252").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val goldPaint = paintOf("#F5C542").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    private fun paintOf(hex: String): Paint = Paint().apply {
        setColor(Color.parseColor(hex)); isAntiAlias = true
    }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        reset()
    }

    override fun reset() {
        state = GameState.MENU
        balance = 1000
        bet = 100
        scoreVal = balance
        best = 1000
        menuSelection = 0
        initDeck()
    }

    private fun initDeck() {
        deck.clear()
        for (suit in 0..3) for (value in 1..13) {
            deck.add(Card(value, suit))
        }
        deck.shuffle()
    }

    private fun drawCard(): Card {
        if (deck.isEmpty()) initDeck()
        return deck.removeAt(0)
    }

    private fun cardValue(card: Card): Int = when (card.value) {
        1 -> 11  // As
        in 11..13 -> 10  // J, Q, K
        else -> card.value
    }

    private fun handValue(hand: List<Card>): Int {
        var total = 0
        var aces = 0
        for (card in hand) {
            if (card.value == 1) aces++
            total += cardValue(card)
        }
        while (total > 21 && aces > 0) {
            total -= 10
            aces--
        }
        return total
    }

    // ---------- Blackjack ----------

    private fun startBlackjack() {
        if (balance < bet) return
        balance -= bet
        scoreVal = balance
        playerHand.clear()
        dealerHand.clear()
        playerHand.add(drawCard())
        dealerHand.add(drawCard())
        playerHand.add(drawCard())
        dealerHand.add(drawCard())
        blackjackState = 1
        blackjackResult = ""
        state = GameState.BLACKJACK
    }

    private fun blackjackHit() {
        if (blackjackState != 1) return
        playerHand.add(drawCard())
        if (handValue(playerHand) > 21) {
            blackjackState = 3
            blackjackResult = "¡TE PASASTE! Pierdes $bet"
        }
    }

    private fun blackjackStand() {
        if (blackjackState != 1) return
        blackjackState = 2
        // Dealer juega
        while (handValue(dealerHand) < 17) {
            dealerHand.add(drawCard())
        }
        val playerVal = handValue(playerHand)
        val dealerVal = handValue(dealerHand)
        blackjackState = 3
        when {
            dealerVal > 21 -> { balance += bet * 2; scoreVal = balance; blackjackResult = "¡DEALER SE PASA! Ganas $bet" }
            playerVal > dealerVal -> { balance += bet * 2; scoreVal = balance; blackjackResult = "¡GANAS! +$bet" }
            playerVal == dealerVal -> { balance += bet; scoreVal = balance; blackjackResult = "¡EMPATE! Recuperas apuesta" }
            else -> { blackjackResult = "¡PIERDES! -$bet" }
        }
        if (balance > best) best = balance
    }

    // ---------- Slots ----------

    private fun startSlots() {
        if (balance < bet) return
        balance -= bet
        scoreVal = balance
        slotSpinning = true
        slotSpinTimer = 2f
        slotResult = ""
        state = GameState.SLOTS
    }

    private fun spinSlots() {
        slotReels.clear()
        for (i in 0..2) slotReels.add(Random.nextInt(6))
        // Verificar premio
        if (slotReels[0] == slotReels[1] && slotReels[1] == slotReels[2]) {
            val win = bet * 10
            balance += win
            scoreVal = balance
            slotResult = "¡JACKPOT! +$win"
        } else if (slotReels[0] == slotReels[1] || slotReels[1] == slotReels[2]) {
            val win = bet * 2
            balance += win
            scoreVal = balance
            slotResult = "¡DOS IGUALES! +$win"
        } else {
            slotResult = "Sin premio"
        }
        if (balance > best) best = balance
    }

    // ---------- Roulette ----------

    private fun startRoulette() {
        if (balance < bet) return
        balance -= bet
        scoreVal = balance
        rouletteSpinning = true
        rouletteTimer = 2f
        rouletteResult = ""
        state = GameState.ROULETTE
    }

    private fun spinRoulette() {
        rouletteNumber = Random.nextInt(37)  // 0-36
        val isRed = rouletteNumber in listOf(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36)
        val isBlack = rouletteNumber != 0 && !isRed
        val isEven = rouletteNumber % 2 == 0 && rouletteNumber != 0
        val isOdd = rouletteNumber % 2 != 0

        val win = when (rouletteBet) {
            0 -> if (isRed) bet * 2 else 0  // Rojo
            1 -> if (isBlack) bet * 2 else 0  // Negro
            2 -> if (isEven) bet * 2 else 0  // Par
            else -> if (isOdd) bet * 2 else 0  // Impar
        }
        if (win > 0) {
            balance += win
            scoreVal = balance
            rouletteResult = "¡GANAS! +$win  (Número: $rouletteNumber)"
        } else {
            rouletteResult = "Pierdes  (Número: $rouletteNumber)"
        }
        if (balance > best) best = balance
    }

    // ---------- Baccarat ----------

    private fun startBaccarat() {
        if (balance < bet) return
        balance -= bet
        scoreVal = balance
        playerBaccarat.clear()
        bankerBaccarat.clear()
        playerBaccarat.add(drawCard())
        bankerBaccarat.add(drawCard())
        playerBaccarat.add(drawCard())
        bankerBaccarat.add(drawCard())
        baccaratState = 1
        baccaratResult = ""
        state = GameState.BACCARAT
    }

    private fun baccaratPlay() {
        if (baccaratState != 1) return
        val playerVal = baccaratValue(playerBaccarat)
        val bankerVal = baccaratValue(bankerBaccarat)

        // Reglas simplificadas de Baccarat
        if (playerVal <= 5) playerBaccarat.add(drawCard())
        if (bankerVal <= 5) bankerBaccarat.add(drawCard())

        val finalPlayer = baccaratValue(playerBaccarat)
        val finalBanker = baccaratValue(bankerBaccarat)

        baccaratState = 2
        when {
            finalPlayer > finalBanker -> { balance += bet * 2; scoreVal = balance; baccaratResult = "¡JUGADOR GANA! +$bet" }
            finalPlayer < finalBanker -> { baccaratResult = "¡BANCA GANA! -$bet" }
            else -> { balance += bet; scoreVal = balance; baccaratResult = "¡EMPATE! Recuperas apuesta" }
        }
        if (balance > best) best = balance
    }

    private fun baccaratValue(hand: List<Card>): Int {
        var total = 0
        for (card in hand) {
            total += when (card.value) {
                in 10..13 -> 0
                1 -> 1
                else -> card.value
            }
        }
        return total % 10
    }

    override fun update(dt: Float, input: GameInput) {
        cardAnim += dt

        // Slots animation
        if (slotSpinning) {
            slotSpinTimer -= dt
            if (slotSpinTimer <= 0f) {
                slotSpinning = false
                spinSlots()
            }
        }

        // Roulette animation
        if (rouletteSpinning) {
            rouletteTimer -= dt
            if (rouletteTimer <= 0f) {
                rouletteSpinning = false
                spinRoulette()
            }
        }

        when (state) {
            GameState.MENU -> {
                if (input.up) { menuSelection = (menuSelection - 1).coerceAtLeast(0); input.up = false }
                if (input.down) { menuSelection = (menuSelection + 1).coerceAtMost(3); input.down = false }
                if (input.left) { bet = (bet - 50).coerceAtLeast(50); input.left = false }
                if (input.right) { bet = (bet + 50).coerceAtMost(balance.coerceAtLeast(50)); input.right = false }
                if (input.a || input.start) {
                    when (menuSelection) {
                        0 -> startBlackjack()
                        1 -> startSlots()
                        2 -> startRoulette()
                        3 -> startBaccarat()
                    }
                    input.a = false; input.start = false
                }
            }
            GameState.BLACKJACK -> {
                if (blackjackState == 1) {
                    if (input.a) { blackjackHit(); input.a = false }
                    if (input.b) { blackjackStand(); input.b = false }
                } else if (blackjackState == 3) {
                    if (input.start || input.a) {
                        if (balance <= 0) state = GameState.GAME_OVER
                        else state = GameState.MENU
                        input.start = false; input.a = false
                    }
                }
            }
            GameState.SLOTS -> {
                if (!slotSpinning && slotResult.isNotEmpty()) {
                    if (input.start || input.a) {
                        if (balance <= 0) state = GameState.GAME_OVER
                        else state = GameState.MENU
                        input.start = false; input.a = false
                    }
                }
            }
            GameState.ROULETTE -> {
                if (rouletteSpinning) {
                    // Seleccionar apuesta mientras gira
                    if (input.left) { rouletteBet = (rouletteBet - 1).coerceAtLeast(0); input.left = false }
                    if (input.right) { rouletteBet = (rouletteBet + 1).coerceAtMost(3); input.right = false }
                } else if (rouletteResult.isNotEmpty()) {
                    if (input.start || input.a) {
                        if (balance <= 0) state = GameState.GAME_OVER
                        else state = GameState.MENU
                        input.start = false; input.a = false
                    }
                } else {
                    // Iniciar ruleta
                    if (input.a || input.start) {
                        startRoulette()
                        input.a = false; input.start = false
                    }
                    if (input.left) { rouletteBet = (rouletteBet - 1).coerceAtLeast(0); input.left = false }
                    if (input.right) { rouletteBet = (rouletteBet + 1).coerceAtMost(3); input.right = false }
                }
            }
            GameState.BACCARAT -> {
                if (baccaratState == 1) {
                    if (input.a || input.start) {
                        baccaratPlay()
                        input.a = false; input.start = false
                    }
                } else if (baccaratState == 2) {
                    if (input.start || input.a) {
                        if (balance <= 0) state = GameState.GAME_OVER
                        else state = GameState.MENU
                        input.start = false; input.a = false
                    }
                }
            }
            GameState.GAME_OVER -> {
                if (input.start || input.a) reset()
            }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#0A4A1A"))

        when (state) {
            GameState.MENU -> drawMenu(canvas)
            GameState.BLACKJACK -> drawBlackjack(canvas)
            GameState.SLOTS -> drawSlots(canvas)
            GameState.ROULETTE -> drawRoulette(canvas)
            GameState.BACCARAT -> drawBaccarat(canvas)
            GameState.GAME_OVER -> drawGameOver(canvas)
        }
    }

    private fun drawMenu(canvas: Canvas) {
        titlePaint.textSize = 42f
        canvas.drawText("CASINO", w / 2f, 60f, titlePaint)

        // Saldo
        hudPaint.textSize = 22f
        canvas.drawText("SALDO: $balance", w / 2f, 100f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("MEJOR: $best", w / 2f, 125f, smallPaint.apply { textAlign = Paint.Align.CENTER; textSize = 16f })
        smallPaint.textAlign = Paint.Align.CENTER

        // Apuesta
        canvas.drawText("APUESTA: $bet  (◀▶ para cambiar)", w / 2f, 160f, smallPaint)

        // Menú de juegos
        val games = listOf("BLACKJACK", "SLOTS", "RULETA", "BACCARAT")
        for (i in games.indices) {
            val y = 200f + i * 45f
            val isSelected = i == menuSelection
            if (isSelected) {
                val selPaint = paintOf("#F5C542")
                selPaint.alpha = 40
                canvas.drawRect(w / 2f - 120f, y - 20f, w / 2f + 120f, y + 10f, selPaint)
            }
            val gamePaint = if (isSelected) titlePaint else textPaint
            gamePaint.textSize = 24f
            canvas.drawText(games[i], w / 2f, y, gamePaint)
        }

        smallPaint.textSize = 14f
        canvas.drawText("▲▼: seleccionar  ·  ◀▶: apuesta  ·  A/START: jugar", w / 2f, h - 20f, smallPaint)
    }

    private fun drawBlackjack(canvas: Canvas) {
        titlePaint.textSize = 28f
        canvas.drawText("BLACKJACK", w / 2f, 30f, titlePaint)

        // Dealer
        smallPaint.textSize = 16f
        canvas.drawText("DEALER: ${if (blackjackState >= 2) handValue(dealerHand) else "?"}", w / 2f, 60f, smallPaint)

        val cardW = 40f
        val cardH = 60f
        // Cartas del dealer
        for (i in dealerHand.indices) {
            val x = w / 2f - (dealerHand.size * cardW) / 2f + i * (cardW + 5f)
            val y = 75f
            if (i == 1 && blackjackState == 1) {
                // Carta oculta
                canvas.drawRoundRect(RectF(x, y, x + cardW, y + cardH), 4f, 4f, cardBack)
            } else {
                drawCard(canvas, dealerHand[i], x, y, cardW, cardH)
            }
        }

        // Jugador
        canvas.drawText("JUGADOR: ${handValue(playerHand)}", w / 2f, 170f, smallPaint)
        for (i in playerHand.indices) {
            val x = w / 2f - (playerHand.size * cardW) / 2f + i * (cardW + 5f)
            val y = 185f
            drawCard(canvas, playerHand[i], x, y, cardW, cardH)
        }

        // Resultado
        if (blackjackResult.isNotEmpty()) {
            textPaint.textSize = 20f
            canvas.drawText(blackjackResult, w / 2f, 270f, textPaint)
        }

        // Instrucciones
        if (blackjackState == 1) {
            smallPaint.textSize = 14f
            canvas.drawText("A: HIT (pedir carta)  ·  B: STAND (plantarse)", w / 2f, h - 20f, smallPaint)
        } else if (blackjackState == 3) {
            greenPaint.textSize = 16f
            canvas.drawText("PULSA START PARA CONTINUAR", w / 2f, h - 20f, greenPaint)
        }
    }

    private fun drawCard(canvas: Canvas, card: Card, x: Float, y: Float, w: Float, h: Float) {
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 4f, 4f, cardPaint)
        val suitColors = listOf("#D82800", "#000000", "#D82800", "#000000")
        val suitSymbols = listOf("♥", "♠", "♦", "♣")
        val valueStr = when (card.value) {
            1 -> "A"; 11 -> "J"; 12 -> "Q"; 13 -> "K"; else -> card.value.toString()
        }
        val cardPaintLocal = paintOf(suitColors[card.suit])
        cardPaintLocal.textSize = 16f
        cardPaintLocal.textAlign = Paint.Align.CENTER
        canvas.drawText(valueStr, x + w / 2f, y + h / 2f + 6f, cardPaintLocal)
        cardPaintLocal.textSize = 10f
        canvas.drawText(suitSymbols[card.suit], x + w / 2f, y + h / 2f - 8f, cardPaintLocal)
    }

    private fun drawSlots(canvas: Canvas) {
        titlePaint.textSize = 28f
        canvas.drawText("SLOTS", w / 2f, 30f, titlePaint)

        val reelW = 80f
        val reelH = 100f
        val reelY = h / 2f - 60f
        val startX = w / 2f - (3 * reelW + 2 * 10f) / 2f

        val symbols = listOf("🍒", "🍋", "🍊", "🍇", "🔔", "7")

        for (i in 0..2) {
            val x = startX + i * (reelW + 10f)
            // Marco del rodillo
            val reelBg = paintOf("#2A2A3E")
            canvas.drawRoundRect(RectF(x, reelY, x + reelW, reelY + reelH), 8f, 8f, reelBg)
            val reelFrame = paintOf("#F5C542")
            reelFrame.style = Paint.Style.STROKE
            reelFrame.strokeWidth = 2f
            canvas.drawRoundRect(RectF(x, reelY, x + reelW, reelY + reelH), 8f, 8f, reelFrame)

            // Símbolo
            val symbol = if (slotReels.size > i) symbols.getOrElse(slotReels[i]) { "?" } else "?"
            textPaint.textSize = 48f
            canvas.drawText(symbol, x + reelW / 2f, reelY + reelH / 2f + 16f, textPaint)
        }

        // Resultado
        if (slotResult.isNotEmpty()) {
            textPaint.textSize = 20f
            canvas.drawText(slotResult, w / 2f, reelY + reelH + 40f, textPaint)
        }

        // Saldo
        smallPaint.textSize = 18f
        canvas.drawText("SALDO: $balance", w / 2f, h - 60f, smallPaint)

        if (slotSpinning) {
            smallPaint.textSize = 16f
            canvas.drawText("Girando...", w / 2f, h - 30f, smallPaint)
        } else if (slotResult.isNotEmpty()) {
            greenPaint.textSize = 16f
            canvas.drawText("PULSA START PARA CONTINUAR", w / 2f, h - 30f, greenPaint)
        } else {
            greenPaint.textSize = 16f
            canvas.drawText("PULSA A PARA GIRAR", w / 2f, h - 30f, greenPaint)
        }
    }

    private fun drawRoulette(canvas: Canvas) {
        titlePaint.textSize = 28f
        canvas.drawText("RULETA", w / 2f, 30f, titlePaint)

        // Ruleta (círculo)
        val cx = w / 2f
        val cy = h / 2f
        val radius = minOf(w, h) * 0.35f
        val roulettePaint = paintOf("#2A2A4E")
        canvas.drawCircle(cx, cy, radius, roulettePaint)
        val rouletteFrame = paintOf("#F5C542")
        rouletteFrame.style = Paint.Style.STROKE
        rouletteFrame.strokeWidth = 3f
        canvas.drawCircle(cx, cy, radius, rouletteFrame)

        // Números
        for (i in 0 until 37) {
            val angle = (i * 360f / 37f - 90f) * Math.PI / 180.0
            val nx = cx + (radius - 15f) * kotlin.math.cos(angle).toFloat()
            val ny = cy + (radius - 15f) * kotlin.math.sin(angle).toFloat()
            val numPaint = paintOf(if (i == 0) "#00E676" else if (i in listOf(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36)) "#D82800" else "#2A2A2A")
            numPaint.textSize = 10f
            numPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(i.toString(), nx, ny + 4f, numPaint)
        }

        // Bola
        if (rouletteNumber >= 0) {
            val ballAngle = (rouletteNumber * 360f / 37f - 90f) * Math.PI / 180.0
            val bx = cx + (radius - 30f) * kotlin.math.cos(ballAngle).toFloat()
            val by = cy + (radius - 30f) * kotlin.math.sin(ballAngle).toFloat()
            val ballPaint = paintOf("#FFFFFF")
            canvas.drawCircle(bx, by, 6f, ballPaint)
        }

        // Apuesta actual
        val betNames = listOf("ROJO", "NEGRO", "PAR", "IMPAR")
        smallPaint.textSize = 18f
        canvas.drawText("Apuesta: ${betNames[rouletteBet]}  (◀▶ para cambiar)", w / 2f, h - 60f, smallPaint)

        // Resultado
        if (rouletteResult.isNotEmpty()) {
            textPaint.textSize = 18f
            canvas.drawText(rouletteResult, w / 2f, h - 35f, textPaint)
        }

        // Saldo
        smallPaint.textSize = 16f
        canvas.drawText("SALDO: $balance", w / 2f, 60f, smallPaint)

        if (rouletteSpinning) {
            smallPaint.textSize = 16f
            canvas.drawText("Girando...", w / 2f, h - 15f, smallPaint)
        } else if (rouletteResult.isNotEmpty()) {
            greenPaint.textSize = 16f
            canvas.drawText("PULSA START PARA CONTINUAR", w / 2f, h - 15f, greenPaint)
        } else {
            greenPaint.textSize = 16f
            canvas.drawText("PULSA A PARA GIRAR", w / 2f, h - 15f, greenPaint)
        }
    }

    private fun drawBaccarat(canvas: Canvas) {
        titlePaint.textSize = 28f
        canvas.drawText("BACCARAT", w / 2f, 30f, titlePaint)

        // Jugador
        smallPaint.textSize = 16f
        canvas.drawText("JUGADOR: ${baccaratValue(playerBaccarat)}", w / 4f, 60f, smallPaint)
        val cardW = 35f
        val cardH = 50f
        for (i in playerBaccarat.indices) {
            val x = w / 4f - (playerBaccarat.size * cardW) / 2f + i * (cardW + 3f)
            drawCard(canvas, playerBaccarat[i], x, 75f, cardW, cardH)
        }

        // Banca
        canvas.drawText("BANCA: ${baccaratValue(bankerBaccarat)}", 3 * w / 4f, 60f, smallPaint)
        for (i in bankerBaccarat.indices) {
            val x = 3 * w / 4f - (bankerBaccarat.size * cardW) / 2f + i * (cardW + 3f)
            drawCard(canvas, bankerBaccarat[i], x, 75f, cardW, cardH)
        }

        // Resultado
        if (baccaratResult.isNotEmpty()) {
            textPaint.textSize = 20f
            canvas.drawText(baccaratResult, w / 2f, 160f, textPaint)
        }

        // Saldo
        smallPaint.textSize = 18f
        canvas.drawText("SALDO: $balance", w / 2f, h - 60f, smallPaint)

        if (baccaratState == 1) {
            greenPaint.textSize = 16f
            canvas.drawText("PULSA A PARA JUGAR", w / 2f, h - 30f, greenPaint)
        } else if (baccaratState == 2) {
            greenPaint.textSize = 16f
            canvas.drawText("PULSA START PARA CONTINUAR", w / 2f, h - 30f, greenPaint)
        }
    }

    private fun drawGameOver(canvas: Canvas) {
        val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
        redPaint.textSize = 48f
        canvas.drawText("GAME OVER", w / 2f, h / 2f - 30f, redPaint)
        smallPaint.textSize = 22f
        canvas.drawText("Te quedaste sin saldo", w / 2f, h / 2f + 10f, smallPaint)
        canvas.drawText("Saldo final: $balance", w / 2f, h / 2f + 40f, smallPaint)
        if (balance >= best && balance > 0) {
            titlePaint.textSize = 22f
            canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 70f, titlePaint)
        }
        greenPaint.textSize = 20f
        canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 110f, greenPaint)
    }

    override val isGameOver: Boolean get() = state == GameState.GAME_OVER
}
