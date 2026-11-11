package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Mundo Juanchito: pueblo central estilo Roblox con 6 barrios temáticos
 * y 30 portales (uno por minijuego). Juanchito camina libremente,
 * habla con NPCs y entra a los portales para jugar.
 */
class WorldMapGame : Game {
    override val name = "Mundo Juanchito"
    override val genre = "Mundo"

    /** Notifica el nombre del juego elegido para que MainActivity lo lance. */
    var onPortalEnter: ((String) -> Unit)? = null

    private var w = 0; private var h = 0
    private val worldW = 2400f
    private val worldH = 1400f

    private var px = 1200f
    private var py = 780f
    private var facingRight = true
    private var walkPhase = 0f

    private var camX = 0f
    private var camY = 0f

    private var prevA = false
    private var prevB = false

    /** Estado: 0 = explorando, 1 = diálogo de misión abierto. */
    private var dialogPortal = -1

    // ---- Datos del mundo ----

    data class Portal(
        val gameName: String,
        val x: Float,
        val y: Float,
        val colorHex: String,
        val story: String
    )

    data class Npc(val x: Float, val y: Float, val nome: String, val hatColor: Long, val quote: String)
    data class Decor(val x: Float, val y: Float, val kind: Int, val seed: Float) // 0 árbol, 1 casa, 2 flor

    private val districts = listOf(
        "PLAZA ARCADE" to "#5C94FC",
        "COLINA ESPACIO" to "#8814A8",
        "BARRIO JUNGLA" to "#00A800",
        "NEO CITY" to "#D82800",
        "VILLA DEPORTES" to "#FC9838",
        "FERIA FANTASÍA" to "#F5C542"
    )

