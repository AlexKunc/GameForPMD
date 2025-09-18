package com.example.gameforpmd

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private lateinit var textSpeed: TextView
    private lateinit var textMaxCockroaches: TextView
    private lateinit var textBonusInterval: TextView
    private lateinit var textRoundDuration: TextView

    private lateinit var seekBarSpeed: SeekBar
    private lateinit var seekBarMaxCockroaches: SeekBar
    private lateinit var seekBarBonusInterval: SeekBar
    private lateinit var seekBarRoundDuration: SeekBar

    private val PREFS = "game_settings"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textSpeed = view.findViewById(R.id.textSpeed)
        textMaxCockroaches = view.findViewById(R.id.textMaxCockroaches)
        textBonusInterval = view.findViewById(R.id.textBonusInterval)
        textRoundDuration = view.findViewById(R.id.textRoundDuration)

        seekBarSpeed = view.findViewById(R.id.seekBarSpeed)
        seekBarMaxCockroaches = view.findViewById(R.id.seekBarMaxCockroaches)
        seekBarBonusInterval = view.findViewById(R.id.seekBarBonusInterval)
        seekBarRoundDuration = view.findViewById(R.id.seekBarRoundDuration)

        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // Загружаем сохранённые значения
        seekBarSpeed.progress = prefs.getInt("speed", 1)
        seekBarMaxCockroaches.progress = prefs.getInt("maxCockroaches", 5)
        seekBarBonusInterval.progress = prefs.getInt("bonusInterval", 10)
        seekBarRoundDuration.progress = prefs.getInt("roundDuration", 30)

        updateLabels()

        // Слушатели
        seekBarSpeed.setOnSeekBarChangeListener(makeListener("speed", textSpeed, "Скорость игры: "))
        seekBarMaxCockroaches.setOnSeekBarChangeListener(makeListener("maxCockroaches", textMaxCockroaches, "Максимум тараканов: "))
        seekBarBonusInterval.setOnSeekBarChangeListener(makeListener("bonusInterval", textBonusInterval, "Интервал бонусов (сек): "))
        seekBarRoundDuration.setOnSeekBarChangeListener(makeListener("roundDuration", textRoundDuration, "Длительность раунда (сек): "))
    }

    private fun makeListener(key: String, label: TextView, prefix: String): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                label.text = "$prefix $progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                prefs.edit().putInt(key, seekBar?.progress ?: 0).apply()
            }
        }
    }

    private fun updateLabels() {
        textSpeed.text = "Скорость игры: ${seekBarSpeed.progress}"
        textMaxCockroaches.text = "Максимум тараканов: ${seekBarMaxCockroaches.progress}"
        textBonusInterval.text = "Интервал бонусов (сек): ${seekBarBonusInterval.progress}"
        textRoundDuration.text = "Длительность раунда (сек): ${seekBarRoundDuration.progress}"
    }
}
