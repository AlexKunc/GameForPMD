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

    private val prefsName = "game_settings"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        // Уровень сложности
        val seekBarDifficulty = view.findViewById<SeekBar>(R.id.seekBarDifficulty)
        val textDifficultyLabel = view.findViewById<TextView>(R.id.textDifficultyLabel)
        seekBarDifficulty.progress = prefs.getInt("difficulty", 3)
        textDifficultyLabel.text = "Уровень сложности: ${seekBarDifficulty.progress}"
        seekBarDifficulty.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                textDifficultyLabel.text = "Уровень сложности: $progress"
                prefs.edit().putInt("difficulty", progress).apply()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Максимум тараканов
        val seekBarMaxCockroaches = view.findViewById<SeekBar>(R.id.seekBarMaxCockroaches)
        val textMaxCockroachesLabel = view.findViewById<TextView>(R.id.textMaxCockroachesLabel)
        seekBarMaxCockroaches.progress = prefs.getInt("maxCockroaches", 5)
        textMaxCockroachesLabel.text = "Максимум тараканов: ${seekBarMaxCockroaches.progress}"
        seekBarMaxCockroaches.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                textMaxCockroachesLabel.text = "Максимум тараканов: $progress"
                prefs.edit().putInt("maxCockroaches", progress).apply()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Интервал бонусов
        val seekBarBonusInterval = view.findViewById<SeekBar>(R.id.seekBarBonusInterval)
        val textBonusIntervalLabel = view.findViewById<TextView>(R.id.textBonusIntervalLabel)
        seekBarBonusInterval.progress = prefs.getInt("bonusInterval", 15)
        textBonusIntervalLabel.text = "Интервал появления бонусов (сек): ${seekBarBonusInterval.progress}"
        seekBarBonusInterval.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                textBonusIntervalLabel.text = "Интервал появления бонусов (сек): $progress"
                prefs.edit().putInt("bonusInterval", progress).apply()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Длительность раунда
        val seekBarRoundDuration = view.findViewById<SeekBar>(R.id.seekBarRoundDuration)
        val textRoundDurationLabel = view.findViewById<TextView>(R.id.textRoundDurationLabel)
        seekBarRoundDuration.progress = prefs.getInt("roundDuration", 30)
        textRoundDurationLabel.text = "Длительность раунда (сек): ${seekBarRoundDuration.progress}"
        seekBarRoundDuration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                textRoundDurationLabel.text = "Длительность раунда (сек): $progress"
                prefs.edit().putInt("roundDuration", progress).apply()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }
}