    val portals: List<Portal> = buildList {
        val stories = mapOf(
            "Serpiente Retro" to "La serpiente dorada se escapó al jardín del arcade. ¡Guíala hasta las manzanas sin que choque con los muros!",
            "Duelo Raqueta" to "El campeonato de paleta está en pie en la cancha azul. ¡Gana 5 puntos contra la máquina Titán!",
            "Rompeladrillos" to "Los ladrillos encantados bloquean la puerta del arcade. ¡Derríbalos con la bola mágica montada en tu paleta!",
            "Block Drop" to "Los bloques caen del cielo sobre la plaza. ¡Apílalos perfectos y limpia líneas para salvar el pueblo!",
            "Bubble Pop" to "Las burbujas del mago Zarko invaden el techo del carnaval. ¡Revienta el enjambre lanzando burbujas gemelas!",
            "Pixel Invaders" to "Una flota alienígena sobrevuela Juanchito Town. ¡Sube a tu nave y defiende el pueblo oleada tras oleada!",
            "Pixel Ritmo" to "El concierto de las notas doradas empieza esta noche. ¡Atrápalas al ritmo en la línea de meta!",
            "Reflejos" to "El semáforo espacial del barrio parpadea sin aviso. ¡Presiona A justo cuando diga GO y demuestra tus reflejos!",
            "Fusion Pixel" to "Los orbes numéricos duermen en la zona 4x4. ¡Fusiona los iguales y despierta el orbe legendario!",
            "Pixel Blocks" to "La torre de Neo Town necesita un piso nuevo. ¡Detén el bloque giratorio justo en el centro de la base!",
            "Jungla Perdida" to "Juanchito perdió el ídolo dorado en la selva profunda. ¡Sáltalo entre lianas, pozos y cocodrilos!",
            "Comando Supremo" to "Furia Roja invadió Neo City esta madrugada. ¡Recorre la autopista en llamas y destruye el núcleo alienígena!",
            "Helirescate" to "Los vecinos quedaron atrapados en el desierto de guerra. ¡Pilota el helicóptero, esquiva tanques y jets, y rescatados a todos!",
            "Pixel Runner" to "Los arbustos espinosos con ojos tomaron el bosque. ¡Corre y salta sin detenerte un solo segundo!",
            "Pixel Minas" to "El campo minado del desierto sella la entrada a la mina de oro. ¡Pisa solo celdas seguras, sapper Juanchito!",
            "Pixel Kombat" to "El torneo milenario del Templo Rojo abre sus puertas. ¡Vence a TITAN en 3 rondas de combate marcial!",
            "Pixel Draw" to "El gran muro del arte está en blanco. ¡Pinta tu obra maestra pixel para la galería del pueblo!",
            "Pixel Puzzles" to "El albañil olvidó el orden de los bloques del taller. ¡Elige el bloque correcto de la secuencia sin fallar!",
            "Pixel Charades" to "La adivinanza de colores confunde a todo el vecindario. ¡Elige el color que piden y gana el premio gordo!",
            "Pixel Tictactoe" to "La IA del parque desafía a tres en raya al aire libre. ¡Vence en el tablero gigante del distrito!",
            "Fútbol Retro Arena" to "¡La Copa Retro 1994 arranca hoy en el estadio del pueblo! ¡Mete más goles que el rival antes del pitazo final!",
            "Turbo Total" to "El Gran Premio de Juanchito Town corre esta tarde. ¡Adelanta rivales con nitro y gana las 3 vueltas!",
            "Cycle Dash" to "El monociclo turbo espera en la rampa del barrio. ¡Cruza el circuito completo esquivando todos los obstáculos!",
            "Carreras" to "La autopista del pueblo está llena de tráfico borracho. ¡Esquiva coches y suma kilómetros sin chocar!",
            "Pixel Smash" to "La sala de demoledores abre sus puertas. ¡Rompe todos los ladrillos de colores con la bola furia!",
            "Cazabombas" to "El sótano del castillo está infestado de blobs patrones. ¡Detona bombas estratégicas y encuentra la puerta secreta!",
            "Crónica del Tiempo" to "El Devorador despierta bajo el Reino de Valdoria. ¡Acompaña a Ciro y Mara por el tiempo y detén el fin del mundo!",
            "Payasito" to "El mago Zarko secuestró a la princesa Miel en su globo-bomba. ¡Pilota Payasito y rescátala del cielo del circo!",
            "Pixel Dados" to "El casino de la feria reta a los pueblerinos con dados cargados. ¡Apuesta a mayor o menor y racha la fortuna!",
            "Solitario" to "Las cartas de la mesa de casino se volvieron locas. ¡Empareja los palos gemelos y limpia la mesa!"
        )
        // (nombre, xs, y, color): si y=0 la fila es vertical con ys fijos
        val rows = listOf(
            "PLAZA ARCADE" to Row(listOf(750f, 900f, 1050f, 1200f, 1350f), 300f, "#5C94FC",
                listOf("Serpiente Retro", "Duelo Raqueta", "Rompeladrillos", "Block Drop", "Bubble Pop")),
            "COLINA ESPACIO" to Row(listOf(1750f, 1860f, 1970f, 2080f, 2190f), 300f, "#8814A8",
                listOf("Pixel Invaders", "Pixel Ritmo", "Reflejos", "Fusion Pixel", "Pixel Blocks")),
            "BARRIO JUNGLA" to Row(listOf(200f, 200f, 200f, 200f, 200f), 0f, "#00A800",
                listOf("Jungla Perdida", "Comando Supremo", "Helirescate", "Pixel Runner", "Pixel Minas")),
            "NEO CITY" to Row(listOf(2200f, 2200f, 2200f, 2200f, 2200f), 0f, "#D82800",
                listOf("Pixel Kombat", "Pixel Draw", "Pixel Puzzles", "Pixel Charades", "Pixel Tictactoe")),
            "VILLA DEPORTES" to Row(listOf(750f, 900f, 1050f, 1200f, 1350f), 1100f, "#FC9838",
                listOf("Fútbol Retro Arena", "Turbo Total", "Cycle Dash", "Carreras", "Pixel Smash")),
            "FERIA FANTASÍA" to Row(listOf(1750f, 1860f, 1970f, 2080f, 2190f), 1100f, "#F5C542",
                listOf("Cazabombas", "Crónica del Tiempo", "Payasito", "Pixel Dados", "Solitario"))
        )
        for ((distName, row) in rows) {
            val ys = if (row.y > 0f) listOf(row.y, row.y, row.y, row.y, row.y)
                     else listOf(400f, 560f, 720f, 880f, 1040f)
            row.games.forEachIndexed { i, gameName ->
                add(Portal(gameName, row.xs[i], ys[i], row.color, stories[gameName] ?: "¡Aventura te espera!"))
            }
        }
    }

