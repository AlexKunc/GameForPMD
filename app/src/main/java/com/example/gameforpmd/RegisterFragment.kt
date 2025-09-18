package com.example.gameforpmd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import java.util.*

data class Player(
    val name: String,
    val gender: String,
    val course: String,
    val level: Int,
    val birthDate: String,
    val zodiac: String
)

class RegisterFragment : Fragment() {

    private var birthDate: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Привязываем макет fragment_register.xml
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Находим элементы формы
        val editName = view.findViewById<EditText>(R.id.editTextName)
        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupGender)
        val radioMale = view.findViewById<RadioButton>(R.id.radioMale)
        val spinnerCourse = view.findViewById<Spinner>(R.id.spinnerCourse)
        val seekBar = view.findViewById<SeekBar>(R.id.seekBarLevel)
        val calendar = view.findViewById<CalendarView>(R.id.calendarView)
        val imageZodiac = view.findViewById<ImageView>(R.id.imageZodiac)
        val button = view.findViewById<Button>(R.id.buttonRegister)
        val textResult = view.findViewById<TextView>(R.id.textResult)

        // Выбор Мужской по умолчанию
        radioMale.isChecked = true

        // Устанавливаем текущую дату по умолчанию
        val today = Calendar.getInstance()
        val day = today.get(Calendar.DAY_OF_MONTH)
        val month = today.get(Calendar.MONTH) + 1
        val year = today.get(Calendar.YEAR)
        birthDate = "$day.$month.$year"
        calendar.date = today.timeInMillis

        // Заполняем Spinner (курс)
        val courses = arrayOf("1 курс", "2 курс", "3 курс", "4 курс")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, courses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCourse.adapter = adapter

        // Следим за изменением даты рождения
        calendar.setOnDateChangeListener { _, year, month, day ->
            birthDate = "$day.${month + 1}.$year"
        }

        // Кнопка "Регистрация"
        button.setOnClickListener {
            val name = editName.text.toString()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Пожалуйста, введите ФИО", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val genderId = radioGroup.checkedRadioButtonId
            val gender = if (genderId != -1) {
                view.findViewById<RadioButton>(genderId).text.toString()
            } else {
                "Не указан"
            }

            val course = spinnerCourse.selectedItem.toString()
            val level = seekBar.progress
            val zodiac = getZodiacFromDate(birthDate)

            val player = Player(name, gender, course, level, birthDate, zodiac)

            // Обновляем картинку знака зодиака
            updateZodiacImage(zodiac, imageZodiac)

            textResult.text = """
                Игрок: ${player.name}
                Пол: ${player.gender}
                Курс: ${player.course}
                Уровень: ${player.level}
                Дата рождения: ${player.birthDate}
                Знак зодиака: ${player.zodiac}
            """.trimIndent()
        }
    }

    // Определение знака зодиака
    private fun getZodiac(day: Int, month: Int): String {
        return when (month) {
            1 -> if (day < 20) "Козерог" else "Водолей"
            2 -> if (day < 19) "Водолей" else "Рыбы"
            3 -> if (day < 21) "Рыбы" else "Овен"
            4 -> if (day < 20) "Овен" else "Телец"
            5 -> if (day < 21) "Телец" else "Близнецы"
            6 -> if (day < 22) "Близнецы" else "Рак"
            7 -> if (day < 23) "Рак" else "Лев"
            8 -> if (day < 23) "Лев" else "Дева"
            9 -> if (day < 23) "Дева" else "Весы"
            10 -> if (day < 23) "Весы" else "Скорпион"
            11 -> if (day < 23) "Скорпион" else "Стрелец"
            12 -> if (day < 22) "Стрелец" else "Козерог"
            else -> "Неизвестно"
        }
    }

    private fun getZodiacFromDate(date: String): String {
        val parts = date.split(".")
        if (parts.size < 2) return "Неизвестно"
        val day = parts[0].toIntOrNull() ?: return "Неизвестно"
        val month = parts[1].toIntOrNull() ?: return "Неизвестно"
        return getZodiac(day, month)
    }

    private fun updateZodiacImage(zodiac: String, imageView: ImageView) {
        when (zodiac) {
            "Овен" -> imageView.setImageResource(R.drawable.aries)
            "Телец" -> imageView.setImageResource(R.drawable.taurus)
            "Близнецы" -> imageView.setImageResource(R.drawable.gemini)
            "Рак" -> imageView.setImageResource(R.drawable.cancer)
            "Лев" -> imageView.setImageResource(R.drawable.leo)
            "Дева" -> imageView.setImageResource(R.drawable.virgo)
            "Весы" -> imageView.setImageResource(R.drawable.libra)
            "Скорпион" -> imageView.setImageResource(R.drawable.scorpio)
            "Стрелец" -> imageView.setImageResource(R.drawable.sagittarius)
            "Козерог" -> imageView.setImageResource(R.drawable.capricorn)
            "Водолей" -> imageView.setImageResource(R.drawable.aquarius)
            "Рыбы" -> imageView.setImageResource(R.drawable.pisces)
            else -> imageView.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }
}
