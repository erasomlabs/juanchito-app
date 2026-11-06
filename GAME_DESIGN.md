# PlayZone (juanchito.app) — Diseño de Gameplay

## Estética unificada: Minimalista Moderno
- **Fondo**: `#1A1A2E` (dark navy) plano, sin gradientes.
- **Paleta**: `#00D9FF` (cyan), `#FF6B6B` (coral), `#FECA57` (amarillo), `#48DBFB` (azul claro), `#F5F5F5` (blanco), `#2D3748` (gris).
- **Formas**: rectángulos, círculos, líneas. Sin texturas ni sombras complejas.
- **Tipografía**: sans-serif monospace para HUD (score, vidas).
- **Controles**: por juego (cada juego define sus botones en pantalla).

## Interfaz Game
Todos los juegos implementan `Game` (init/update/draw/reset/isGameOver/score). Controles por juego vía `GameInput` (up/down/left/right/a/b/start/select) mapeados a botones on-screen específicos de cada juego.

---

## 1. Snake (Arcade) — existente, adaptar estética
- **Mecánica**: come comida, crece, evita paredes y sí mismo.
- **Controles**: D-pad (4 direcciones).
- **Score**: +10 por comida.

## 2. Pong (Arcade) — existente
- **Mecánica**: jugador vs IA, rebotes.
- **Controles**: up/down para mover paleta.
- **Score**: puntos por tanto.

## 3. Breakout (Arcade) — existente
- **Mecánica**: romper ladrillos con paleta.
- **Controles**: left/right para mover paleta; auto-lanzamiento con A.
- **Score**: por ladrillo.

## 4. Pitfall (Plataformas) — nuevo
- **Mecánica**: scroll horizontal automático. Saltar obstáculos (troncos, serpientes, escorpiones), recoger tesoros (+), usar lianas para cruzar hoyos. Temporizador 20 min.
- **Controles**: left/right mover, A saltar, B usar liana.
- **Score**: por tesoro recogido; bonus por tiempo restante.
- **Game over**: caer en hoyo, tocar enemigo, tiempo agotado.

## 5. Capcom Soccer (Deportes) — nuevo
- **Mecánica**: fútbol arcade top-down 5v5. Pase (A), tiro (B), cambio de jugador (SELECT). IA para equipo rival. Partido 3 min.
- **Controles**: D-pad mover jugador activo, A pase, B tiro, SELECT cambiar.
- **Score**: goles.

## 6. Uniracer (Carreras) — nuevo
- **Mecánica**: carrera de monociclos en pista 2D side-scroll. Saltar (A) para evitar obstáculos, trucos en el aire (B + dirección) dan boost. 3 vueltas.
- **Controles**: left/right acelerar/frenar, A saltar, B truco.
- **Score**: tiempo + bonus por trucos.

## 7. Tetris (Puzzle) — nuevo
- **Mecánica**: piezas caen, rotar/mover, líneas completas desaparecen. Velocidad aumenta por nivel.
- **Controles**: left/right mover, up rotar, down caída rápida, A hard drop.
- **Score**: por líneas (1/2/3/4 líneas = 100/300/500/800 × nivel).

## 8. Puzzle Bobble (Puzzle) — nuevo
- **Mecánica**: disparar burbujas de colores para formar grupos de 3+ del mismo color. Burbujas caen si quedan sueltas.
- **Controles**: left/right apuntar, A disparar.
- **Score**: por burbujas eliminadas.

## 9. Bomberman 3 (Acción) — nuevo
- **Mecánica**: laberinto, colocar bombas (A) para destruir bloques y enemigos, recoger power-ups. No tocar explosiones ni enemigos.
- **Controles**: D-pad mover, A bomba.
- **Score**: por enemigo y bloque destruido.

## 10. Contra 3 (Run & Gun) — nuevo
- **Mecánica**: side-scroll, disparar (A mantenido), saltar (B), agacharse. Enemigos y jefes. 3 vidas.
- **Controles**: D-pad mover/agacharse, A disparar, B saltar.
- **Score**: por enemigo eliminado.

## 11. Choplifter 3 (Acción) — nuevo
- **Mecánica**: helicóptero, rescatar rehenes, disparar (A), soltar bombas (B). Evitar misiles y tanques.
- **Controles**: D-pad mover, A disparar, B bomba.
- **Score**: por rehén rescatado.

## 12. Chrono Trigger (RPG) — nuevo (simplificado)
- **Mecánica**: batallas por turnos (ATB simplificado). Exploración de mapa, 3 personajes, técnicas dobles. Historia mínima.
- **Controles**: D-pad mover en mapa, A seleccionar/atacar, B cancelar.
- **Score**: XP y nivel.

## 13. Top Gear 2 (Carreras) — nuevo
- **Mecánica**: carrera arcade top-down, 8 coches, nitro (B), curvas. 4 pistas.
- **Controles**: left/right girar, A acelerar, B nitro.
- **Score**: posición final + tiempo.

## 14. Kid the Clown (Plataformas) — nuevo
- **Mecánica**: plataforma side-scroll, saltar (A), lanzar bolas (B) para derrotar enemigos. Recoger items.
- **Controles**: D-pad mover, A saltar, B lanzar.
- **Score**: por enemigo y item.

## 15. Mortal Kombat (Lucha) — nuevo (simplificado)
- **Mecánica**: 1v1, golpes (A puño, B patada), bloqueo (down), especiales con secuencias. 2 rounds.
- **Controles**: D-pad mover/agacharse, A puño, B patada, down bloqueo.
- **Score**: por victoria.

---

## Prioridad de implementación
1. **Pitfall** (plataformas base reutilizable)
2. **Tetris** (puzzle simple, alto engagement)
3. **Capcom Soccer** (deportes, multijugador local futuro)
4. **Uniracer** (carreras)
5. **Bomberman 3** (acción)
6. **Contra 3** (run & gun)
7. Resto.

## Controles por juego
Cada juego define su layout de botones en `MainActivity` (o un `GameControls` por juego). El `GameInput` se mapea según el juego activo.

## Aesthetic checklist
- [ ] Fondo plano `#1A1A2E`.
- [ ] Sin gradientes ni sombras.
- [ ] Formas geométricas (rects, circles, lines).
- [ ] HUD monospace blanco.
- [ ] Game over screen consistente: título + "Pulsa START".