    private data class Row(val xs: List<Float>, val y: Float, val color: String, val games: List<String>)

    private val npcs = listOf(
        Npc(1120f, 640f, "ABUELA CHELA", 0x8814A8, "¡Juanchito! Cada portal brillante es una aventura. Acércate y pulsa A para escuchar la misión."),
        Npc(1290f, 760f, "DON COCO", 0x00A800, "Los 30 portales reparten monedas. ¡Juega y sube en el ránking arcade del pueblo!"),
        Npc(1060f, 820f, "NIN-ROBOT", 0x5C94FC, "BIP BOP. Detectados 30 portales. Diversión máxima garantizada. BIP.")
    )

    private val decors: List<Decor> = buildList {
        // Árboles y flores deterministas, lejos de portales/roads/plaza
        val roadHX = { y: Float -> abs(y - 700f) < 110f }
        val roadVX = { x: Float -> abs(x - 1200f) < 110f }
        var i = 0
        var placed = 0
        while (placed < 40 && i < 400) {
            val x = ((i * 397) % 2200) + 100f
            val y = ((i * 571) % 1200) + 100f
            i++
            val nearPortal = portals.any { val dx = it.x - x; val dy = it.y - y; sqrt(dx * dx + dy * dy) < 130f }
            if (nearPortal || roadHX(y) || roadVX(x) || (abs(x - 1200f) < 260f && abs(y - 700f) < 220f)) continue
            val kind = if ((i * 7) % 3 == 0) 2 else 0
            add(Decor(x, y, kind, (i * 31) % 100 / 100f))
            placed++
        }
        // Casas decorativas junto a los distritos
        val houses = listOf(
            620f to 200f, 1500f to 200f,   // norte
            1500f to 1180f, 620f to 1180f, // sur
            150f to 300f, 150f to 1140f,   // oeste
            2280f to 300f, 2280f to 1140f  // este
        )
        houses.forEachIndexed { idx, (hx, hy) -> add(Decor(hx, hy, 1, idx / houses.size.toFloat())) }
    }

    // Paints
    private val textPaint = Paint().apply {
        color = Color.WHITE; textSize = 26f; textAlign = Paint.Align.CENTER; isAntiAlias = false
    }
    private val smallPaint = Paint().apply {
        color = Color.WHITE; textSize = 16f; textAlign = Paint.Align.CENTER; isAntiAlias = false
    }

    override fun init(width: Int, height: Int) { w = width; h = height; clampCamera() }

    override fun reset() {
        // Se conserva la posición del jugador para volver de una partida al mundo donde estaba.
        dialogPortal = -1
        prevA = false; prevB = false
    }

    private fun clampCamera() {
        camX = (px - w / 2f).coerceIn(0f, (worldW - w).coerceAtLeast(0f))
        camY = (py - h / 2f).coerceIn(0f, (worldH - h).coerceAtLeast(0f))
    }

