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
            "Chrono Trigger", "Chronicles RPG" -> GameMetadata(
                synopsis = "Año 1000 d.C. Crono viaja a través de los portales del tiempo para derrotar a Lavos y salvar el destino del planeta.",
                chapterName = "Capítulo 1: El Reino de Guardia y el Portal",
                primaryColor = Color.parseColor("#C41E24"),
                accentColor = Color.parseColor("#E6B800"),
                iconType = "sword"
            )
            "Contra 3", "Cyber Commando" -> GameMetadata(
                synopsis = "Año 2636. La horda extraterrestre invade Neo City. Bill y Lance despliegan armamento pesado de asalto.",
                chapterName = "Misión 1: Frontera Urbana y Núcleo Alienígena",
                primaryColor = Color.parseColor("#00A800"),
                accentColor = Color.parseColor("#D82800"),
                iconType = "gun"
            )
            "Pitfall", "Jungle Run" -> GameMetadata(
                synopsis = "Harry el aventurero penetra la selva prohibida en busca de los 32 tesoros mayas antes de que anochezca.",
                chapterName = "Expedición I: Pantano de Cocodrilos y Lianas",
                primaryColor = Color.parseColor("#805A20"),
                accentColor = Color.parseColor("#50A000"),
                iconType = "jungle"
            )
            "Bomberman 3", "Bomb Labyrinth" -> GameMetadata(
                synopsis = "El ejército androide ha sitiado la base planetaria. White Bomber debe colocar explosivos y hallar la llave maestra.",
                chapterName = "Sector 1: Fortaleza de Bloques y Tanque Boss",
                primaryColor = Color.parseColor("#3C44AA"),
                accentColor = Color.parseColor("#E6482E"),
                iconType = "bomb"
            )
            "Top Gear 2", "Nitro Speed" -> GameMetadata(
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
            "Capcom Soccer", "Arcade Soccer" -> GameMetadata(
                synopsis = "Copa Internacional Arcade 1990. Fútbol veloz 5v5 con tiros especiales acrobáticos y árbitro sin piedad.",
                chapterName = "Fase de Grupos: Estadio Nacional",
                primaryColor = Color.parseColor("#007800"),
                accentColor = Color.parseColor("#F8F8F8"),
                iconType = "ball"
            )
            "Choplifter 3", "Rescue Chopper" -> GameMetadata(
                synopsis = "Vuelo en zona de combate hostil. El escuadrón debe rescatar 64 prisioneros de guerra esquivando antiaéreos.",
                chapterName = "Operación Rescate: Sector Desierto",
                primaryColor = Color.parseColor("#5C94FC"),
                accentColor = Color.parseColor("#FC9838"),
                iconType = "chopper"
            )
            "Kid the Clown", "Circus Run" -> GameMetadata(
                synopsis = "El circo mágico ha sido hechizado por Blackjack. Kid debe recuperar los globos dorados por las carpas flotantes.",
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
            "Snake", "Snake!" -> GameMetadata(
                synopsis = "Caza cibernética en rejilla retro. Consume fuentes de energía para crecer sin colisionar.",
                chapterName = "Rejilla 1: Alimentador Cuántico",
                primaryColor = Color.parseColor("#00A800"),
                accentColor = Color.parseColor("#F8D878"),
                iconType = "snake"
            )
            "Pong", "Reflejos" -> GameMetadata(
                synopsis = "El duelo fundacional del videojuego arcade. Velocidad y reflejos puros frente a la IA.",
                chapterName = "Duelo Arcade: Set de Campeonato",
                primaryColor = Color.parseColor("#F8F8F8"),
                accentColor = Color.parseColor("#3C44AA"),
                iconType = "paddle"
            )
            "Breakout", "Pixel Smash" -> GameMetadata(
                synopsis = "Destruye la barrera de defensa bloque a bloque con la esfera deflectora y potencia angular.",
                chapterName = "Muro de Cuarzo: Fase 1",
                primaryColor = Color.parseColor("#FECA57"),
                accentColor = Color.parseColor("#FF6B6B"),
                iconType = "brick"
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
