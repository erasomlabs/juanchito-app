package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

/**
 * Mini3D: motor 3D por software (estilo bloques/Roblox) renderizado sobre Canvas.
 *
 * Sistema de coordenadas del mundo:
 *   x = este, y = norte, z = arriba (altura).
 * La cámara mira hacia (sin(yaw), cos(yaw)) con horizonte fijo en el centro de pantalla.
 *
 * Estrategia de render:
 *  1. Cielo (2D) + suelo (2D, debajo del horizonte).
 *  2. Caras 3D recolectadas, filtradas por distancia y backface, ordenadas por
 *     profundidad (painter's algorithm) y dibujadas como polígonos.
 *  3. Billboards (letreros, resplandores) sobre la escena 3D.
 */

class MiniCam(var x: Float, var y: Float, var z: Float, var yaw: Float)

object Mini3D {

    const val FOCAL = 640f
    const val NEAR = 12f

    /** Cara plana de 4 vértices: p = [x0,y0,z0, x1,y1,z1, ... x3,y3,z3], normal exterior, color. */
    class Face(val p: FloatArray, val nx: Float, val ny: Float, val nz: Float, val color: Int)

    // ---------- Constructores ----------

    /** Añade las 5 caras visibles de una caja (sin base) con sombreado por cara. */
    fun box(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        rotZ: Float, color: Int,
        out: MutableList<Face>
    ) {
        val hx = sx / 2f; val hy = sy / 2f; val hz = sz / 2f
        val c = cos(rotZ); val s = sin(rotZ)
        fun rx(px: Float, py: Float) = cx + px * c - py * s
        fun ry(px: Float, py: Float) = cy + px * s + py * c
        fun shade(f: Float) = Color.argb(
            255,
            (Color.red(color) * f).toInt().coerceIn(0, 255),
            (Color.green(color) * f).toInt().coerceIn(0, 255),
            (Color.blue(color) * f).toInt().coerceIn(0, 255)
        )
        val top = shade(1.15f)
        val sideY = color
        val sideX = shade(0.72f)

        // Cara superior (z+)
        addQuad(
            out, top, 0f, 0f, 1f,
            rx(-hx, -hy), ry(-hx, -hy), cz + hz,
            rx(hx, -hy), ry(hx, -hy), cz + hz,
            rx(hx, hy), ry(hx, hy), cz + hz,
            rx(-hx, hy), ry(-hx, hy), cz + hz
        )
        // Cara norte (y+)
        addQuad(
            out, sideY, 0f, 1f, 0f,
            rx(-hx, hy), ry(-hx, hy), cz - hz,
            rx(hx, hy), ry(hx, hy), cz - hz,
            rx(hx, hy), ry(hx, hy), cz + hz,
            rx(-hx, hy), ry(-hx, hy), cz + hz
        )
        // Cara sur (y-)
        addQuad(
            out, shade(0.88f), 0f, -1f, 0f,
            rx(hx, -hy), ry(hx, -hy), cz - hz,
            rx(-hx, -hy), ry(-hx, -hy), cz - hz,
            rx(-hx, -hy), ry(-hx, -hy), cz + hz,
            rx(hx, -hy), ry(hx, -hy), cz + hz
        )
        // Cara este (x+)
        addQuad(
            out, sideX, 1f, 0f, 0f,
            rx(hx, hy), ry(hx, hy), cz - hz,
            rx(hx, -hy), ry(hx, -hy), cz - hz,
            rx(hx, -hy), ry(hx, -hy), cz + hz,
            rx(hx, hy), ry(hx, hy), cz + hz
        )
        // Cara oeste (x-)
        addQuad(
            out, sideX, -1f, 0f, 0f,
            rx(-hx, -hy), ry(-hx, -hy), cz - hz,
            rx(-hx, hy), ry(-hx, hy), cz - hz,
            rx(-hx, hy), ry(-hx, hy), cz + hz,
            rx(-hx, -hy), ry(-hx, -hy), cz + hz
        )
    }

    /** Añade una cara arbitraria de 4 vértices con normal explícita. */
    fun addQuad(
        out: MutableList<Face>, color: Int, nx: Float, ny: Float, nz: Float,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float
    ) {
        out.add(
            Face(
                floatArrayOf(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3),
                nx, ny, nz, color
            )
        )
    }

    // ---------- Render ----------

