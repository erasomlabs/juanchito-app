package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class ComingSoonGame(override val name: String, override val genre: String) : Game {
    private var w = 0
    private var h = 0
    override var isGameOver = false
    override var score = 0

    private val titlePaint = Paint().apply {
        color = Color.parseColor("#FF6B1A")
        textSize = 64f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val subPaint = Paint().apply {
        color = Color.parseColor("#F5F5F5")
        textSize = 28f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    override fun init(width: Int, height: Int) {
        w = width; h = height
    }

    override fun update(dt: Float, input: GameInput) {
        if (input.start) isGameOver = false
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#0A0A0F"))
        canvas.drawText(name, w / 2f, h / 2f - 40f, titlePaint)
        canvas.drawText("PRÓXIMAMENTE", w / 2f, h / 2f + 20f, subPaint)
        canvas.drawText("Pulsa START para volver", w / 2f, h / 2f + 80f, subPaint.apply { textSize = 20f })
    }

    override fun reset() {
        isGameOver = false
    }
}
