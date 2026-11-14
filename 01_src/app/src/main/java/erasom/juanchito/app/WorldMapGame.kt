package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.sin

/**
 * Mundo Pixel 3D: pueblo estilo Roblox con 6 barrios temáticos,
 * 26 portales de misión, NPCs y avatar de bloques. Cámara en tercera
 * persona: D-PAD izq/der giran la vista, arriba/abajo caminan.
 */
class WorldMapGame : Game {
    override val name = "Mundo Pixel"
    override val genre = "Mundo"

    /** Notifica el nombre del juego elegido para que MainActivity lo lance. */
    var onPortalEnter: ((String) -> Unit)? = null

    /** START/SELECT abren el menú clásico de cartuchos sobre el mundo. */
    var onOpenMenu: (() -> Unit)? = null

    private var w = 0; private var h = 0
    private val worldW = 2400f
    private val worldH = 1400f

    // Jugador y cámara
    private var px = 1200f
    private var py = 880f
    private var camYaw = 0f
    private var walking = false
    private var phase = 0f
    private val cam = MiniCam(1200f, 740f, 52f, 0f)

    private var prevA = false
    private var prevB = false
    private var prevStart = false
    private var prevSelect = false

    /** Estado: -1 = explorando, >= 0 = diálogo de misión del portal abierto. */
    private var dialogPortal = -1

    // ---- Datos del mundo ----

    data class Portal(
        val gameName: String,
        val x: Float,
        val y: Float,
        val colorHex: String,
        val story: String
    )

    data class Npc(val x: Float, val y: Float, val nome: String, val hatColor: Long, val shirt: Int, val quote: String)

    private val districts = listOf(
        "PLAZA ARCADE" to "#5C94FC",
        "COLINA ESPACIO" to "#8814A8",
        "BARRIO JUNGLA" to "#00A800",
        "NEO CITY" to "#D82800",
        "VILLA DEPORTES" to "#FC9838",
        "FERIA FANTASÍA" to "#F5C542"
    )

    private data class Row(val xs: List<Float>, val y: Float, val color: String, val games: List<String>)