    /**
     * Dibuja las caras ordenadas por profundidad. maxDist recorta por rendimiento.
     * Las esquinas por detrás de la cámara se pellizcan al plano cercano (hack
     * estándar para quads de suelo que cubren la pantalla).
     */
    fun render(canvas: Canvas, cam: MiniCam, w: Int, h: Int, faces: List<Face>, maxDist: Float = 950f) {
        val cosY = cos(cam.yaw); val sinY = sin(cam.yaw)
        val cx = w / 2f; val cy = h / 2f
        val maxD2 = maxDist * maxDist

        val vis = ArrayList<Vis>(64)
        for (f in faces) {
            // Centro de la cara
            var fcx = 0f; var fcy = 0f; var fcz = 0f
            for (i in 0 until 4) {
                fcx += f.p[i * 3]; fcy += f.p[i * 3 + 1]; fcz += f.p[i * 3 + 2]
            }
            fcx /= 4f; fcy /= 4f; fcz /= 4f
            val ddx = fcx - cam.x; val ddy = fcy - cam.y; val ddz = fcz - cam.z
            val dist2 = ddx * ddx + ddy * ddy + ddz * ddz
            if (dist2 > maxD2) continue
            // Backface: visible solo si la normal apunta hacia la cámara
            if (f.nx * ddx + f.ny * ddy + f.nz * ddz >= 0f) continue

            var any = false
            val sx = FloatArray(4); val sy = FloatArray(4); val fw = FloatArray(4)
            for (i in 0 until 4) {
                val px = f.p[i * 3]; val py = f.p[i * 3 + 1]; val pz = f.p[i * 3 + 2]
                val dx = px - cam.x; val dy = py - cam.y
                var fwd = dx * sinY + dy * cosY
                val rgt = dx * cosY - dy * sinY
                val ff = if (fwd < NEAR) NEAR else fwd
                fw[i] = fwd
                sx[i] = cx + rgt * FOCAL / ff
                sy[i] = cy - (pz - cam.z) * FOCAL / ff
                if (fwd >= NEAR) any = true
            }
            if (!any) continue
            // Fuera de pantalla
            var minSx = sx[0]; var maxSx = sx[0]; var minSy = sy[0]; var maxSy = sy[0]
            for (i in 1 until 4) {
                if (sx[i] < minSx) minSx = sx[i]; if (sx[i] > maxSx) maxSx = sx[i]
                if (sy[i] < minSy) minSy = sy[i]; if (sy[i] > maxSy) maxSy = sy[i]
            }
            if (maxSx < -80f || minSx > w + 80f || maxSy < -80f || minSy > h + 80f) continue

            val depth = (fw[0] + fw[1] + fw[2] + fw[3]) / 4f
            vis.add(
                Vis(
                    depth, f.color,
                    floatArrayOf(sx[0], sy[0], sx[1], sy[1], sx[2], sy[2], sx[3], sy[3])
                )
            )
        }

        // Painter's algorithm: lejanas primero
        vis.sortByDescending { it.depth }
        val paint = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL }
        val path = Path()
        for (v in vis) {
            path.reset()
            path.moveTo(v.poly[0], v.poly[1])
            path.lineTo(v.poly[2], v.poly[3])
            path.lineTo(v.poly[4], v.poly[5])
            path.lineTo(v.poly[6], v.poly[7])
            path.close()
            paint.color = v.color
            canvas.drawPath(path, paint)
        }
    }

    private class Vis(val depth: Float, val color: Int, val poly: FloatArray)

    /** Convierte un punto del mundo a pantalla. Devuelve null si está detrás de la cámara. */
    fun project(
        cam: MiniCam, w: Int, h: Int,
        wx: Float, wy: Float, wz: Float
    ): FloatArray? {
        val dx = wx - cam.x; val dy = wy - cam.y
        val fwd = dx * sin(cam.yaw) + dy * cos(cam.yaw)
        if (fwd < NEAR) return null
        val rgt = dx * cos(cam.yaw) - dy * sin(cam.yaw)
        val scale = FOCAL / fwd
        return floatArrayOf(
            w / 2f + rgt * scale,
            h / 2f - (wz - cam.z) * scale,
            scale
        )
    }

    /** Pinta un polígono convexo del mundo (p. ej. estanque) con relleno sólido.
     *  Las esquinas traseras se pellizcan al plano cercano para no desaparecer. */
    fun drawConvex(canvas: Canvas, cam: MiniCam, w: Int, h: Int, pts: List<Triple<Float, Float, Float>>, color: Int) {
        val cosY = cos(cam.yaw); val sinY = sin(cam.yaw)
        val paint = Paint().apply { isAntiAlias = false; setColor(color); style = Paint.Style.FILL }
        val path = Path()
        var any = false
        for (pt in pts) {
            val dx = pt.first - cam.x; val dy = pt.second - cam.y
            var fwd = dx * sinY + dy * cosY
            val rgt = dx * cosY - dy * sinY
            val ff = if (fwd < NEAR) NEAR else fwd
            if (fwd >= NEAR) any = true
            val sx = w / 2f + rgt * FOCAL / ff
            val sy = h / 2f - (pt.third - cam.z) * FOCAL / ff
            if (!path.isEmpty) path.lineTo(sx, sy) else path.moveTo(sx, sy)
        }
        if (!any) return
        path.close()
        canvas.drawPath(path, paint)
    }

    /** Suelo bajo el horizonte: como no hay pitch, el horizonte es exactamente h/2. */
    fun drawGroundAndSky(canvas: Canvas, w: Int, h: Int, skyColor: Int, groundColor: Int) {
        canvas.drawRect(0f, 0f, w.toFloat(), h / 2f, Paint().apply { color = skyColor; isAntiAlias = false })
        canvas.drawRect(0f, h / 2f, w.toFloat(), h.toFloat(), Paint().apply { color = groundColor; isAntiAlias = false })
    }

    // ---------- Avatar blocky estilo Roblox ----------

    /**
     * Construye un personaje de bloques. facing = ángulo donde (sin,cos) es el
     * frente. Colores: hatColor, shirt, skin, pants, boots.
     * walking anima piernas/brazos con bob vertical.
     */
    fun avatar(
        out: MutableList<Face>,
        px: Float, py: Float, facing: Float,
        hatColor: Int, shirt: Int, skin: Int, pants: Int,
        walking: Boolean, phase: Float, scale: Float = 1f
    ) {
        val rotZ = -facing
        val bob = if (walking) (2.5f * sin(phase)).let { if (it > 0f) it else 0f } else 0f
        val legSwing = if (walking) sin(phase) * 2.5f else 0f
        val sc = scale

        // Piernas
        val legH = 16f * sc
        box(px, py, (legH / 2f + 1f), 7f * sc, 9f * sc, legH, rotZ, pants, out)
        // Torso (camisa)
        box(px, py, legH + 1f + 11f * sc + bob, 17f * sc, 11f * sc, 22f * sc, rotZ, shirt, out)
        // Brazos
        val armY = 0f
        box(
            px + 11f * sc, py + armY + legSwing, legH + 2f + 11f * sc + bob,
            5f * sc, 8f * sc, 20f * sc, rotZ, shirt, out
        )
        box(
            px - 11f * sc, py + armY - legSwing, legH + 2f + 11f * sc + bob,
            5f * sc, 8f * sc, 20f * sc, rotZ, shirt, out
        )
        // Cabeza
        val headZ = legH + 1f + 22f * sc + bob + 8f * sc
        box(px, py, headZ, 14f * sc, 14f * sc, 14f * sc, rotZ, skin, out)
        // Gorra
        box(px, py, headZ + 7f * sc + 2f, 16f * sc, 16f * sc, 5f * sc, rotZ, hatColor, out)
        // Visera de la gorra (hacia el frente)
        val fx = sin(facing); val fy = cos(facing)
        box(px + fx * 11f * sc, py + fy * 11f * sc, headZ + 6f * sc, 9f * sc, 8f * sc, 3f * sc, rotZ, hatColor, out)
    }

    // ---------- Utilidades de color ----------

    fun lighten(color: Int, amount: Int): Int = Color.argb(
        255,
        (Color.red(color) + amount).coerceAtMost(255),
        (Color.green(color) + amount).coerceAtMost(255),
        (Color.blue(color) + amount).coerceAtMost(255)
    )

    fun darken(color: Int, amount: Int): Int = Color.argb(
        255,
        (Color.red(color) - amount).coerceAtLeast(0),
        (Color.green(color) - amount).coerceAtLeast(0),
        (Color.blue(color) - amount).coerceAtLeast(0)
    )

    fun alpha(color: Int, a: Int): Int = Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
}
