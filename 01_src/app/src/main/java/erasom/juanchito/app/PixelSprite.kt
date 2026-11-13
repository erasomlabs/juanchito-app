package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

object PixelSprite {
    // Draws a small Pixel character centered at (cx,cy) with given size.
    fun draw(canvas: Canvas, cx: Float, cy: Float, size: Float, facingRight: Boolean = true, hatColor: Int = 0xD82800, bodyColor: Int = 0xF8D878) {
        val half = size / 2f
        val bodyPaint = Paint().apply { color = bodyColor; isAntiAlias = false }
        val hatPaint = Paint().apply { color = hatColor; isAntiAlias = false }
        val legPaint = Paint().apply { color = bodyColor; isAntiAlias = false }
        // body
        canvas.drawRect(cx - half * 0.5f, cy - half * 0.2f, cx + half * 0.5f, cy + half * 0.6f, bodyPaint)
        // head
        canvas.drawRect(cx - half * 0.4f, cy - half, cx + half * 0.4f, cy - half * 0.2f, bodyPaint)
        // hat
        canvas.drawRect(cx - half * 0.5f, cy - half * 1.2f, cx + half * 0.5f, cy - half, hatPaint)
        // legs
        canvas.drawRect(cx - half * 0.4f, cy + half * 0.6f, cx - half * 0.1f, cy + half, legPaint)
        canvas.drawRect(cx + half * 0.1f, cy + half * 0.6f, cx + half * 0.4f, cy + half, legPaint)
        // nose
        val nosePaint = Paint().apply { color = hatColor; isAntiAlias = false }
        val nx = if (facingRight) cx + half * 0.35f else cx - half * 0.35f
        canvas.drawCircle(nx, cy - half * 0.5f, half * 0.12f, nosePaint)
    }
}