    val portals: List<Portal> = buildList {
        val stories = mapOf(
            "Serpiente Retro" to "La serpiente dorada se escapó al jardín del arcade. ¡Guíala hasta las manzanas sin que choque con los muros!",
            "Serpiente 3D" to "La serpiente evolucionó y ahora vive en un mundo 3D. ¡Recorre la arena isométrica, come manzanas y evita obstáculos!",
            "Fusion Pixel" to "Los orbes numéricos duermen en la zona 4x4. ¡Fusiona los iguales y despierta el orbe legendario del 2048!",
            "Pixel Blocks" to "La torre de Neo Town necesita un piso nuevo. ¡Deja caer el bloque en el momento justo y construye la torre más alta!",
            "Pixel Draw" to "El gran muro del arte está en blanco. ¡Revela la imagen oculta marcando las celdas correctas del Picross!",
            "Bouncy Bird" to "El pájaro Bouncy debe volar entre tuberías. ¡Toca para subir y esquiva los obstáculos!",
            "Canal Control" to "El agua no fluye. ¡Rota las tuberías para crear un camino y que el agua llegue al desagüe!",
            "Ghost Blade" to "Los fantasmas invaden la ciudad. ¡Dispara, esquiva y destruye a todos los enemigos!",
            "Pixel Runner" to "Los arbustos espinosos con ojos tomaron el bosque. ¡Corre y salta sin detenerte un solo segundo!",
            "Reflejos" to "El laboratorio de Dexter está invadido de moléculas. ¡Alinea y zapea en el momento justo!",
            "Bounce" to "La pelota Bounce debe rebotar por los niveles. ¡Recoge anillos y evita los obstáculos!",
            "Golf" to "El campo de golf espera. ¡Calcula el swing, considera el viento y emboca en el menor número de golpes!",
            "Motocross" to "La pista de tierra te espera. ¡Acelera, salta rampas y gana la carrera de motocross!",
            "Carreras Off-Road" to "El Gran Premio off-road arranca hoy. ¡Adelanta rivales en terreno irregular y gana las 3 vueltas!",
            "Pixel Kombat" to "El torneo milenario abre sus puertas. ¡Vence a tu rival en 3 rondas de combate marcial!",
            "Pixel Puzzles" to "El tablero de runas necesita orden. ¡Empareja 3 o más del mismo color y lanza hechizos!",
            "Pixel Charades" to "El juego de adivinanzas del pueblo. ¡Pon el celular en la frente y adivina la palabra!",
            "Pixel Ritmo" to "El concierto de las notas doradas empieza esta noche. ¡Atrápalas al ritmo en la línea de meta!",
            "Futbol Retro Arena" to "¡La Copa Retro arranca hoy en el estadio! ¡Mete más goles que el rival antes del pitazo final!",
            "Bike Stunts" to "La rampa del barrio espera. ¡Haz acrobacias en tu bici y gana puntos por cada trick!",
            "Rompeladrillos" to "Los ladrillos encantados bloquean la puerta del arcade. ¡Derríbalos con la bola mágica!",
            "Bubble Pop" to "Las burbujas del mago invaden el techo del carnaval. ¡Revienta el enjambre lanzando burbujas gemelas!",
            "Cazabombas" to "El sótano del castillo está infestado de blobs. ¡Detona bombas estratégicas y encuentra la puerta secreta!",
            "Payasito" to "El mago Zarko secuestró a la princesa Miel. ¡Pilota Payasito y rescátala del cielo del circo!",
            "Casino" to "El casino de la feria reta a los pueblerinos. ¡Apuesta a blackjack, slots, ruleta y baccarat!",
            "Cronica del Tiempo" to "El Devorador despierta bajo el Reino de Valdoria. ¡Acompaña a Ciro y Mara por el tiempo y detén el fin del mundo!"
        )
        val rows = listOf(
            "PLAZA ARCADE" to Row(listOf(750f, 900f, 1050f, 1200f, 1350f), 300f, "#5C94FC",
                listOf("Serpiente Retro", "Serpiente 3D", "Fusion Pixel", "Pixel Blocks", "Pixel Draw")),
            "COLINA ESPACIO" to Row(listOf(1750f, 1860f, 1970f, 2080f, 2190f), 300f, "#8814A8",
                listOf("Bouncy Bird", "Canal Control", "Ghost Blade", "Pixel Runner", "Reflejos")),
            "BARRIO JUNGLA" to Row(listOf(200f, 200f, 200f, 200f), 0f, "#00A800",
                listOf("Bounce", "Golf", "Motocross", "Carreras Off-Road")),
            "NEO CITY" to Row(listOf(2200f, 2200f, 2200f, 2200f), 0f, "#D82800",
                listOf("Pixel Kombat", "Pixel Puzzles", "Pixel Charades", "Pixel Ritmo")),
            "VILLA DEPORTES" to Row(listOf(750f, 900f, 1050f, 1200f), 1100f, "#FC9838",
                listOf("Futbol Retro Arena", "Bike Stunts", "Rompeladrillos", "Bubble Pop")),
            "FERIA FANTASÍA" to Row(listOf(1750f, 1860f, 1970f, 2080f), 1100f, "#F5C542",
                listOf("Cazabombas", "Payasito", "Casino", "Cronica del Tiempo"))
        )
        for ((distName, row) in rows) {
            val ys = if (row.y > 0f) listOf(row.y, row.y, row.y, row.y, row.y)
            else listOf(400f, 560f, 720f, 880f, 1040f)
            row.games.forEachIndexed { i, gameName ->
                add(Portal(gameName, row.xs[i], ys[i], row.color, stories[gameName] ?: "¡Aventura te espera!"))
            }
        }
    }

    private val npcs = listOf(
        Npc(1120f, 640f, "ABUELA CHELA", 0x8814A8, 0x5A7D9A, "¡Pixel! Cada portal brillante es una aventura. Acércate y pulsa A para escuchar la misión."),
        Npc(1290f, 760f, "DON COCO", 0x00A800, 0xFC9838, "Los 26 portales reparten monedas. ¡Juega y sube en el ránking arcade del pueblo!"),
        Npc(1060f, 860f, "NIN-ROBOT", 0x5C94FC, 0x4A4A6A, "BIP BOP. Detectados 26 portales. Diversión máxima garantizada. BIP.")
    )

