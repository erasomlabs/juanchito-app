package erasom.juanchito.app

import android.graphics.*

object NesDialogueRenderer {

    fun drawDialogueBox(
        canvas: Canvas,
        w: Int,
        h: Int,
        speaker: String,
        text: String,
        speakerColor: Int = Color.parseColor("#E6B800"),
        progressText: String = "PULSA [A] PARA CONTINUAR"
    ) {
        val density = canvas.density.let { if (it > 0) it / 160f else 1f }
        val boxHeight = (h * 0.32f).coerceAtLeast(80f)
        val boxTop = h - boxHeight - 8f
        val boxRect = RectF(12f, boxTop, w - 12f, h - 8f)

        // Fondo y borde doble estilo NES
        val fillPaint = Paint().apply { color = Color.parseColor("#080812"); style = Paint.Style.FILL }
        canvas.drawRect(boxRect, fillPaint)

        val outerBorder = Paint().apply { color = Color.parseColor("#F8F8F8"); style = Paint.Style.STROKE; strokeWidth = 3f }
        canvas.drawRect(boxRect, outerBorder)

        val innerRect = RectF(boxRect.left + 4f, boxRect.top + 4f, boxRect.right - 4f, boxRect.bottom - 4f)
        val innerBorder = Paint().apply { color = speakerColor; style = Paint.Style.STROKE; strokeWidth = 1.5f }
        canvas.drawRect(innerRect, innerBorder)

        // Nombre de personaje
        val speakerPaint = Paint().apply {
            color = speakerColor
            textSize = (boxHeight * 0.17f).coerceIn(12f, 18f)
            isFakeBoldText = true
            isAntiAlias = false
        }
        canvas.drawText("▶ $speaker", boxRect.left + 14f, boxRect.top + speakerPaint.textSize + 8f, speakerPaint)

        // Texto de diálogo con corte de líneas
        val textPaint = Paint().apply {
            color = Color.parseColor("#F8F8F8")
            textSize = (boxHeight * 0.14f).coerceIn(10f, 15f)
            isAntiAlias = false
        }

        val maxWidth = (boxRect.width() - 28f).toInt()
        val words = text.split(" ")
        var line = ""
        var lineY = boxRect.top + speakerPaint.textSize + textPaint.textSize + 14f

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (textPaint.measureText(testLine) < maxWidth) {
                line = testLine
            } else {
                canvas.drawText(line, boxRect.left + 14f, lineY, textPaint)
                line = word
                lineY += textPaint.textSize + 4f
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, boxRect.left + 14f, lineY, textPaint)
        }

        // Flecha parpadeante
        val promptPaint = Paint().apply {
            color = Color.parseColor("#F5C542")
            textSize = (boxHeight * 0.11f).coerceIn(8f, 12f)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = false
        }
        canvas.drawText("▼ $progressText", boxRect.right - 14f, boxRect.bottom - 8f, promptPaint)
    }

    fun drawPrologueScreen(
        canvas: Canvas,
        w: Int,
        h: Int,
        gameTitle: String,
        missionTitle: String,
        prologueLines: List<String>,
        primaryColor: Int = Color.parseColor("#C41E24")
    ) {
        canvas.drawColor(Color.parseColor("#05050A"))

        // Marco exterior
        val framePaint = Paint().apply {
            color = primaryColor
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(8f, 8f, w - 8f, h - 8f, framePaint)

        // Cabecera
        val titlePaint = Paint().apply {
            color = Color.parseColor("#F5C542")
            textSize = (h * 0.08f).coerceIn(14f, 24f)
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = false
        }
        canvas.drawText("★ $gameTitle ★", w / 2f, 32f, titlePaint)

        val missionPaint = Paint().apply {
            color = Color.parseColor("#00E5FF")
            textSize = (h * 0.06f).coerceIn(11f, 16f)
            textAlign = Paint.Align.CENTER
            isAntiAlias = false
        }
        canvas.drawText(missionTitle, w / 2f, 54f, missionPaint)

        val sepPaint = Paint().apply { color = Color.parseColor("#404060"); strokeWidth = 2f }
        canvas.drawLine(24f, 64f, w - 24f, 64f, sepPaint)

        // Texto de lore
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#F0F0F0")
            textSize = (h * 0.052f).coerceIn(10f, 14f)
            textAlign = Paint.Align.LEFT
            isAntiAlias = false
        }

        var textY = 86f
        for (line in prologueLines) {
            canvas.drawText(line, 28f, textY, bodyPaint)
            textY += bodyPaint.textSize + 6f
        }

        // Prompt
        val startPaint = Paint().apply {
            color = Color.parseColor("#E6B800")
            textSize = (h * 0.06f).coerceIn(11f, 16f)
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = false
        }
        canvas.drawText("PULSA [A] O [START] PARA COMENZAR", w / 2f, h - 20f, startPaint)
    }

    fun drawBossHud(
        canvas: Canvas,
        w: Int,
        bossName: String,
        curHp: Int,
        maxHp: Int
    ) {
        val barWidth = 160f
        val startX = w / 2f - barWidth / 2f
        val bgRect = RectF(startX, 10f, startX + barWidth, 24f)

        val bgPaint = Paint().apply { color = Color.parseColor("#200508"); style = Paint.Style.FILL }
        canvas.drawRect(bgRect, bgPaint)

        val fillRatio = (curHp.toFloat() / maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
        val fillRect = RectF(startX, 10f, startX + barWidth * fillRatio, 24f)
        val fillPaint = Paint().apply { color = Color.parseColor("#E63946"); style = Paint.Style.FILL }
        canvas.drawRect(fillRect, fillPaint)

        val strokePaint = Paint().apply { color = Color.parseColor("#F8F8F8"); style = Paint.Style.STROKE; strokeWidth = 2f }
        canvas.drawRect(bgRect, strokePaint)

        val textPaint = Paint().apply {
            color = Color.parseColor("#FFFFFF")
            textSize = 10f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = false
        }
        canvas.drawText("BOSS: $bossName  [$curHp/$maxHp]", w / 2f, 21f, textPaint)
    }
}