    override fun update(dt: Float, input: GameInput) {
        val aPressed = input.a && !prevA
        val bPressed = input.b && !prevB
        prevA = input.a
        prevB = input.b

        if (dialogPortal >= 0) {
            // Diálogo abierto: A entra al juego (tras mostrarse), B cierra
            if (aPressed) {
                val target = portals[dialogPortal]
                dialogPortal = -1
                onPortalEnter?.invoke(target.gameName)
            } else if (bPressed) {
                dialogPortal = -1
            }
            return
        }

        // Movimiento libre
        val speed = 260f * dt
        var moved = false
        if (input.left) { px -= speed; facingRight = false; moved = true }
        if (input.right) { px += speed; facingRight = true; moved = true }
        if (input.up) { py -= speed; moved = true }
        if (input.down) { py += speed; moved = true }
        if (moved) walkPhase += dt * 10f
        px = px.coerceIn(40f, worldW - 40f)
        py = py.coerceIn(80f, worldH - 40f)
        clampCamera()

        // Interacción con portales
        if (aPressed) {
            val idx = nearestPortalWithin(70f)
            if (idx >= 0) dialogPortal = idx
        }
    }

    private fun nearestPortalWithin(radius: Float): Int {
        var best = -1
        var bestD = radius * radius
        portals.forEachIndexed { i, p ->
            val dx = p.x - px; val dy = p.y - py
            val d = dx * dx + dy * dy
            if (d < bestD) { bestD = d; best = i }
        }
        return best
    }

    private fun nearestNpc(): Npc? {
        return npcs.firstOrNull { n ->
            val dx = n.x - px; val dy = n.y - py
            sqrt(dx * dx + dy * dy) < 70f
        }
    }

    override fun draw(canvas: Canvas) {
        // Cielo/celeste base
        canvas.drawColor(Color.parseColor("#79C050"))

        canvas.save()
        canvas.translate(-camX, -camY)

        drawTiles(canvas)
        drawDecors(canvas)
        drawDistrictBanners(canvas)
        drawPortals(canvas)
        drawFountain(canvas)
        drawNpcs(canvas)
        drawPlayer(canvas)

        canvas.restore()

        drawHud(canvas)
    }

    // ---------- Capas ----------

    private fun drawTiles(canvas: Canvas) {
        val grass1 = Paint().apply { color = Color.parseColor("#5CA040"); isAntiAlias = false }
        val grass2 = Paint().apply { color = Color.parseColor("#68AC48"); isAntiAlias = false }
        val path = Paint().apply { color = Color.parseColor("#C8A060"); isAntiAlias = false }
        val pathEdge = Paint().apply { color = Color.parseColor("#A88040"); isAntiAlias = false }
        val plaza1 = Paint().apply { color = Color.parseColor("#E8E0C8"); isAntiAlias = false }
        val plaza2 = Paint().apply { color = Color.parseColor("#D0C0A0"); isAntiAlias = false }
        val water = Paint().apply { color = Color.parseColor("#4890E8"); isAntiAlias = false }
        val waterHi = Paint().apply { color = Color.parseColor("#78B8F8"); isAntiAlias = false }

        val tile = 48f
        val startX = (camX / tile).toInt() * 48f
        val startY = (camY / tile).toInt() * 48f
        var tx = startX
        while (tx < camX + w && tx < worldW) {
            var ty = startY
            while (ty < camY + h && ty < worldH) {
                val checker = ((tx / tile).toInt() + (ty / tile).toInt()) % 2 == 0
                canvas.drawRect(tx, ty, tx + tile, ty + tile, if (checker) grass1 else grass2)
                // Carretera horizontal central
                if (ty in 656f..744f) {
                    canvas.drawRect(tx, ty, tx + tile, ty + tile, path)
                    if (ty < 664f || ty > 736f) canvas.drawRect(tx, ty, tx + tile, ty + 8f, pathEdge)
                }
                // Carretera vertical central
                if (tx in 1152f..1248f) {
                    canvas.drawRect(tx, ty, tx + tile, ty + tile, path)
                    if (tx < 1160f || tx > 1240f) canvas.drawRect(tx, ty, tx + 8f, ty + tile, pathEdge)
                }
                // Plaza empedrada alrededor de la fuente
                if (abs(tx + tile / 2 - 1200f) < 240f && abs(ty + tile / 2 - 700f) < 200f) {
                    canvas.drawRect(tx, ty, tx + tile, ty + tile, if (checker) plaza1 else plaza2)
                }
                ty += tile
            }
            tx += tile
        }
        // Estanque (suroeste de la plaza)
        val pond = RectF(380f, 380f, 660f, 600f)
        canvas.drawRoundRect(pond, 60f, 60f, water)
        var ripple = 400f
        while (ripple < 640f) {
            canvas.drawLine(ripple, 420f + ((ripple * 13) % 140), ripple + 24f, 420f + ((ripple * 13) % 140), waterHi)
            ripple += 46f
        }
    }

