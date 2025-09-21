package com.example.gameforpmd.ui.game

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GameViewModel : ViewModel() {

    // Очки
    private val _score = MutableLiveData(0)
    val score: LiveData<Int> get() = _score

    // Таймер (в миллисекундах)
    private val _remainingMs = MutableLiveData<Long>()
    val remainingMs: LiveData<Long> get() = _remainingMs

    private var timer: CountDownTimer? = null

    // Состояние игры
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> get() = _isRunning

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> get() = _isPaused

    private val _gameFinished = MutableLiveData<Boolean>()
    val gameFinished: LiveData<Boolean> get() = _gameFinished



    // Настройки
    var difficulty: Int = 3
    var maxBugs: Int = 5
    var bonusIntervalSec: Int = 15
    var roundMs: Long = 30_000

    // ---------------- Методы ----------------
    fun startGame() {
        _score.value = 0
        _isRunning.value = true
        _isPaused.value = false
        startTimer(roundMs)
    }

    fun setRemainingTime(ms: Long) {
        _remainingMs.value = ms
    }

    fun pauseGame() {
        if (_isRunning.value) {
            _isPaused.value = !_isPaused.value
            if (_isPaused.value) {
                timer?.cancel()
            } else {
                startTimer(_remainingMs.value ?: roundMs)
            }
        }
    }

    fun stopGame() {
        _isRunning.value = false
        _isPaused.value = false
        timer?.cancel()
        _remainingMs.value = roundMs
    }

    fun addScore(delta: Int) {
        _score.value = (_score.value ?: 0) + delta
    }

    fun startTimer(totalMs: Long) {
        timer?.cancel()
        timer = object : CountDownTimer(totalMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                _remainingMs.value = millisUntilFinished
            }

            override fun onFinish() {
                _remainingMs.value = 0
                _isRunning.value = false
                _gameFinished.value = true
            }
        }.start()
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}
