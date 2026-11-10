package erasom.juanchito.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import erasom.juanchito.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentGame: Game? = null
    private lateinit var monetizationManager: MonetizationManager
    private lateinit var gamificationManager: GamificationManager
    private lateinit var billingManager: BillingManager
    private lateinit var gameAdapter: GameAdapter

    private var currentPadShape = ConsoleThemeManager.PadShape.SQUIRCLE
    private var currentThemeIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gamificationManager = GamificationManager(this)
        billingManager = BillingManager.getInstance(this)
        monetizationManager = MonetizationManager(this)

        updateCoinsDisplay()
        applyPadStyle()

        // Daily Streak
        val (streak, isNewDay) = gamificationManager.checkDailyLogin()
        if (isNewDay) {
            Toast.makeText(this, "¡Racha diaria de $streak días! +${streak * 25} monedas 🪙", Toast.LENGTH_LONG).show()
            updateCoinsDisplay()
        }

        // Selector de Apariencia (Temas y Formas Squircle / Round / Square)
        binding.btnThemeSettings.setOnClickListener {
            showAppearanceDialog()
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

        // Carrusel Continuo Horizontal de Juegos (Estilo Atari)
        gameAdapter = GameAdapter(GameLibrary.games, selectedIndex = 0) { selectedGame, _ ->
            updateGameShowcase(selectedGame)
        }
        binding.rvGames.adapter = gameAdapter
        binding.rvGames.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Botón Insertar Cartucho / Jugar
        binding.btnLaunchGame.setOnClickListener {
            val game = gameAdapter.getSelectedGame()
            launchGame(game)
        }

        // Botón volver al menú de cartuchos
        binding.btnMenu.setOnClickListener {
            returnToMenu()
        }

        // Actualizar datos del primer juego inicial
        updateGameShowcase(GameLibrary.games[0])

        // Gamepad inputs suaves
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

    private fun updateGameShowcase(game: Game) {
        val meta = GameMetadataProvider.getMetadata(game.name)
        binding.tvPreviewTitle.text = game.name.uppercase()
        binding.tvPreviewGenre.text = game.genre.uppercase()
        binding.tvPreviewSynopsis.text = meta.synopsis
        binding.tvPreviewChapter.text = meta.chapterName

        val density = resources.displayMetrics.density
        val showcaseBg = GameMetadataProvider.createCartridgeDrawable(
            primary = 0xEE141420.toInt(),
            accent = meta.primaryColor,
            radiusDp = 12f,
            density = density
        )
        binding.screenShowcase.background = showcaseBg
    }

    private fun showAppearanceDialog() {
        val options = arrayOf(
            "Mando: Squircle (Curva suave)",
            "Mando: Redondeado (Círculo)",
            "Mando: Cuadrado Clásico",
            "Paleta: Atari 2600 Wood",
            "Paleta: NES Famicom",
            "Paleta: GameBoy Clásico",
            "Paleta: Cyber Neon"
        )

        AlertDialog.Builder(this)
            .setTitle("🎨 Selector de Apariencia de Consola")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> currentPadShape = ConsoleThemeManager.PadShape.SQUIRCLE
                    1 -> currentPadShape = ConsoleThemeManager.PadShape.ROUND
                    2 -> currentPadShape = ConsoleThemeManager.PadShape.SQUARE
                    3 -> currentThemeIndex = 0
                    4 -> currentThemeIndex = 1
                    5 -> currentThemeIndex = 2
                    6 -> currentThemeIndex = 3
                }
                applyPadStyle()
                Toast.makeText(this, "Apariencia actualizada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun applyPadStyle() {
        val theme = ConsoleThemeManager.THEMES[currentThemeIndex]
        val density = resources.displayMetrics.density

        // Actualizar fondos de botones D-pad
        val padDrawable = ConsoleThemeManager.createButtonDrawable(
            bgColor = theme.panel,
            strokeColor = theme.accentOrange,
            shape = currentPadShape,
            sizeDp = 38,
            density = density
        )
        binding.btnUp.background = padDrawable
        binding.btnDown.background = padDrawable
        binding.btnLeft.background = padDrawable
        binding.btnRight.background = padDrawable

        // Botones de acción A y B
        val actionDrawable = ConsoleThemeManager.createButtonDrawable(
            bgColor = theme.accentOrange,
            strokeColor = theme.accentYellow,
            shape = currentPadShape,
            sizeDp = 44,
            density = density
        )
        binding.btnA.background = actionDrawable
        binding.btnB.background = actionDrawable

        // Panel de gamepad y barra superior
        binding.gamepadContainer.setBackgroundColor(theme.panel)
        binding.topBar.setBackgroundColor(theme.panel)
        binding.rootLayout.setBackgroundColor(theme.bg)
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
        binding.menuContainer.isVisible = false
        binding.bannerContainer.isVisible = false
        binding.gameView.isVisible = true
        binding.gameView.setGame(game)
        binding.tvConsoleTitle.text = game.name.uppercase()
    }

    private fun returnToMenu() {
        currentGame = null
        binding.gameView.isVisible = false
        binding.menuContainer.isVisible = true
        binding.bannerContainer.isVisible = true
        binding.tvConsoleTitle.text = "★ ATARI 2600 / NES RETRO CONSOLE ★"
        binding.tvScore.isVisible = false

        monetizationManager.onGameFinished { }
    }

    // Mapeo automático de teclado para dispositivos Android sin touch (TV, Chromebook, etc.)
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (binding.gameView.isVisible) {
            val gi = binding.gameView.gameInput
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> { gi.up = true; return true }
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> { gi.down = true; return true }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> { gi.left = true; return true }
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> { gi.right = true; return true }
                KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_BUTTON_A -> { gi.a = true; return true }
                KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_BUTTON_B -> { gi.b = true; return true }
                KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_BUTTON_SELECT -> { gi.select = true; return true }
                KeyEvent.KEYCODE_BUTTON_START -> { gi.start = true; return true }
            }
        } else {
            // Navegación en menú
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> { binding.rvGames.smoothScrollBy(200, 0); return true }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> { binding.rvGames.smoothScrollBy(-200, 0); return true }
                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_SPACE -> { binding.btnLaunchGame.performClick(); return true }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (binding.gameView.isVisible) {
            val gi = binding.gameView.gameInput
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> { gi.up = false; return true }
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> { gi.down = false; return true }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> { gi.left = false; return true }
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> { gi.right = false; return true }
                KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_BUTTON_A -> { gi.a = false; return true }
                KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_BUTTON_B -> { gi.b = false; return true }
                KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_BUTTON_SELECT -> { gi.select = false; return true }
                KeyEvent.KEYCODE_BUTTON_START -> { gi.start = false; return true }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onBackPressed() {
        if (binding.gameView.isVisible) {
            returnToMenu()
        } else {
            super.onBackPressed()
        }
    }
}
