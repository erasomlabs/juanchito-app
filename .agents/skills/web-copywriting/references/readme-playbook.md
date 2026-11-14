# README Playbook — GitHub como landing

Un README no es documentación completa: es la página que decide si alguien prueba el proyecto. Responde qué y por qué en segundos, y luego enruta.

## Datos que importan

Estudio de 393 repositorios (Prana et al., 2019), citado por el skill `readme-optimization`:

- Qué es el proyecto: 97 % — obligatorio.
- Cómo se usa: 88,5 % — obligatorio.
- Por qué elegirlo frente a alternativas: 25,7 % — escaso y decisivo.
- En qué estado está: 21,4 % — escaso y decisivo.

Invierte donde hay escasez: diferenciación y estado.

## Esqueleto A — App Android pública

```markdown
# Nombre de la app

Una línea: qué hace, para quién y qué la diferencia.

[badge versión] [badge licencia] [badge plataforma] [badge build]

## Qué es

Dos a cuatro frases. Problema, solución, resultado. Sin adjetivos sin prueba.

## Estado

Beta cerrada / Activa / Experimental. Última versión: X.Y.Z (mes año).
Qué funciona hoy y qué falta.

## Capturas

| Pantalla | Qué muestra |
|---|---|
| ... | ... |

## Descarga e instalación

Enlace a Play Store o APK. Requisitos: Android X+, tamaño, permisos.

## Primer uso

1. Abre la app.
2. Elige X.
3. Listo: verás Y.

## Cómo funciona

Arquitectura en 3 a 5 frases. Stack: Kotlin, Gradle, etc.

## Qué no hace

Lista corta y honesta. Cuándo usar otra app.

## Roadmap

- [ ] ...
- [ ] ...

## Licencia y contacto

MIT / propietaria. Correo o issue tracker.

## Español

Resumen de 3 a 5 líneas para lectores hispanohablantes.
```

## Esqueleto B — Repositorio de contenido

```markdown
# Nombre del proyecto

Qué es, para quién y qué contiene este repositorio.

## Qué encontrarás

- `01_scripts/` — guiones y documentos de trabajo.
- `02_assets/` — logos, imágenes y recursos.
- `03_exports/` — entregables listos para revisión.
- `04_workflows/` — automatizaciones y procesos.

## Documentos clave

| Documento | Para qué sirve |
|---|---|
| ... | ... |

## Cómo se usa

Pasos para reproducir o continuar el trabajo.

## Estado

Qué está terminado, qué está en curso y qué está bloqueado.

## Contacto

...
```

## Esqueleto C — Hub / portafolio técnico

```markdown
# Eraso M Labs

Laboratorio de software. Apps Android nativas y experimentos de interfaz.

## Apps

| App | Qué hace | Estado | Enlace |
|---|---|---|---|
| ... | ... | ... | ... |

## Stack

Kotlin · Jetpack · Gradle · ...

## Principios

3 a 5 decisiones de ingeniería, no valores corporativos.

## Contacto
```

## Reglas de oro

1. **Una línea de posicionamiento** que no sirva para otro repo. Si puedes cambiarte el nombre por el del competidor y sigue teniendo sentido, no sirve.
2. **Estado honesto.** "Beta" no es una debilidad; "producción lista" sin evidencia es una mentira comprobable en un clic.
3. **Instalación ejecutable.** Comando exacto, versión de runtime, variable de entorno. Si no lo probaste, dilo y pide la salida al usuario.
4. **Qué no hace.** La frase que más confianza genera y menos espacio ocupa.
5. **Sin tabla de contenido manual.** GitHub la genera.
6. **Sin HTML crudo** que se rompa en el registro de paquetes o en terminal.
7. **Una sola copia renderizada.** `.github/README.md` y `README.md` no deben divergir.
8. **Monorepo:** el README raíz enruta a los README por paquete.

## Badges

Solo señales de evaluación, es decir, algo que un tercero verifica y que no podría estar en verde si fuera falso:

- Versión publicada.
- Estado de CI.
- Cobertura.
- Licencia.
- Plataforma mínima.

No uses badges de intención: "PRs welcome", "hecho con ❤", "mantenido". Máximo 4. Un badge que no responde una pregunta del lector sobra.

## Descripción del repositorio (furniture)

Es el texto que aparece en búsquedas y enlaces compartidos. Debe contener: qué es + para quién. Sin punto final. Sin emojis si el resto del proyecto no los usa. Ejemplo:

`Suite de apps Android nativas: consola retro, agregador social, utilidades y TV.`

Topics: `android`, `kotlin`, `jetpack-compose`, `...` (5 a 10, en minúsculas).

## Auditoría rápida

Puntúa de 0 a 2 cada bloque. Pasa con 14/16 y los cinco bloqueantes en verde.

### Bloqueantes (0/1)

1. Identidad: se entiende qué es y para quién en la primera pantalla.
2. Instalación: el camino documentado se ejecutó sin pasos ocultos.
3. Claims: toda cifra, comando y comportamiento es verificable.
4. Estado: es honesto y coincide con commits e issues.
5. Licencia: existe y es alcanzable.

### Puntuables (0-2)

6. Diferenciación: dice por qué elegirlo y frente a qué.
7. Escaneabilidad: encabezados claros, párrafos cortos, sin muro de texto.
8. Audiencia: el vocabulario coincide con quien decide.
9. Prueba: capturas, demo o datos reales.
10. Acción: un siguiente paso obvio.
11. Estructura: embudo, no orden cronológico del desarrollo.
12. Accesibilidad: alt text, nada esencial solo en imagen.
13. Mantenimiento: fecha, versión o cadencia creíble.
14. Límites: qué no hace y cuándo usar otra cosa.
15. Enlaces: relativos, resueltos desde la raíz.
16. Longitud: 5 a 12 secciones; 7 es la mediana.

## Prueba del lector frío

Da el README a alguien (o a una sesión nueva sin contexto) y pídele en 60 segundos:

1. ¿Qué es?
2. ¿Para quién?
3. ¿Qué problema resuelve?
4. ¿En qué se diferencia?
5. ¿Cuál es el primer comando o enlace?

Cada respuesta incorrecta apunta a un bloque concreto.

## Errores frecuentes

- Reescribir la prosa sin arreglar la instalación.
- Rellenar para parecer serio: arquitectura y roadmap en una utilidad de 200 líneas la hace parecer abandonada.
- Voz de marketing: superlativo sin evidencia.
- Badges de intención.
- Estado inventado.
- Capturas sin alt y instrucciones solo dentro de la imagen.
- README solo en inglés para un público hispanohablante, o solo en español para un repo público global.

## Cuándo usar otro formato

- Especificación o dataset: no hay instalación; elimina esa fila, no la dejes en cero.
- Plantilla: el README enseña a copiar y adaptar, no a instalar.
- Proyecto privado: README interno, sin enlaces públicos ni badges de estado.
