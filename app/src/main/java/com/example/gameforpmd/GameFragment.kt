package com.example.gameforpmd

import android.content.Context
import android.graphics.PointF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
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

    private val bugAnimators = mutableListOf<ObjectAnimator>()
    private lateinit var btnStop: Button

    private var score = 0
    private var roundMs = 30_000L
    private var bonusIntervalSec = 15
    private var maxBugs = 5
    private var difficulty = 3

    private var isRunning = false
    private var isPaused = false

    private var timer: CountDownTimer? = null
    private var spawnerJob: Job? = null
    private var bonusJob: Job? = null

    private var remainingMs = 0L

    // --- tilt ---
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

        val p = requireContext().getSharedPreferences("game_settings", Context.MODE_PRIVATE)
        difficulty = p.getInt("difficulty", 3).coerceIn(1, 10)
        maxBugs = p.getInt("maxCockroaches", 5).coerceAtLeast(1)
        bonusIntervalSec = p.getInt("bonusInterval", 15).coerceAtLeast(5)
        val roundSec = p.getInt("roundDuration", 30).coerceAtLeast(10)
        roundMs = roundSec * 1000L

        updateScore(0)
        updateTimer(roundMs)

        gameField.setOnTouchListener { _, event ->
            if (isRunning && !isPaused && event.action == MotionEvent.ACTION_DOWN) {
                updateScore(-5)
            }
            false
        }

        btnStart.setOnClickListener { startGame() }
        btnPause.setOnClickListener { togglePause() }
        btnStop.setOnClickListener { stopGame() }
    }

    // -------------------- Сохранение результата --------------------
    private fun saveResult(points: Int) {
        val prefsUser = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = prefsUser.getInt("current_user_id", -1)
        if (userId == -1) {
            Log.d("GameFragment", "Игрок не выбран, результат не сохранён")
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
            MyApp.db.scoreDao().insert(scoreEntity)
            Log.d("GameFragment", "Результат сохранён: $points очков (userId=$userId)")
        }
    }

    // -------------------- Таймер --------------------
    private fun startTimer(totalMs: Long) {
        remainingMs = totalMs
        timer?.cancel()
        timer = object : CountDownTimer(totalMs, 100L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMs = millisUntilFinished
                updateTimer(millisUntilFinished)
            }

            override fun onFinish() {
                updateTimer(0)
                finishRound()
            }
        }.start()
    }

    private fun updateTimer(ms: Long) {
        val totalSec = (ms / 1000).toInt()
        val m = totalSec / 60
        val s = totalSec % 60
        textTimer.text = "Время: %02d:%02d".format(m, s)
    }

    private fun finishRound() {
        isRunning = false
        spawnerJob?.cancel()
        bonusJob?.cancel()
        Toast.makeText(requireContext(), "Раунд окончен! Счёт: $score", Toast.LENGTH_LONG).show()
        saveResult(score)
    }

    // -------------------- Управление --------------------
    private fun startGame() {
        if (isRunning) return
        isRunning = true
        isPaused = false
        score = 0
        updateScore(0)
        clearField()
        startTimer(roundMs)
        gameField.post {
            startSpawning()
            startBonusSpawning()
        }
    }

    private fun togglePause() {
        if (!isRunning) return
        isPaused = !isPaused
        if (isPaused) {
            timer?.cancel()
            spawnerJob?.cancel()
            bonusJob?.cancel()
            Toast.makeText(requireContext(), "Пауза", Toast.LENGTH_SHORT).show()
        } else {
            startTimer(remainingMs)
            startSpawning()
            startBonusSpawning()
            Toast.makeText(requireContext(), "Продолжаем!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopGame() {
        isRunning = false
        isPaused = false
        timer?.cancel()
        spawnerJob?.cancel()
        bonusJob?.cancel()
        clearField()
        updateTimer(roundMs)
        Toast.makeText(requireContext(), "Игра остановлена", Toast.LENGTH_SHORT).show()
    }

    // -------------------- Жуки --------------------
    private fun startSpawning() {
        spawnerJob?.cancel()
        spawnerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive && isRunning) {
                if (!isPaused) {
                    val currentBugs = (0 until gameField.childCount).count {
                        (gameField.getChildAt(it).tag as? String) == "bug"
                    }
                    if (currentBugs < maxBugs) addBug()
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
                if (isRunning && !isPaused) {
                    updateScore(+10)
                    gameField.removeView(this)
                }
            }
        }
        gameField.addView(iv)

        if (tiltModeActive) {
            // если tilt включён → ставим жука на случайное место, без анимации
            gameField.post {
                val w = gameField.width
                val h = gameField.height
                val margin = 50
                iv.x = Random.nextInt(margin, max(1, w - margin)).toFloat()
                iv.y = Random.nextInt(margin, max(1, h - margin)).toFloat()
            }
        } else {
            // обычное движение по траектории
            moveBugRandom(iv)
        }
    }



    private fun moveBugRandom(iv: ImageView) {
        gameField.post {
            val w = gameField.width
            val h = gameField.height

            // Начало = текущая позиция жука
            val start = PointF(iv.x, iv.y)
            val end = randomEdgePoint(w, h)

            val distance = max(1f, dist(start, end))
            val baseSpeed = (11 - difficulty) * 300L
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

    // -------------------- Бонусы --------------------
    private fun startBonusSpawning() {
        bonusJob?.cancel()
        bonusJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive && isRunning) {
                if (!isPaused) addBonus()
                delay(bonusIntervalSec * 1000L)
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
                if (isRunning && !isPaused) {
                    updateScore(+50)
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
        tiltModeActive = true

        // стопим все анимации у уже появившихся жуков
        for (i in 0 until gameField.childCount) {
            val v = gameField.getChildAt(i)
            if (v.tag == "bug") {
                v.animate().cancel()
                v.clearAnimation()
            }
        }

        // включаем сенсор
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // Music
        screamPlayer?.release()
        screamPlayer = MediaPlayer.create(requireContext(), R.raw.scream)
        screamPlayer?.seekTo(31500) // перемотка на 2 секунды
        screamPlayer?.start()

        // через 5 секунд выключаем tilt-режим
        viewLifecycleOwner.lifecycleScope.launch {
            delay(12500L)
            tiltModeActive = false
            sensorManager?.unregisterListener(this@GameFragment)

            screamPlayer?.stop()
            screamPlayer?.release()
            screamPlayer = null

            // оживляем всех жуков заново
            for (i in 0 until gameField.childCount) {
                val v = gameField.getChildAt(i)
                if (v is ImageView && v.tag == "bug") {
                    moveBugRandom(v)
                }
            }

            Toast.makeText(requireContext(), "Tilt-режим завершён", Toast.LENGTH_SHORT).show()
        }

        Toast.makeText(requireContext(), "Tilt-режим активирован!", Toast.LENGTH_SHORT).show()
    }

    // -------------------- Акселерометр --------------------
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

    // -------------------- Служебные --------------------
    private fun updateScore(delta: Int) {
        score = (score + delta).coerceAtLeast(0)
        textScore.text = "Счёт: $score"
    }

    private fun clearField() {
        gameField.removeAllViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        spawnerJob?.cancel()
        bonusJob?.cancel()
        sensorManager?.unregisterListener(this)
    }
}
