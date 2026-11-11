package erasom.juanchito.app

object GameLibrary {
    val games: List<Game> = listOf(
        // Mundo virtual (hub estilo Roblox con los 30 portales)
        WorldMapGame(),
        // Clásicos (Lote 1)
        SnakeGame(),
        PongGame(),
        BreakoutGame(),
        PitfallGame(),
        TetrisGame(),
        PuzzleBobbleGame(),
        BombermanGame(),
        ContraGame(),
        ChoplifterGame(),
        UniracerGame(),
        TopGearGame(),
        KidClownGame(),
        MortalKombatGame(),
        ChronoTriggerGame(),
        CapcomSoccerGame(),
        // Juegos Pixel (Lote 2-5)
        PixelSmashGame(),
        FusionPixelGame(),
        PixelBlocksGame(),
        PixelRitmoGame(),
        PixelDrawGame(),
        PixelMinasGame(),
        PixelRunnerGame(),
        ReflejosGame(),
        PixelPuzzlesGame(),
        CarrerasGame(),
        SolitarioGame(),
        PixelDadosGame(),
        PixelCharadesGame(),
        PixelTictactoeGame(),
        PixelInvadersGame()
    )
}