package com.example.gameforpmd

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val seekBarDifficulty = view.findViewById<SeekBar>(R.id.seekBarDifficulty)
        val seekBarMaxBugs = view.findViewById<SeekBar>(R.id.seekBarMaxCockroaches)
        val seekBarBonusInterval = view.findViewById<SeekBar>(R.id.seekBarBonusInterval)
        val seekBarRoundDuration = view.findViewById<SeekBar>(R.id.seekBarRoundDuration)

        val textDifficulty = view.findViewById<TextView>(R.id.textDifficultyLabel)
        val textMaxBugs = view.findViewById<TextView>(R.id.textMaxCockroachesLabel)
        val textBonusInterval = view.findViewById<TextView>(R.id.textBonusIntervalLabel)
        val textRoundDuration = view.findViewById<TextView>(R.id.textRoundDurationLabel)

        // минимальные значения
        val minDifficulty = 1
        val minMaxBugs = 5
        val minBonusInterval = 10
        val minRoundDuration = 10

        // SharedPreferences
        val prefs = requireContext().getSharedPreferences("game_settings", Context.MODE_PRIVATE)

        // ✅ Если первый запуск — пишем дефолтные минимумы
        if (!prefs.contains("difficulty")) {
            prefs.edit()
                .putInt("difficulty", minDifficulty)
                .putInt("maxCockroaches", minMaxBugs)
                .putInt("bonusInterval", minBonusInterval)
                .putInt("roundDuration", minRoundDuration)
                .apply()
        }

        // подтягиваем сохранённые значения
        val difficulty = prefs.getInt("difficulty", minDifficulty)
        val maxBugs = prefs.getInt("maxCockroaches", minMaxBugs)
        val bonusInterval = prefs.getInt("bonusInterval", minBonusInterval)
        val roundDuration = prefs.getInt("roundDuration", minRoundDuration)

        // устанавливаем прогресс ползунков
        seekBarDifficulty.progress = difficulty - minDifficulty
        seekBarMaxBugs.progress = maxBugs - minMaxBugs
        seekBarBonusInterval.progress = bonusInterval - minBonusInterval
        seekBarRoundDuration.progress = roundDuration - minRoundDuration

        // подписываем текстовые метки
        textDifficulty.text = "Уровень сложности: $difficulty"
        textMaxBugs.text = "Максимум тараканов: $maxBugs"
        textBonusInterval.text = "Интервал появления бонусов (сек): $bonusInterval"
        textRoundDuration.text = "Длительность раунда (сек): $roundDuration"

        // слушатели изменения прогресса
        seekBarDifficulty.setOnSeekBarChangeListener(simpleListener {
            textDifficulty.text = "Уровень сложности: ${it + minDifficulty}"
        })

        seekBarMaxBugs.setOnSeekBarChangeListener(simpleListener {
            textMaxBugs.text = "Максимум тараканов: ${it + minMaxBugs}"
        })

        seekBarBonusInterval.setOnSeekBarChangeListener(simpleListener {
            textBonusInterval.text = "Интервал появления бонусов (сек): ${it + minBonusInterval}"
        })

        seekBarRoundDuration.setOnSeekBarChangeListener(simpleListener {
            textRoundDuration.text = "Длительность раунда (сек): ${it + minRoundDuration}"
        })

        // кнопка сохранить
        val btnSave = view.findViewById<Button>(R.id.btnSaveSettings)
        btnSave.setOnClickListener {
            prefs.edit()
                .putInt("difficulty", seekBarDifficulty.progress + minDifficulty)
                .putInt("maxCockroaches", seekBarMaxBugs.progress + minMaxBugs)
                .putInt("bonusInterval", seekBarBonusInterval.progress + minBonusInterval)
                .putInt("roundDuration", seekBarRoundDuration.progress + minRoundDuration)
                .apply()

            Toast.makeText(requireContext(), "Настройки сохранены", Toast.LENGTH_SHORT).show()
        }
    }

    private fun simpleListener(onChange: (Int) -> Unit) =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                onChange(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
}