    private fun drawDecors(canvas: Canvas) {
        for (d in decors) {
            when (d.kind) {
                0 -> { // Árbol
                    val trunk = Paint().apply { color = Color.parseColor("#8B4513"); isAntiAlias = false }
                    val crown = Paint().apply { color = if (d.seed < 0.5f) "#2E8B2E".hex() else "#3AA03A".hex(); isAntiAlias = false }
                    canvas.drawRect(d.x - 5f, d.y - 10f, d.x + 5f, d.y + 12f, trunk)
                    canvas.drawCircle(d.x, d.y - 22f, 22f, crown)
                    canvas.drawCircle(d.x - 12f, d.y - 12f, 14f, crown)
                    canvas.drawCircle(d.x + 12f, d.y - 12f, 14f, crown)
                }
                1 -> { // Casa
                    val wall = Paint().apply { color = Color.parseColor("#F0E0C0"); isAntiAlias = false }
                    val roof = Paint().apply { color = Color.parseColor("#C41E24"); isAntiAlias = false }
                    val door = Paint().apply { color = Color.parseColor("#8B4513"); isAntiAlias = false }
                    canvas.drawRect(d.x - 34f, d.y - 10f, d.x + 34f, d.y + 30f, wall)
                    canvas.drawRect(d.x - 42f, d.y - 34f, d.x + 42f, d.y - 8f, roof)
                    canvas.drawRect(d.x - 8f, d.y + 6f, d.x + 8f, d.y + 30f, door)
                    val win = Paint().apply { color = Color.parseColor("#78B8F8"); isAntiAlias = false }
                    canvas.drawRect(d.x + 14f, d.y, d.x + 28f, d.y + 14f, win)
                    canvas.drawRect(d.x - 28f, d.y, d.x - 14f, d.y + 14f, win)
                }
                else -> { // Flor
                    val petal = Paint().apply { color = if (d.seed < 0.33f) "#F8D878".hex() else if (d.seed < 0.66f) "#F57878".hex() else "#F8F8F8".hex(); isAntiAlias = false }
                    val center = Paint().apply { color = Color.parseColor("#F5C542"); isAntiAlias = false }
                    canvas.drawCircle(d.x, d.y, 4f, petal)
                    canvas.drawCircle(d.x, d.y, 2f, center)
                }
            }
        }
    }

    private fun drawDistrictBanners(canvas: Canvas) {
        val bannerSpots = listOf(
            Triple(1050f, 235f, 0), Triple(1970f, 235f, 1),
            Triple(200f, 330f, 2), Triple(2200f, 330f, 3),
            Triple(1050f, 1035f, 4), Triple(1970f, 1035f, 5)
        )
        for ((bx, by, idx) in bannerSpots) {
            val (distName, colorHex) = districts[idx]
            val board = Paint().apply { color = Color.parseColor("#5A3A1A"); isAntiAlias = false }
            val plate = Paint().apply { color = colorHex.hex(); isAntiAlias = false }
            val post = Paint().apply { color = Color.parseColor("#8B4513"); isAntiAlias = false }
            canvas.drawRect(bx - 110f, by - 18f, bx + 110f, by + 18f, board)
            canvas.drawRect(bx - 104f, by - 13f, bx + 104f, by + 13f, plate)
            canvas.drawRect(bx - 4f, by + 18f, bx + 4f, by + 40f, post)
            textPaint.color = Color.parseColor("#0A0A0A")
            textPaint.textSize = 20f
            canvas.drawText("★ $distName", bx, by + 7f, textPaint)
        }
    }

