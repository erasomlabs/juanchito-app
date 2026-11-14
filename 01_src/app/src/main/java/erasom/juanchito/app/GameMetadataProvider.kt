package erasom.juanchito.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable

object GameMetadataProvider {

    data class GameMetadata(
        val synopsis: String,
        val chapterName: String,
        val primaryColor: Int,
        val accentColor: Int,
        val iconType: String
    )

    fun getMetadata(gameName: String): GameMetadata {
        return when (gameName) {
            "Crónica del Tiempo", "RPG Cronica" -> GameMetadata(
                synopsis = "Año 1000 d.C. Ciro viaja a través de los portales del tiempo para derrotar a Devorador y salvar el destino del planeta.",
                chapterName = "Capítulo 1: El Reino de Valdoria y el Portal",
                primaryColor = Color.parseColor("#C41E24"),
                accentColor = Color.parseColor("#E6B800"),
                iconType = "sword"
            )
            "Comando Supremo", "Cyber Commando" -> GameMetadata(
                synopsis = "Año 2636. La horda extraterrestre invade Neo City. Pixel y Tito despliegan armamento pesado de asalto.",
                chapterName = "Misión 1: Frontera Urbana y Núcleo Alienígena",
                primaryColor = Color.parseColor("#00A800"),
                accentColor = Color.parseColor("#D82800"),
                iconType = "gun"
            )
            "Jungla Perdida", "Selva Perdida" -> GameMetadata(
                synopsis = "Pixel el explorador penetra la selva prohibida en busca de los 32 tesoros mayas antes de que anochezca.",
                chapterName = "Expedición I: Pantano de Cocodrilos y Lianas",
                primaryColor = Color.parseColor("#805A20"),
                accentColor = Color.parseColor("#50A000"),
                iconType = "jungle"
            )
            "Cazabombas", "Bomb Labyrinth" -> GameMetadata(
                synopsis = "El ejército androide ha sitiado la base planetaria. Bombita debe colocar explosivos y hallar la llave maestra.",
                chapterName = "Sector 1: Fortaleza de Bloques y Tanque Boss",
                primaryColor = Color.parseColor("#3C44AA"),
                accentColor = Color.parseColor("#E6482E"),
                iconType = "bomb"
            )
            "Turbo Total", "Nitro Speed" -> GameMetadata(
                synopsis = "Campeonato mundial nocturno sobre asfalto mojado. Gestiona el nitro, el desgaste de gomas y adelanta al rival.",
                chapterName = "Circuito 1: Tokio Speedway Nocturno",
                primaryColor = Color.parseColor("#D82800"),
                accentColor = Color.parseColor("#F8D878"),
                iconType = "car"
            )
            "Mortal Kombat", "Pixel Kombat" -> GameMetadata(
                synopsis = "Torneo milenario en la isla de Shang Tsung para defender el reino de la Tierra en combate marcial a muerte.",
                chapterName = "Ronda de Honor: El Templo de los Guerreros",
                primaryColor = Color.parseColor("#8C1414"),
                accentColor = Color.parseColor("#F5C542"),
                iconType = "fist"
            )
            "Fútbol Retro Arena", "Arcade Soccer" -> GameMetadata(
                synopsis = "Copa Internacional Arcade 1990. Fútbol veloz 5v5 con tiros especiales acrobáticos y árbitro sin piedad.",
                chapterName = "Fase de Grupos: Estadio Nacional",
                primaryColor = Color.parseColor("#007800"),
                accentColor = Color.parseColor("#F8F8F8"),
                iconType = "ball"
            )
            "Helirescate", "Rescue Chopper" -> GameMetadata(
                synopsis = "Vuelo en zona de combate hostil. El escuadrón debe rescatar 64 prisioneros de guerra esquivando antiaéreos.",
                chapterName = "Operación Rescate: Sector Desierto",
                primaryColor = Color.parseColor("#5C94FC"),
                accentColor = Color.parseColor("#FC9838"),
                iconType = "chopper"
            )
            "Payasito", "Circus Run" -> GameMetadata(
                synopsis = "El circo mágico ha sido hechizado por el mago Zarko. Kid debe recuperar los globos dorados por las carpas flotantes.",
                chapterName = "Acto 1: La Carpa Estelar",
                primaryColor = Color.parseColor("#C84C0C"),
                accentColor = Color.parseColor("#F8B800"),
                iconType = "circus"
            )
            "Uniracer", "Cycle Dash" -> GameMetadata(
                synopsis = "Monociclos hipersónicos en pistas acrobáticas. Realiza giros mortales en el aire para ganar propulsión nitro.",
                chapterName = "Pista Nitro: Circuito Neón",
                primaryColor = Color.parseColor("#FC7460"),
                accentColor = Color.parseColor("#00E5FF"),
                iconType = "uniracer"
            )
            "Tetris", "Block Drop", "Pixel Blocks" -> GameMetadata(
                synopsis = "Arquitectura de precisión geométrica. Ensambla y despeja líneas continuas para mantener la matriz intacta.",
                chapterName = "Nivel de Entrenamiento: Velocidad A",
                primaryColor = Color.parseColor("#00D9FF"),
                accentColor = Color.parseColor("#FF6B6B"),
                iconType = "tetris"
            )
            "Puzzle Bobble", "Bubble Pop", "Fusion Pixel" -> GameMetadata(
                synopsis = "El canon mágico de orbes cromáticos. Conecta 3 gemas del mismo color antes de que el techo descienda.",
                chapterName = "Mundo 1: Las Cavernas de Burbujas",
                primaryColor = Color.parseColor("#55EFC4"),
                accentColor = Color.parseColor("#A29BFE"),
                iconType = "bubbles"
            )
            "Pixel Invaders" -> GameMetadata(
                synopsis = "Flotas invasoras espaciales descienden sobre la órbita terrestre. Destruye la nave nodriza.",
                chapterName = "Defensa Orbital: Oleada Alfa",
                primaryColor = Color.parseColor("#5C94FC"),
                accentColor = Color.parseColor("#FF3366"),
                iconType = "alien"
            )
            "Serpiente Retro", "Snake!" -> GameMetadata(
                synopsis = "Caza cibernética en rejilla retro. Consume fuentes de energía para crecer sin colisionar.",
                chapterName = "Rejilla 1: Alimentador Cuántico",
                primaryColor = Color.parseColor("#00A800"),
                accentColor = Color.parseColor("#F8D878"),
                iconType = "snake"
            )
            "Duelo Raqueta", "Reflejos" -> GameMetadata(
                synopsis = "El duelo fundacional del videojuego arcade. Velocidad y reflejos puros frente a la IA.",
                chapterName = "Duelo Arcade: Set de Campeonato",
                primaryColor = Color.parseColor("#F8F8F8"),
                accentColor = Color.parseColor("#3C44AA"),
                iconType = "paddle"
            )
            "Rompeladrillos", "Pixel Smash" -> GameMetadata(
                synopsis = "Destruye la barrera de defensa bloque a bloque con la esfera deflectora y potencia angular.",
                chapterName = "Muro de Cuarzo: Fase 1",
                primaryColor = Color.parseColor("#FECA57"),
                accentColor = Color.parseColor("#FF6B6B"),
                iconType = "brick"
            )
            "Mundo Pixel" -> GameMetadata(
                synopsis = "El pueblo virtual de Pixel: 6 barrios temáticos, 26 portales de aventura y una plaza con fuente y vecinos que te dan misiones.",
                chapterName = "Plaza Central: La Fuente de los Portales",
                primaryColor = Color.parseColor("#D4A017"),
                accentColor = Color.parseColor("#5C94FC"),
                iconType = "globe"
            )
            "Serpiente 3D", "Serpiente Retro" -> GameMetadata(
                synopsis = "Serpiente en mundo 3D isométrico. Come manzanas, crece y evita obstáculos.",
                chapterName = "Arena 1: El Jardín de Manzanas",
                primaryColor = Color.parseColor("#00A800"),
                accentColor = Color.parseColor("#F8D878"),
                iconType = "snake"
            )
            "Fusion Pixel" -> GameMetadata(
                synopsis = "Fusiona tiles numéricos en un tablero 4x4. Alcanza el tile 2048.",
                chapterName = "Fase 1: Orbes Numéricos",
                primaryColor = Color.parseColor("#EDC22E"),
                accentColor = Color.parseColor("#776E65"),
                iconType = "puzzle"
            )
            "Bouncy Bird" -> GameMetadata(
                synopsis = "Vuela entre tuberías esquivando obstáculos. Toca para subir.",
                chapterName = "Vuelo 1: El Cielo Infinito",
                primaryColor = Color.parseColor("#5C94FC"),
                accentColor = Color.parseColor("#F8D878"),
                iconType = "bird"
            )
            "Canal Control" -> GameMetadata(
                synopsis = "Conecta tuberías para que el agua fluya del origen al desagüe.",
                chapterName = "Nivel 1: El Primer Canal",
                primaryColor = Color.parseColor("#00BFFF"),
                accentColor = Color.parseColor("#5C94FC"),
                iconType = "pipe"
            )
            "Ghost Blade" -> GameMetadata(
                synopsis = "Shooter espacial. Dispara enemigos, esquiva proyectiles y recoge power-ups.",
                chapterName = "Misión 1: La Flota Fantasma",
                primaryColor = Color.parseColor("#D82800"),
                accentColor = Color.parseColor("#5C94FC"),
                iconType = "gun"
            )
            "Bounce" -> GameMetadata(
                synopsis = "Pelota que rebota por niveles. Recoge anillos y evita obstáculos.",
                chapterName = "Nivel 1: El Primer Rebote",
                primaryColor = Color.parseColor("#F8D878"),
                accentColor = Color.parseColor("#F5C542"),
                iconType = "ball"
            )
            "Golf" -> GameMetadata(
                synopsis = "Golf con swing meter y viento. Emboca en el menor número de golpes.",
                chapterName = "Hoyo 1: El Tee de Salida",
                primaryColor = Color.parseColor("#228B22"),
                accentColor = Color.parseColor("#F5C542"),
                iconType = "golf"
            )
            "Casino" -> GameMetadata(
                synopsis = "Casino completo: Blackjack, Slots, Ruleta y Baccarat.",
                chapterName = "Mesa 1: La Apuesta",
                primaryColor = Color.parseColor("#0A4A1A"),
                accentColor = Color.parseColor("#F5C542"),
                iconType = "casino"
            )
            "Pixel Draw" -> GameMetadata(
                synopsis = "Picross: revela imágenes ocultas marcando celdas. Paint by numbers.",
                chapterName = "Imagen 1: El Primer Dibujo",
                primaryColor = Color.parseColor("#F5C542"),
                accentColor = Color.parseColor("#776E65"),
                iconType = "draw"
            )
            "Pixel Charades" -> GameMetadata(
                synopsis = "Juego de adivinanza de palabras. Pon el celular en la frente y adivina.",
                chapterName = "Ronda 1: Las Primeras Palabras",
                primaryColor = Color.parseColor("#8814A8"),
                accentColor = Color.parseColor("#F5C542"),
                iconType = "charades"
            )
            "Cronica del Tiempo" -> GameMetadata(
                synopsis = "RPG de viaje en el tiempo. Derrota a Lavos y salva el mundo.",
                chapterName = "Capítulo 1: El Reino de Valdoria",
                primaryColor = Color.parseColor("#C41E24"),
                accentColor = Color.parseColor("#E6B800"),
                iconType = "sword"
            )
            "Pixel Blocks" -> GameMetadata(
                synopsis = "Apila pisos para construir la torre más alta. Viento y combos.",
                chapterName = "Edificio 1: La Primera Torre",
                primaryColor = Color.parseColor("#5C94FC"),
                accentColor = Color.parseColor("#F5C542"),
                iconType = "blocks"
            )
            "Pixel Ritmo" -> GameMetadata(
                synopsis = "Ritmo: atrapa notas al ritmo de la música. 5 lanes, combos.",
                chapterName = "Canción 1: El Primer Ritmo",
                primaryColor = Color.parseColor("#8814A8"),
                accentColor = Color.parseColor("#F5C542"),
                iconType = "music"
            )
            "Pixel Puzzles" -> GameMetadata(
                synopsis = "Match-3 RPG: empareja runas y lanza hechizos.",
                chapterName = "Batalla 1: El Primer Enemigo",
                primaryColor = Color.parseColor("#00A800"),
                accentColor = Color.parseColor("#F5C542"),
                iconType = "puzzle"
            )
            "Payasito" -> GameMetadata(
                synopsis = "Apila animales para alcanzar la altura. 5 mini-juegos, 25 niveles.",
                chapterName = "Escena 1: El Primer Circo",
                primaryColor = Color.parseColor("#C84C0C"),
                accentColor = Color.parseColor("#F8B800"),
                iconType = "circus"
            )
            else -> GameMetadata(
                synopsis = "Desafío arcade clásico retro diseñado para probar tus habilidades con controles clásicos.",
                chapterName = "Fase 1: Prueba de Destreza",
                primaryColor = Color.parseColor("#FF6B1A"),
                accentColor = Color.parseColor("#FFD700"),
                iconType = "arcade"
            )
        }
    }

    fun createCartridgeDrawable(primary: Int, accent: Int, radiusDp: Float, density: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(primary)
            setStroke((2 * density).toInt(), accent)
            cornerRadius = radiusDp * density
        }
    }
}