    // Árboles deterministas lejos de portales/caminos/plaza
    private val treeSpots: List<Pair<Float, Float>> = buildList {
        var i = 0
        var placed = 0
        while (placed < 26 && i < 400) {
            val x = ((i * 397) % 2200) + 100f
            val y = ((i * 571) % 1200) + 100f
            i++
            val nearPortal = portals.any { p ->
                val dx = p.x - x; val dy = p.y - y
                sqrt(dx * dx + dy * dy) < 130f
            }
            if (nearPortal || abs(y - 700f) < 110f || abs(x - 1200f) < 110f ||
                (abs(x - 1200f) < 260f && abs(y - 700f) < 220f)) continue
            add(x to y)
            placed++
        }
    }

    private val houseSpots = listOf(
        620f to 200f, 1500f to 200f,
        1500f to 1180f, 620f to 1180f,
        150f to 300f, 150f to 1140f,
        2280f to 300f, 2280f to 1140f
    )

    // ---------- Geometría estática (construida una sola vez) ----------

    private val staticFaces: List<Mini3D.Face> = buildList {
        // Portales: dos pilares + dintel
        for (p in portals) {
            val col = Color.parseColor(p.colorHex)
            Mini3D.box(p.x - 24f, p.y, 36f, 10f, 10f, 72f, 0f, Mini3D.darken(col, 40), this)
            Mini3D.box(p.x + 24f, p.y, 36f, 10f, 10f, 72f, 0f, Mini3D.darken(col, 40), this)
            Mini3D.box(p.x, p.y, 78f, 66f, 10f, 12f, 0f, Mini3D.darken(col, 60), this)
        }
        // Árboles blocky
        val trunkC = Color.parseColor("#8B5A2B")
        for ((tx, ty) in treeSpots) {
            Mini3D.box(tx, ty, 13f, 12f, 12f, 26f, 0f, trunkC, this)
            Mini3D.box(tx, ty, 38f, 34f, 34f, 30f, 0f, Color.parseColor("#3AA03A"), this)
        }
        // Casas blocky con techo
        for ((hx, hy) in houseSpots) {
            Mini3D.box(hx, hy, 30f, 96f, 86f, 60f, 0f, Color.parseColor("#F0E0C0"), this)
            Mini3D.box(hx, hy, 68f, 108f, 98f, 24f, 0f, Color.parseColor("#C41E24"), this)
        }
        // Fuente: base y agua
        Mini3D.box(1200f, 700f, 8f, 110f, 110f, 16f, 0f, Color.parseColor("#9A9AAE"), this)
        Mini3D.box(1200f, 700f, 20f, 84f, 84f, 12f, 0f, Color.parseColor("#4890E8"), this)
        // Pedestal de la estatua
        Mini3D.box(1200f, 700f, 32f, 34f, 34f, 14f, 0f, Color.parseColor("#7A7A8E"), this)
    }

