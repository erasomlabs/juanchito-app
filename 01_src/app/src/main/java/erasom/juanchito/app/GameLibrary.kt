package erasom.juanchito.app

object GameLibrary {
    val games: List<Game> = listOf(
        SnakeGame(),
        PongGame(),
        BreakoutGame(),
        ComingSoonGame("Chrono Trigger", "RPG"),
        ComingSoonGame("Top Gear 2", "Carreras"),
        ComingSoonGame("Kid the Clown", "Plataformas"),
        ComingSoonGame("Mortal Kombat", "Lucha"),
        ComingSoonGame("Capcom Soccer", "Deportes"),
        ComingSoonGame("Tetris", "Puzzle"),
        ComingSoonGame("Puzzle Bobble", "Puzzle"),
        ComingSoonGame("Bomberman 3", "Acción"),
        ComingSoonGame("Uniracer", "Carreras"),
        ComingSoonGame("Choplifter 3", "Acción"),
        ComingSoonGame("Contra 3", "Run & Gun")
    )
}
