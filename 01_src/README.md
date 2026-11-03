# Juanchito Console - Mini consola retro de juegos

## Identificador de la aplicación
- applicationId: `erasom.juanchito.app`
- Si tu app antigua ya estaba registrada en Google Play, reemplaza el
  applicationId en `app/build.gradle` por tu ID antiguo exacto.

## Cómo abrir
1. Abre Android Studio.
2. Selecciona "Open" y busca la carpeta:
   `D:\Antigravity\20@erasomlabs\juanchito.app\01_src`
3. Espera a que Gradle sincronice.
4. Conecta un dispositivo Android o inicia un emulador.
5. Haz clic en Run (▶).

## Interfaz
- Estilo Atari retro/moderno: fondo oscuro, acentos naranja y amarillo.
- Mando virtual personalizable: D-pad, botones A/B, SELECT/START.
- Pantalla en horizontal (landscape).

## Juegos incluidos (funcionales)
1. **Snake** - Arcade clásico, come y crece.
2. **Pong** - Tenis de mesa, jugador vs IA.
3. **Breakout** - Rompe ladrillos con la paleta.

## Juegos planificados (próximamente)
- Chrono Trigger (RPG)
- Top Gear 2 (Carreras)
- Kid the Clown (Plataformas)
- Mortal Kombat (Lucha)
- Capcom Soccer (Deportes)
- Tetris (Puzzle)
- Puzzle Bobble (Puzzle)
- Bomberman 3 (Acción)
- Uniracer (Carreras)
- Choplifter 3 (Acción)
- Contra 3 (Run & Gun)

Cada juego se implementa como una clase que implementa la interfaz `Game`
(`Game.kt`). Para agregar un juego nuevo:
1. Crea una clase en el mismo paquete que implemente `Game`.
2. Agrégala a la lista en `GameLibrary.kt`.

## Personalización del mando
El mando virtual se puede personalizar (tamaño, opacidad, posición) editando
los layouts y los listeners en `MainActivity.kt`. La estructura está pensada
para que cada botón sea un View independiente que se puede mover/redimensionar.

## Notas
- Los juegos usan Canvas 2D nativo (sin librerías externas).
- AdMob se puede agregar cuando tengas el ID: añade la dependencia en
  `app/build.gradle` y el meta-data en `AndroidManifest.xml`.
- La firma de la APK debe usar la misma upload key de Google Play si ya
  publicaste una versión previa.
