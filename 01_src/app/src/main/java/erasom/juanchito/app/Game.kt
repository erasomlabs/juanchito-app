package erasom.juanchito.app

import android.graphics.Canvas

data class GameInput(
    var up: Boolean = false,
    var down: Boolean = false,
    var left: Boolean = false,
    var right: Boolean = false,
    var a: Boolean = false,
    var b: Boolean = false,
    var start: Boolean = false,
    var select: Boolean = false
)

interface Game {
    val name: String
    val genre: String
    fun init(width: Int, height: Int)
    fun update(dt: Float, input: GameInput)
    fun draw(canvas: Canvas)
    fun reset()
    val isGameOver: Boolean
    val score: Int
}
