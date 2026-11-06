package erasom.juanchito.app

object GameLibrary {
    val games: List<Game> = listOf(
        // Clásicos originales con nombres seguros
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

        // 15 Nuevos juegos Arcade de tu lista
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
