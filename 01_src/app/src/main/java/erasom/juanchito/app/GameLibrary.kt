package erasom.juanchito.app

object GameLibrary {
    val games: List<Game> = listOf(
        // Mundo virtual (hub estilo Roblox con los portales)
        WorldMapGame(),
        // Juegos basados en JARs de assets/jar-games
        SnakeGame(),               // Snake II (Nokia)
        Serpiente3DGame(),         // Snake III (Nokia)
        CazabombasGame(),          // Bomber 2 (Kevin Yank)
        BubblePopGame(),           // Frozen Bubble (Nokia)
        RompeladrillosGame(),      // SmashHit (Twistfuture)
        FutbolRetroArenaGame(),    // Soccer 3D (mob.ua)
        PixelKombatGame(),         // Street Fighter II (Glu)
        PixelRunnerGame(),         // Temple Flee (mob.ua)
        CarrerasOffRoadGame(),     // 4x4 Bigfoot (Falconmobile)
        MotocrossGame(),           // OffRoadDirtMotocross (mob.ua)
        BikeStuntsGame(),          // XBikeStunts (Xendex)
        BouncyBirdGame(),          // Bouncy Bird (Migital)
        CanalControlGame(),        // Canal Control (mob.ua)
        GhostBladeGame(),          // Ghost Blade (Club Nokia)
        BounceGame(),              // Bounce (Nokia)
        GolfGame(),                // Nokia Golf (Nokia)
        CasinoGame(),              // GoPremiumPackTrial (GoodWood)
        // Juegos mejorados con JARs descargados
        FusionPixelGame(),         // 2048 (Baltoro)
        PixelBlocksGame(),         // Tower Bloxx (Digital Chocolate)
        PixelRitmoGame(),          // Music Revolution (Ozura)
        ReflejosGame(),            // Dexter's Lab (Turner)
        PixelPuzzlesGame(),        // Puzzle Quest Warlords (THQ)
        PayasitoGame(),            // Animal Circus (ZED)
        // Juegos creados desde cero
        PixelDrawGame(),           // Picross/Nonogram (paint by numbers)
        PixelCharadesGame(),       // Heads Up! (charadas)
        CronicaDelTiempoGame()     // RPG (Chrono Trigger)
    )
}
