package com.example.gameforpmd

import android.content.Context
import android.graphics.PointF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.example.gameforpmd.ui.game.GameViewModel
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.gameforpmd.data.db.Score
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.media.MediaPlayer
import android.view.animation.AccelerateDecelerateInterpolator

class GameFragment : Fragment(), SensorEventListener {

    private lateinit var gameField: FrameLayout
    private lateinit var textTimer: TextView
    private lateinit var textScore: TextView

    private var screamPlayer: MediaPlayer? = null
    private lateinit var btnStart: Button
    private lateinit var btnPause: Button
    private lateinit var btnStop: Button

    private val bugAnimators = mutableListOf<ObjectAnimator>()
    private val gameViewModel: GameViewModel by viewModel()

    private var goldenBugJob: Job? = null
    private var spawnerJob: Job? = null
    private var bonusJob: Job? = null

    private var goldRate: Float = 0f
    private var sensorManager: SensorManager? = null
    private var tiltModeActive = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_game, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        gameField = view.findViewById(R.id.gameField)
        textTimer = view.findViewById(R.id.textTimer)
        textScore = view.findViewById(R.id.textScore)
        btnStart = view.findViewById(R.id.btnStart)
        btnPause = view.findViewById(R.id.btnPause)
        btnStop = view.findViewById(R.id.btnStop)

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager

        gameViewModel.score.observe(viewLifecycleOwner) { newScore ->
            textScore.text = "Счёт: $newScore"
        }

        gameViewModel.gameFinished.observe(viewLifecycleOwner) {
            finishRound()
        }

        gameViewModel.remainingMs.observe(viewLifecycleOwner) { ms ->
            val totalSec = ((ms ?: gameViewModel.roundMs) / 1000).toInt()
            val m = totalSec / 60
            val s = totalSec % 60
            textTimer.text = "Время: %02d:%02d".format(m, s)

        }

        val p = requireContext().getSharedPreferences("game_settings", Context.MODE_PRIVATE)
        gameViewModel.difficulty = p.getInt("difficulty", 3).coerceIn(1, 10)
        gameViewModel.maxBugs = p.getInt("maxCockroaches", 5).coerceAtLeast(1)
        gameViewModel.bonusIntervalSec = p.getInt("bonusInterval", 15).coerceAtLeast(5)
        val roundSec = p.getInt("roundDuration", 30).coerceAtLeast(10)
        gameViewModel.roundMs = roundSec * 1000L

        val goldPrefs = requireContext().getSharedPreferences("gold_prefs", Context.MODE_PRIVATE)
        val savedRate = goldPrefs.getString("gold_rate", null)
        goldRate = savedRate?.replace(",", ".")?.toFloatOrNull() ?: 0f
        Log.d("GameFragment", "Загружен курс золота: $goldRate ₽")

        gameField.setOnTouchListener { _, event ->
            if (gameViewModel.isRunning.value && !gameViewModel.isPaused.value && event.action == MotionEvent.ACTION_DOWN) {
                gameViewModel.addScore(-500)
            }
            false
        }

        btnStart.setOnClickListener { startGame() }
        btnPause.setOnClickListener { togglePause() }
        btnStop.setOnClickListener { stopGame() }

