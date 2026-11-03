package erasom.juanchito.app

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentGame: Game? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Grid de juegos
        val adapter = GameAdapter(GameLibrary.games) { game ->
            launchGame(game)
        }
        binding.rvGames.adapter = adapter
        binding.rvGames.layoutManager = GridLayoutManager(this, 3)

        // Gamepad input wiring
        wireButton(binding.btnUp) { binding.gameView.input.up = it }
        wireButton(binding.btnDown) { binding.gameView.input.down = it }
        wireButton(binding.btnLeft) { binding.gameView.input.left = it }
        wireButton(binding.btnRight) { binding.gameView.input.right = it }
        wireButton(binding.btnA) { binding.gameView.input.a = it }
        wireButton(binding.btnB) { binding.gameView.input.b = it }

        binding.btnStart.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { binding.gameView.input.start = true; true }
                MotionEvent.ACTION_UP -> { binding.gameView.input.start = false; true }
                else -> false
            }
        }
        binding.btnSelect.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { binding.gameView.input.select = true; true }
                MotionEvent.ACTION_UP -> { binding.gameView.input.select = false; true }
                else -> false
            }
        }

        binding.gameView.setScoreListener { score ->
            runOnUiThread {
                binding.tvScore.text = "SCORE: $score"
                binding.tvScore.isVisible = true
            }
        }
        binding.gameView.setGameOverListener {
            runOnUiThread {
                Toast.makeText(this, "Game Over", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun wireButton(button: Button, setter: (Boolean) -> Unit) {
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { setter(true); true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { setter(false); true }
                else -> false
            }
        }
    }

    private fun launchGame(game: Game) {
        currentGame = game
        binding.rvGames.isVisible = false
        binding.gameView.isVisible = true
        binding.gameView.setGame(game)
        binding.tvConsoleTitle.text = game.name.uppercase()
    }

    private fun returnToMenu() {
        currentGame?.let { }
        binding.gameView.isVisible = false
        binding.rvGames.isVisible = true
        binding.tvConsoleTitle.text = getString(R.string.console_title)
        binding.tvScore.isVisible = false
    }

    override fun onBackPressed() {
        if (binding.gameView.isVisible) {
            returnToMenu()
        } else {
            super.onBackPressed()
        }
    }
}
