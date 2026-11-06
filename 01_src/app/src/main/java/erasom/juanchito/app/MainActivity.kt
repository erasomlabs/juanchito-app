package erasom.juanchito.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import erasom.juanchito.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentGame: Game? = null
    private lateinit var monetizationManager: MonetizationManager
    private lateinit var gamificationManager: GamificationManager
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gamificationManager = GamificationManager(this)
        billingManager = BillingManager.getInstance(this)
        monetizationManager = MonetizationManager(this)

        updateCoinsDisplay()

        // Comprobar recompensa diaria (Daily Streak)
        val (streak, isNewDay) = gamificationManager.checkDailyLogin()
        if (isNewDay) {
            Toast.makeText(this, "¡Racha diaria de $streak días! +${streak * 25} monedas 🪙", Toast.LENGTH_LONG).show()
            updateCoinsDisplay()
        }

        // Configuración de Billing (Quitar anuncios)
        binding.btnRemoveAds.setOnClickListener {
            billingManager.launchBillingFlow(this)
        }
        billingManager.onPurchaseCompleted = {
            runOnUiThread {
                binding.btnRemoveAds.isVisible = false
                monetizationManager.hideBanner()
                Toast.makeText(this, "¡Anuncios eliminados permanentemente!", Toast.LENGTH_SHORT).show()
            }
        }
        if (billingManager.isAdsRemoved()) {
            binding.btnRemoveAds.isVisible = false
        }

        // Inicializar Privacidad (UMP / GDPR) y Anuncios
        monetizationManager.initPrivacyAndAds {
            runOnUiThread {
                monetizationManager.attachBanner(binding.bannerContainer)
            }
        }

        // Grid de juegos (Colección de 30 títulos retro)
        val adapter = GameAdapter(GameLibrary.games) { game ->
            launchGame(game)
        }
        binding.rvGames.adapter = adapter
        binding.rvGames.layoutManager = GridLayoutManager(this, 3)

        // Gamepad input wiring
        wireButton(binding.btnUp) { binding.gameView.gameInput.up = it }
        wireButton(binding.btnDown) { binding.gameView.gameInput.down = it }
        wireButton(binding.btnLeft) { binding.gameView.gameInput.left = it }
        wireButton(binding.btnRight) { binding.gameView.gameInput.right = it }
        wireButton(binding.btnA) { binding.gameView.gameInput.a = it }
        wireButton(binding.btnB) { binding.gameView.gameInput.b = it }

        binding.btnStart.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { binding.gameView.gameInput.start = true; true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { binding.gameView.gameInput.start = false; true }
                else -> false
            }
        }
        binding.btnSelect.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { binding.gameView.gameInput.select = true; true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { binding.gameView.gameInput.select = false; true }
                else -> false
            }
        }

        binding.gameView.setScoreListener { score ->
            runOnUiThread {
                val hs = currentGame?.let { gamificationManager.getHighScore(it.name) } ?: 0
                binding.tvScore.text = "SCORE: $score | HI: $hs"
                binding.tvScore.isVisible = true
            }
        }

        binding.gameView.setGameOverListener {
            runOnUiThread {
                handleGameOver()
            }
        }
    }

    private fun updateCoinsDisplay() {
        binding.tvCoins.text = "🪙 ${gamificationManager.getCoins()}"
    }

    private fun handleGameOver() {
        val game = currentGame ?: return
        val finalScore = game.score
        val isNewRecord = gamificationManager.checkAndSaveHighScore(game.name, finalScore)
        gamificationManager.addCoins(finalScore / 10)
        updateCoinsDisplay()

        // Diálogo con oportunidad de revivir vía Rewarded Ad o Interstitial de salida
        AlertDialog.Builder(this)
            .setTitle(if (isNewRecord) "¡Nuevo Récord Arcade!" else "Game Over")
            .setMessage("Puntuación: $finalScore\n¿Deseas ver un video para revivir y seguir jugando?")
            .setPositiveButton("Revivir (Video)") { _, _ ->
                monetizationManager.showRewardedRevive(
                    onRewarded = {
                        runOnUiThread {
                            game.reset()
                            Toast.makeText(this, "¡Segunda oportunidad activada!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailedOrDismissed = {
                        runOnUiThread {
                            returnToMenu()
                        }
                    }
                )
            }
            .setNegativeButton("Volver al menú") { _, _ ->
                returnToMenu()
            }
            .setCancelable(false)
            .show()
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
        binding.bannerContainer.isVisible = false
        binding.gameView.isVisible = true
        binding.gameView.setGame(game)
        binding.tvConsoleTitle.text = game.name.uppercase()
    }

    private fun returnToMenu() {
        currentGame = null
        binding.gameView.isVisible = false
        binding.rvGames.isVisible = true
        binding.bannerContainer.isVisible = true
        binding.tvConsoleTitle.text = getString(R.string.console_title)
        binding.tvScore.isVisible = false

        monetizationManager.onGameFinished {
            // Callback tras intersticial si aplica
        }
    }

    override fun onBackPressed() {
        if (binding.gameView.isVisible) {
            returnToMenu()
        } else {
            super.onBackPressed()
        }
    }
}