    private fun drawPortals(canvas: Canvas) {
        val t = walkPhase
        for (p in portals) {
            val col = p.colorHex.hex()
            val colHi = lighten(col, 70)
            val colDark = darken(col, 70)
            val frame = Paint().apply { color = colDark; isAntiAlias = false }
            val arch = Paint().apply { color = col; isAntiAlias = false }
            val glow = Paint().apply { color = colHi; alpha = if ((t.toInt() % 2) == 0) 90 else 60; isAntiAlias = false }

            // Resplandor en el suelo
            canvas.drawCircle(p.x, p.y + 18f, 34f, glow)
            // Arco del portal
            canvas.drawRoundRect(RectF(p.x - 24f, p.y - 44f, p.x + 24f, p.y + 22f), 22f, 22f, frame)
            canvas.drawRoundRect(RectF(p.x - 17f, p.y - 38f, p.x + 17f, p.y + 18f), 15f, 15f, arch)
            // Destello interior
            val star = Paint().apply { color = Color.WHITE; isAntiAlias = false }
            canvas.drawCircle(p.x, p.y - 8f, 4f + (t % 2f), star)
            // Cartel con el nombre del juego
            val sign = Paint().apply { color = Color.parseColor("#080812"); alpha = 200; isAntiAlias = false }
            canvas.drawRoundRect(RectF(p.x - 52f, p.y - 74f, p.x + 52f, p.y - 50f), 5f, 5f, sign)
            smallPaint.color = colHi
            smallPaint.textSize = 13f
            canvas.drawText(p.gameName, p.x, p.y - 58f, smallPaint)
        }
        // Prompt flotante sobre el portal más cercano
        val near = nearestPortalWithin(70f)
        if (near >= 0 && dialogPortal < 0) {
            val p = portals[near]
            val bob = 4f * kotlin.math.sin(t)
            textPaint.color = Color.parseColor("#F5C542")
            textPaint.textSize = 22f
            canvas.drawText("¡PULSA A!", p.x, p.y - 86f + bob, textPaint)
        }
    }

    private fun drawFountain(canvas: Canvas) {
        val cx = 1200f; val cy = 700f
        val stone = Paint().apply { color = Color.parseColor("#9A9AAE"); isAntiAlias = false }
        val stoneDark = Paint().apply { color = Color.parseColor("#6A6A7E"); isAntiAlias = false }
        val water = Paint().apply { color = Color.parseColor("#4890E8"); isAntiAlias = false }
        val jet = Paint().apply { color = Color.parseColor("#A8D8FC"); isAntiAlias = false }
        canvas.drawCircle(cx, cy + 6f, 62f, stoneDark)
        canvas.drawCircle(cx, cy, 58f, stone)
        canvas.drawCircle(cx, cy, 46f, water)
        // Chorro central animado
        val drop = ((walkPhase * 4) % 3).toInt()
        canvas.drawRect(cx - 6f, cy - 58f - drop * 4f, cx + 6f, cy - 20f, jet)
        canvas.drawCircle(cx, cy - 62f - drop * 4f, 5f, jet)
        // Pedestal con estatua de Juanchito
        canvas.drawRect(cx - 16f, cy - 16f, cx + 16f, cy + 14f, stone)
        JuanchitoSprite.draw(canvas, cx, cy - 40f, 30f, facingRight = true)
    }

    private fun drawNpcs(canvas: Canvas) {
        for (n in npcs) {
            val near = nearestNpc() === n
            JuanchitoSprite.draw(canvas, n.x, n.y, 30f, facingRight = true, hatColor = n.hatColor.toInt())
            smallPaint.color = Color.parseColor("#0A0A0A")
            smallPaint.textSize = 13f
            canvas.drawText(n.nome, n.x, n.y - 34f, smallPaint)
            if (near && dialogPortal < 0) {
                // Burbuja de conversación (el diálogo se muestra abajo automáticamente)
                val bubble = Paint().apply { color = Color.WHITE; alpha = 230; isAntiAlias = false }
                val r = RectF(n.x + 16f, n.y - 66f, n.x + 40f, n.y - 42f)
                canvas.drawRoundRect(r, 6f, 6f, bubble)
                val dots = Paint().apply { color = Color.parseColor("#5A5A5A"); isAntiAlias = false }
                canvas.drawCircle(n.x + 24f, n.y - 58f, 2.5f, dots)
                canvas.drawCircle(n.x + 32f, n.y - 54f, 2.5f, dots)
            }
        }
    }