        if (gameViewModel.isRunning.value) {
            gameField.post {
                startSpawning()
                startBonusSpawning()
                startGoldenBugSpawning()
            }
        }
    }

    private fun startGoldenBugSpawning() {
        goldenBugJob?.cancel()
        goldenBugJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive && gameViewModel.isRunning.value) {
                if (!gameViewModel.isPaused.value) addGoldenBug()
                delay(20_000L)
            }
        }
    }

    private fun addGoldenBug() {
        val iv = ImageView(requireContext()).apply {
            setImageResource(R.drawable.golden_bug)
            tag = "golden_bug"
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                if (gameViewModel.isRunning.value && !gameViewModel.isPaused.value) {
                    val points = goldRate.toInt().coerceAtLeast(1)
                    gameViewModel.addScore(points)
                    gameField.removeView(this)
                    Toast.makeText(
                        requireContext(),
                        "Золотой жук! +$points очков",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        gameField.addView(iv)

        gameField.post {
            val w = gameField.width
            val h = gameField.height
            val margin = 100
            iv.x = Random.nextInt(margin, max(1, w - margin)).toFloat()
            iv.y = Random.nextInt(margin, max(1, h - margin)).toFloat()
            moveBugRandom(iv)
        }
    }

    private fun saveResult(points: Int) {
        val prefsUser = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = prefsUser.getInt("current_user_id", -1)
        if (userId == -1) {
            Log.d("GameFragment", "saveResult: userId == -1 → результат не сохраняется")
            return
        }

        val prefsGame = requireContext().getSharedPreferences("game_settings", Context.MODE_PRIVATE)
        val difficultyNow = prefsGame.getInt("difficulty", 3).coerceIn(1, 10)
        val maxBugsNow = prefsGame.getInt("maxCockroaches", 5).coerceAtLeast(1)
        val bonusIntervalNow = prefsGame.getInt("bonusInterval", 15).coerceAtLeast(5)
        val roundDurationNow = prefsGame.getInt("roundDuration", 30).coerceAtLeast(10)

        lifecycleScope.launch {
            val scoreEntity = Score(
                userId = userId,
                points = points,
                difficulty = difficultyNow,
                roundDuration = roundDurationNow,
                maxBugs = maxBugsNow,
                bonusInterval = bonusIntervalNow
            )
            Log.d("GameFragment", "saveResult: вставляем $scoreEntity")
            MyApp.db.scoreDao().insert(scoreEntity)
            Log.d("GameFragment", "saveResult: вставка завершена")
        }
    }

    private fun finishRound() {
//        gameViewModel.stopGame()
        Toast.makeText( 
            requireContext(),
            "Раунд окончен! Счёт: ${gameViewModel.score.value}",
            Toast.LENGTH_LONG
        ).show()
        saveResult(gameViewModel.score.value ?: 0)
    }

    private fun startGame() {
        gameViewModel.startGame()
        clearField()
        gameField.post {
            startSpawning()
            startBonusSpawning()
            startGoldenBugSpawning()
        }
        Toast.makeText(requireContext(), "Игра началась!", Toast.LENGTH_SHORT).show()
    }

    private fun togglePause() {
        gameViewModel.pauseGame()
        if (gameViewModel.isPaused.value) {
            spawnerJob?.cancel()
            bonusJob?.cancel()
            goldenBugJob?.cancel()
            Toast.makeText(requireContext(), "Пауза", Toast.LENGTH_SHORT).show()
        } else {
            gameField.post {
                startSpawning()
                startBonusSpawning()
                startGoldenBugSpawning()
            }
            Toast.makeText(requireContext(), "Продолжаем!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopGame() {
        gameViewModel.stopGame()
        spawnerJob?.cancel()
        bonusJob?.cancel()
        goldenBugJob?.cancel()
        clearField()
        Toast.makeText(requireContext(), "Игра остановлена", Toast.LENGTH_SHORT).show()
    }

    private fun startSpawning() {
        spawnerJob?.cancel()
        spawnerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive && gameViewModel.isRunning.value) {
                if (!gameViewModel.isPaused.value) {
                    val currentBugs = (0 until gameField.childCount).count {
                        (gameField.getChildAt(it).tag as? String) == "bug"
                    }
                    if (currentBugs < gameViewModel.maxBugs) addBug()
                }
                delay(500L)
            }
        }
    }

    private fun addBug() {
        val iv = ImageView(requireContext()).apply {
            val bugVariants = listOf(R.drawable.bug, R.drawable.bug2, R.drawable.bug3)
            setImageResource(bugVariants.random())
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            tag = "bug"
            setOnClickListener {
                if (gameViewModel.isRunning.value && !gameViewModel.isPaused.value) {
                    gameViewModel.addScore(+1000)
                    gameField.removeView(this)
                }
            }
        }
        gameField.addView(iv)

        gameField.post {
            val w = gameField.width
            val h = gameField.height
            val margin = 100
            iv.x = Random.nextInt(margin, max(1, w - margin)).toFloat()
            iv.y = Random.nextInt(margin, max(1, h - margin)).toFloat()
            if (!tiltModeActive) moveBugRandom(iv)
        }
    }

    private fun moveBugRandom(iv: ImageView) {
        gameField.post {
            val w = gameField.width
            val h = gameField.height
            val start = PointF(iv.x, iv.y)
            val end = randomEdgePoint(w, h)
            val distance = max(1f, dist(start, end))
            val baseSpeed = (11 - gameViewModel.difficulty) * 300L
            val duration = (distance / 300f * baseSpeed).toLong().coerceIn(500L, 6000L)

            val animX = ObjectAnimator.ofFloat(iv, View.X, start.x, end.x).apply {
                this.duration = duration
                interpolator = smooth()
            }
            val animY = ObjectAnimator.ofFloat(iv, View.Y, start.y, end.y).apply {
                this.duration = duration
                interpolator = smooth()
            }
            animX.start()
            animY.start()

            viewLifecycleOwner.lifecycleScope.launch {
                delay(duration)
                if (iv.parent != null && !tiltModeActive) gameField.removeView(iv)
            }
        }
    }

    private fun smooth(): TimeInterpolator = AccelerateDecelerateInterpolator()

    private fun randomEdgePoint(w: Int, h: Int): PointF {
        val edge = Random.Default.nextInt(4)
        val pad = 10
        return when (edge) {
            0 -> PointF(pad.toFloat(), Random.nextInt(pad, max(1, h - pad)).toFloat())
            1 -> PointF(Random.nextInt(pad, max(1, w - pad)).toFloat(), pad.toFloat())
            2 -> PointF((w - pad).toFloat(), Random.nextInt(pad, max(1, h - pad)).toFloat())
            else -> PointF(Random.nextInt(pad, max(1, w - pad)).toFloat(), (h - pad).toFloat())
        }
    }

    private fun dist(a: PointF, b: PointF): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun startBonusSpawning() {
        bonusJob?.cancel()
        bonusJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive && gameViewModel.isRunning.value) {
                if (!gameViewModel.isPaused.value) addBonus()
                delay(gameViewModel.bonusIntervalSec * 1000L)
            }
        }
    }

    private fun addBonus() {
        val iv = ImageView(requireContext()).apply {
            setImageResource(R.drawable.bonus)
            tag = "bonus"
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                if (gameViewModel.isRunning.value && !gameViewModel.isPaused.value) {
                    gameField.removeView(this)
                    activateTiltMode()
                }
            }
        }
        gameField.addView(iv)
        gameField.post {
            val w = gameField.width
            val h = gameField.height
            val margin = 100
            iv.x = Random.nextInt(margin, max(1, w - margin)).toFloat()
            iv.y = Random.nextInt(margin, max(1, h - margin)).toFloat()
            viewLifecycleOwner.lifecycleScope.launch {
                delay(3000L)
                if (iv.parent != null) gameField.removeView(iv)
            }
        }
    }

    private fun activateTiltMode() {
        if (tiltModeActive) return
        tiltModeActive = true
        for (i in 0 until gameField.childCount) {
            val v = gameField.getChildAt(i)
            if (v.tag == "bug") {
                v.animate().cancel()
                v.clearAnimation()
            }
        }

        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        screamPlayer?.release()
        screamPlayer = MediaPlayer.create(requireContext(), R.raw.scream)
        screamPlayer?.seekTo(31500)
        screamPlayer?.start()

        viewLifecycleOwner.lifecycleScope.launch {
            delay(9400L)
            tiltModeActive = false
            sensorManager?.unregisterListener(this@GameFragment)
            screamPlayer?.stop()
            screamPlayer?.release()
            screamPlayer = null
            for (i in 0 until gameField.childCount) {
                val v = gameField.getChildAt(i)
                if (v is ImageView && v.tag == "bug") moveBugRandom(v)
            }
            Toast.makeText(requireContext(), "Tilt-режим завершён", Toast.LENGTH_SHORT).show()
        }

        Toast.makeText(requireContext(), "Tilt-режим активирован!", Toast.LENGTH_SHORT).show()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!tiltModeActive || event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val ax = -event.values[0] * 5
        val ay = event.values[1] * 5
        for (i in 0 until gameField.childCount) {
            val v = gameField.getChildAt(i)
            if (v.tag == "bug") {
                v.x = (v.x + ax).coerceIn(0f, gameField.width - v.width.toFloat())
                v.y = (v.y + ay).coerceIn(0f, gameField.height - v.height.toFloat())
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun clearField() {
        gameField.removeAllViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        spawnerJob?.cancel()
        bonusJob?.cancel()
        goldenBugJob?.cancel()
        sensorManager?.unregisterListener(this)
        screamPlayer?.stop()
        screamPlayer?.release()
        screamPlayer = null
    }
}
