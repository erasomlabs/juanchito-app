package erasom.juanchito.app

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private var game: Game? = null
    val gameInput = GameInput()
    @Volatile private var running = false
    private var thread: Thread? = null
    private var lastTime = 0L
    private var onScoreChanged: ((Int) -> Unit)? = null
    private var onGameOver: ((Boolean) -> Unit)? = null
    private var initialized = false
    private var reportedGameOver = false

    init {
        holder.addCallback(this)
    }

    fun setGame(g: Game) {
        game = g
        initialized = false
        reportedGameOver = false
        if (width > 0 && height > 0) {
            g.init(width, height)
            g.reset()
            initialized = true
        }
    }

    fun setScoreListener(listener: (Int) -> Unit) { onScoreChanged = listener }
    fun setGameOverListener(listener: (Boolean) -> Unit) { onGameOver = listener }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        lastTime = System.nanoTime()
        thread = Thread {
            while (running) {
                val canvas = holder.lockCanvas()
                if (canvas != null) {
                    try {
                        val now = System.nanoTime()
                        val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0f, 0.05f)
                        lastTime = now
                        val g = game
                        if (g != null) {
                            if (!initialized && width > 0 && height > 0) {
                                g.init(width, height)
                                g.reset()
                                initialized = true
                            }
                            if (initialized && !g.isGameOver) {
                                g.update(dt, gameInput)
                            }
                            g.draw(canvas)
                            onScoreChanged?.invoke(g.score)
                            if (g.isGameOver) {
                                if (!reportedGameOver) {
                                    reportedGameOver = true
                                    onGameOver?.invoke(true)
                                }
                            } else {
                                reportedGameOver = false
                            }
                        }
                    } finally {
                        holder.unlockCanvasAndPost(canvas)
                    }
                }
                try { Thread.sleep(16) } catch (e: InterruptedException) { }
            }
        }
        thread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        game?.init(width, height)
        game?.reset()
        initialized = true
        reportedGameOver = false
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        thread?.join(500)
        thread = null
    }
}