    private fun drawPlayer(canvas: Canvas) {
        val bob = if (walkPhase > 0f) 2f * kotlin.math.sin(walkPhase) else 0f
        JuanchitoSprite.draw(canvas, px, py + bob, 34f, facingRight = facingRight)
        // Sombra
        val shadow = Paint().apply { color = Color.parseColor("#000000"); alpha = 60; isAntiAlias = false }
        canvas.drawOval(RectF(px - 14f, py + 14f, px + 14f, py + 20f), shadow)
    }

    private fun drawHud(canvas: Canvas) {
        // Barra superior
        val bar = Paint().apply { color = Color.parseColor("#080812"); alpha = 170; isAntiAlias = false }
        canvas.drawRect(0f, 0f, w.toFloat(), 34f, bar)
        textPaint.color = Color.parseColor("#F5C542")
        textPaint.textSize = 20f
        canvas.drawText("🌍 MUNDO JUANCHITO  ·  $distNameAtPlayer", w / 2f, 24f, textPaint)
        // Hint inferior
        if (dialogPortal < 0) {
            smallPaint.color = Color.WHITE
            smallPaint.alpha = 190
            canvas.drawText("D-PAD: caminar   ·   A: interactuar", w / 2f, h - 10f, smallPaint)
            smallPaint.alpha = 255
        }
        // Diálogo de misión
        if (dialogPortal >= 0) {
            val p = portals[dialogPortal]
            NesDialogueRenderer.drawDialogueBox(
                canvas, w, h,
                speaker = "PORTAL: ${p.gameName}",
                text = p.story,
                speakerColor = p.colorHex.hex(),
                progressText = "PULSA [A] PARA ENTRAR  ·  [B] PARA CERRAR"
            )
        } else {
            // Burbuja NPC si está cerca
            val n = nearestNpc()
            if (n != null) {
                NesDialogueRenderer.drawDialogueBox(canvas, w, h, n.nome, n.quote)
            }
        }
    }

    private val distNameAtPlayer: String
        get() {
            var best = "CAMPO ABIERTO"
            var bestD = 360f
            val labels = listOf(
                "PLAZA ARCADE" to Triple(1050f, 300f, 0f), "COLINA ESPACIO" to Triple(1970f, 300f, 0f),
                "BARRIO JUNGLA" to Triple(200f, 720f, 0f), "NEO CITY" to Triple(2200f, 720f, 0f),
                "VILLA DEPORTES" to Triple(1050f, 1100f, 0f), "FERIA FANTASÍA" to Triple(1970f, 1100f, 0f)
            )
            for ((name, spot) in labels) {
                val dx = spot.first - px; val dy = spot.second - py
                val d = sqrt(dx * dx + dy * dy)
                if (d < bestD) { bestD = d; best = name }
            }
            return best
        }

    // Utilidades de color
    private fun String.hex(): Int = Color.parseColor(this)
    private fun lighten(color: Int, amount: Int): Int = Color.argb(
        255,
        (Color.red(color) + amount).coerceAtMost(255),
        (Color.green(color) + amount).coerceAtMost(255),
        (Color.blue(color) + amount).coerceAtMost(255)
    )
    private fun darken(color: Int, amount: Int): Int = Color.argb(
        255,
        (Color.red(color) - amount).coerceAtLeast(0),
        (Color.green(color) - amount).coerceAtLeast(0),
        (Color.blue(color) - amount).coerceAtLeast(0)
    )

    override val isGameOver: Boolean get() = false
    override val score: Int get() = 0
}