    // Paints para HUD y billboards
    private val textPaint = Paint().apply {
        color = Color.WHITE; textSize = 26f; textAlign = Paint.Align.CENTER; isAntiAlias = false
    }
    private val smallPaint = Paint().apply {
        color = Color.WHITE; textSize = 16f; textAlign = Paint.Align.CENTER; isAntiAlias = false
    }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        updateCamera()
    }

    override fun reset() {
        dialogPortal = -1
        prevA = false; prevB = false
        prevStart = false; prevSelect = false
    }

    private fun updateCamera() {
        cam.x = px - sin(camYaw) * 130f
        cam.y = py - cos(camYaw) * 130f
        cam.z = 52f
        cam.yaw = camYaw
    }

    override fun update(dt: Float, input: GameInput) {
        val aPressed = input.a && !prevA
        val bPressed = input.b && !prevB
        prevA = input.a
        prevB = input.b
        val startPressed = input.start && !prevStart
        val selectPressed = input.select && !prevSelect
        prevStart = input.start
        prevSelect = input.select

        // START o SELECT alternan el menú clásico de cartuchos
        if (startPressed || selectPressed) {
            onOpenMenu?.invoke()
            return
        }

        if (dialogPortal >= 0) {
            if (aPressed) {
                val target = portals[dialogPortal]
                dialogPortal = -1
                onPortalEnter?.invoke(target.gameName)
            } else if (bPressed) {
                dialogPortal = -1
            }
            return
        }

        // Control estilo Roblox: izq/der giran cámara, arriba/abajo caminan
        walking = false
        val rot = 2.6f * dt
        if (input.left) camYaw -= rot
        if (input.right) camYaw += rot
        val speed = 300f * dt
        if (input.up) {
            px += sin(camYaw) * speed
            py += cos(camYaw) * speed
            walking = true
        }
        if (input.down) {
            px -= sin(camYaw) * speed
            py -= cos(camYaw) * speed
            walking = true
        }
        if (walking) phase += dt * 9f
        px = px.coerceIn(50f, worldW - 50f)
        py = py.coerceIn(90f, worldH - 50f)
        updateCamera()

        if (aPressed) {
            val idx = nearestPortalWithin(75f)
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
            sqrt(dx * dx + dy * dy) < 80f
        }
    }

    // ---------- Geometría de dibujo ----------

    override fun draw(canvas: Canvas) {
        // Cielo y suelo
        Mini3D.drawGroundAndSky(canvas, w, h,
            skyColor = Color.parseColor("#79C8F8"),
            groundColor = Color.parseColor("#5CA040"))

        // Caminos, plaza
        val roadFaces = ArrayList<Mini3D.Face>(2)
        val pathC = Color.parseColor("#C8A060")
        Mini3D.addQuad(roadFaces, pathC, 0f, 0f, 1f,
            0f, 650f, 0.5f, worldW, 650f, 0.5f, worldW, 750f, 0.5f, 0f, 750f, 0.5f)
        Mini3D.addQuad(roadFaces, pathC, 0f, 0f, 1f,
            1150f, 0f, 0.5f, 1250f, 0f, 0.5f, 1250f, worldH, 0.5f, 1150f, worldH, 0.5f)
        Mini3D.render(canvas, cam, w, h, roadFaces)
        val plazaFaces = ArrayList<Mini3D.Face>(1)
        Mini3D.addQuad(plazaFaces, Color.parseColor("#E0D8C0"), 0f, 0f, 1f,
            950f, 490f, 0.6f, 1450f, 490f, 0.6f, 1450f, 910f, 0.6f, 950f, 910f, 0.6f)
        Mini3D.render(canvas, cam, w, h, plazaFaces)
        // Estanque
        val pondPts = ArrayList<Triple<Float, Float, Float>>(8)
        for (i in 0 until 8) {
            val ang = Math.PI * 2 * i / 8
            pondPts.add(Triple(520f + 140f * kotlin.math.cos(ang).toFloat(),
                470f + 110f * kotlin.math.sin(ang).toFloat(), 0.4f))
        }
        Mini3D.drawConvex(canvas, cam, w, h, pondPts, Color.parseColor("#4890E8"))

        // Escena 3D: estáticos + avatares
        val frame = ArrayList<Mini3D.Face>(staticFaces)
        Mini3D.avatar(frame, 1200f, 700f, 0f,
            0xD82800, 0x5C94FC, 0xF8D878, 0x2A2A5A, walking = false, phase = 0f, scale = 0.9f)
        for (n in npcs) {
            val a = atan2((px - n.x).toDouble(), (py - n.y).toDouble()).toFloat()
            Mini3D.avatar(frame, n.x, n.y, a,
                n.hatColor.toInt(), n.shirt, 0xF8D878, 0x2A2A5A, walking = false, phase = 0f)
        }
        Mini3D.avatar(frame, px, py, camYaw,
            0xD82800, 0x5C94FC, 0xF8D878, 0x2A2A5A, walking, phase)

        Mini3D.render(canvas, cam, w, h, frame)

        // Billboards
        drawBillboards(canvas)

        drawHud(canvas)
    }

    private fun drawBillboards(canvas: Canvas) {
        val bannerSpots = listOf(
            Triple(1050f, 235f, 0), Triple(1970f, 235f, 1),
            Triple(200f, 330f, 2), Triple(2200f, 330f, 3),
            Triple(1050f, 1035f, 4), Triple(1970f, 1035f, 5)
        )
        for ((bx, by, idx) in bannerSpots) {
            val (distName, colorHex) = districts[idx]
            drawSign(canvas, bx, by, 96f, "★ $distName", Color.parseColor(colorHex), textSizeBase = 17f)
        }
        for (p in portals) {
            val col = Color.parseColor(p.colorHex)
            val s = Mini3D.project(cam, w, h, p.x, p.y, 44f) ?: continue
            val scale = s[2]
            val pulse = 0.7f + 0.3f * sin(phase * 0.8f)
            val glowW = 30f * scale
            val glowH = 52f * scale
            val glowPaint = Paint().apply {
                color = Mini3D.alpha(col, (110 * pulse).toInt()); isAntiAlias = false
            }
            canvas.drawRoundRect(
                RectF(s[0] - glowW, s[1] - glowH, s[0] + glowW, s[1] + glowH),
                glowW * 0.5f, glowW * 0.5f, glowPaint
            )
            drawSign(canvas, p.x, p.y, 100f, p.gameName, Mini3D.lighten(col, 60), textSizeBase = 13f)
        }
        val near = nearestPortalWithin(75f)
        if (near >= 0 && dialogPortal < 0) {
            val p = portals[near]
            val bob = 5f * sin(phase * 2f)
            drawSign(canvas, p.x, p.y, 138f + bob, "¡PULSA A!", Color.parseColor("#F5C542"), textSizeBase = 15f)
        }
        for (n in npcs) {
            drawSign(canvas, n.x, n.y, 78f, n.nome, Color.parseColor("#F8F8F8"), textSizeBase = 11f)
        }
    }

    private fun drawSign(canvas: Canvas, wx: Float, wy: Float, wz: Float, text: String, color: Int, textSizeBase: Float) {
        val s = Mini3D.project(cam, w, h, wx, wy, wz) ?: return
        val scale = s[2]
        val ts = (textSizeBase * scale * 0.35f).coerceIn(8f, 30f)
        val p = Paint().apply {
            setColor(color); textSize = ts; textAlign = Paint.Align.CENTER; isAntiAlias = false; isFakeBoldText = true
        }
        val tw = p.measureText(text)
        val pad = (4f * scale).coerceIn(3f, 14f)
        val bg = Paint().apply { setColor(Mini3D.alpha(Color.parseColor("#080812"), 205)); isAntiAlias = false }
        canvas.drawRoundRect(
            RectF(s[0] - tw / 2f - pad, s[1] - ts, s[0] + tw / 2f + pad, s[1] + pad),
            pad, pad, bg
        )
        canvas.drawText(text, s[0], s[1], p)
    }

    private fun drawHud(canvas: Canvas) {
        val bar = Paint().apply { color = Color.parseColor("#080812"); alpha = 170; isAntiAlias = false }
        canvas.drawRect(0f, 0f, w.toFloat(), 34f, bar)
        textPaint.color = Color.parseColor("#F5C542")
        textPaint.textSize = 20f
        canvas.drawText("🌍 MUNDO PIXEL  ·  $distNameAtPlayer", w / 2f, 24f, textPaint)
        if (dialogPortal < 0) {
            smallPaint.color = Color.WHITE
            smallPaint.alpha = 190
            canvas.drawText("◀ ▶ girar   ·   ▲ ▼ caminar   ·   A: interactuar   ·   START: MENÚ", w / 2f, h - 10f, smallPaint)
            smallPaint.alpha = 255
        }
        if (dialogPortal >= 0) {
            val p = portals[dialogPortal]
            val ch = WeeklyChallenges.current(p.gameName)
            NesDialogueRenderer.drawDialogueBox(
                canvas, w, h,
                speaker = "PORTAL: ${p.gameName}",
                text = p.story + "\n🏆 RETO SEMANAL: supera ${ch.target} pts y ganas ${ch.reward} 🪙",
                speakerColor = Color.parseColor(p.colorHex),
                progressText = "PULSA [A] PARA ENTRAR  ·  [B] PARA CERRAR"
            )
        } else {
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

    override val isGameOver: Boolean get() = false
    override val score: Int get() = 0
}
