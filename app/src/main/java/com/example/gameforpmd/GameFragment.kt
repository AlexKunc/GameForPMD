package com.example.gameforpmd

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.content.Context
import android.graphics.PointF
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.widget.*
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlin.math.max
import kotlin.random.Random

class GameFragment : Fragment() {

    private lateinit var gameField: FrameLayout
    private lateinit var textTimer: TextView
    private lateinit var textScore: TextView
    private lateinit var btnStart: Button
    private lateinit var btnPause: Button
    private lateinit var btnStop: Button

    private var score = 0
    private var roundMs = 30_000L
    private var bonusIntervalSec = 15
    private var maxBugs = 5
    private var difficulty  = 3                  // «скорость игры» (1..10)
    private var isRunning = false
    private var isPaused = false

    private var timer: CountDownTimer? = null
    private var spawnerJob: Job? = null
    private var bonusJob: Job? = null

    private val prefsName = "game_settings"

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

        // Подхватываем настройки из SharedPreferences (из вкладки «Настройки»)
        val p = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        difficulty  = p.getInt("speed", 3).coerceIn(1, 10)
        maxBugs = p.getInt("maxCockroaches", 5).coerceAtLeast(1)
        bonusIntervalSec = p.getInt("bonusInterval", 15).coerceAtLeast(5)
        val roundSec = p.getInt("roundDuration", 30).coerceAtLeast(10)
        roundMs = roundSec * 1000L

        updateScore(0)
        updateTimer(roundMs)

        // Клик по пустому полю = промах => -5 очков
        gameField.setOnTouchListener { _, event ->
            if (isRunning && !isPaused && event.action == MotionEvent.ACTION_DOWN) {
                // Если попали не по жуку (жуки сами перехватят клик),
                // считаем промах: штраф
                updateScore(-5)
            }
            false
        }

        btnStart.setOnClickListener { startGame() }
        btnPause.setOnClickListener { togglePause() }
        btnStop.setOnClickListener { stopGame() }
    }

    private fun startGame() {
        if (isRunning) return
        isRunning = true
        isPaused = false
        score = 0
        updateScore(0)
        clearField()
        startTimer(roundMs)
        // Ждём, пока поле измерится, чтобы знать размеры
        gameField.doOnLayout {
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
            // продолжить
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

    // -------------------- Таймер раунда --------------------
    private var remainingMs = 0L

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
    }

    // -------------------- Спавн жуков --------------------
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
                delay(500L) // частота проверок
            }
        }
    }

    private fun addBug() {
        // размер картинки жука ~64dp (подложи drawable bug.png)
        val iv = ImageView(requireContext()).apply {
            val bugVariants = listOf(
                R.drawable.bug,
                R.drawable.bug2,
                R.drawable.bug3
            )
            setImageResource(bugVariants.random()) // добавь в res/drawable/bug.png
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            tag = "bug"
            // тап по жуку = +10 очков, удалить жука
            setOnClickListener {
                if (isRunning && !isPaused) {
                    updateScore(+10)
                    gameField.removeView(this)
                }
            }
        }

        gameField.addView(iv)

        // Случайное место старта и финиша, анимация «ползти»
        gameField.post {
            val w = gameField.width
            val h = gameField.height
            val start = randomEdgePoint(w, h)
            val end = randomEdgePoint(w, h)

            iv.x = start.x
            iv.y = start.y

            val distance = max(1f, dist(start, end))
            val baseSpeed = (11 - difficulty ) * 300L // чем выше speed, тем быстрее (меньше время)
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

            // когда «уполз» — убрать
            viewLifecycleOwner.lifecycleScope.launch {
                delay(duration)
                if (iv.parent != null) gameField.removeView(iv)
            }
        }
    }

    private fun smooth(): TimeInterpolator = android.view.animation.AccelerateDecelerateInterpolator()

    private fun randomEdgePoint(w: Int, h: Int): PointF {
        val edge = Random.nextInt(4) // 0=left,1=top,2=right,3=bottom
        val x: Float
        val y: Float
        val pad = 10
        when (edge) {
            0 -> { x = pad.toFloat(); y = Random.nextInt(pad, max(1, h - pad)).toFloat() }
            1 -> { x = Random.nextInt(pad, max(1, w - pad)).toFloat(); y = pad.toFloat() }
            2 -> { x = (w - pad).toFloat(); y = Random.nextInt(pad, max(1, h - pad)).toFloat() }
            else -> { x = Random.nextInt(pad, max(1, w - pad)).toFloat(); y = (h - pad).toFloat() }
        }
        return PointF(x, y)
    }

    private fun dist(a: PointF, b: PointF): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt(dx*dx + dy*dy)
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
            setImageResource(R.drawable.bonus) // добавь drawable/bonus.png
            tag = "bonus"
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // тап по бонусу = +50 очков
            setOnClickListener {
                if (isRunning && !isPaused) {
                    updateScore(+50)
                    gameField.removeView(this)
                }
            }
        }

        gameField.addView(iv)
        gameField.post {
            // случайная позиция внутри поля
            val w = gameField.width
            val h = gameField.height
            val margin = 100
            iv.x = Random.nextInt(margin, max(1, w - margin)).toFloat()
            iv.y = Random.nextInt(margin, max(1, h - margin)).toFloat()

            // бонус живёт 3 секунды
            viewLifecycleOwner.lifecycleScope.launch {
                delay(3000L)
                if (iv.parent != null) gameField.removeView(iv)
            }
        }
    }

    // -------------------- Служебное --------------------
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
    }
}
