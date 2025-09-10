package com.example.gameforpmd

import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.*

// Структура данных игрока
data class Player(
    val name: String,
    val gender: String,
    val course: String,
    val level: Int,
    val birthDate: String,
    val zodiac: String
)

class MainActivity : AppCompatActivity() {

    private var birthDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Находим элементы формы
        val editName = findViewById<EditText>(R.id.editTextName)
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupGender)
        val radioMale = findViewById<RadioButton>(R.id.radioMale)
        val spinnerCourse = findViewById<Spinner>(R.id.spinnerCourse)
        val seekBar = findViewById<SeekBar>(R.id.seekBarLevel)
        val calendar = findViewById<CalendarView>(R.id.calendarView)
        val imageZodiac = findViewById<ImageView>(R.id.imageZodiac)
        val button = findViewById<Button>(R.id.buttonRegister)
        val textResult = findViewById<TextView>(R.id.textResult)

        // Выбор Мужской по умолчанию
        radioMale.isChecked = true

        // Устанавливаем текущую дату по умолчанию
        val today = Calendar.getInstance()
        val day = today.get(Calendar.DAY_OF_MONTH)
        val month = today.get(Calendar.MONTH) + 1 // месяцы начинаются с 0
        val year = today.get(Calendar.YEAR)
        birthDate = "$day.$month.$year"
        calendar.date = today.timeInMillis // визуально выделяем текущую дату

        // Устанавливаем отступы для Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Заполняем Spinner (курс)
        val courses = arrayOf("1 курс", "2 курс", "3 курс", "4 курс")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, courses)
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
                Toast.makeText(this, "Пожалуйста, введите ФИО", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val genderId = radioGroup.checkedRadioButtonId
            val gender = if (genderId != -1) {
                findViewById<RadioButton>(genderId).text.toString()
            } else {
                "Не указан"
            }

            val course = spinnerCourse.selectedItem.toString()
            val level = seekBar.progress
            val zodiac = getZodiacFromDate(birthDate)

            val player = Player(name, gender, course, level, birthDate, zodiac)

            //Обновляем картинку здесь
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

    // Вспомогательная функция для даты (строкой)
    private fun getZodiacFromDate(date: String): String {
        val parts = date.split(".")
        if (parts.size < 2) return "Неизвестно"
        val day = parts[0].toIntOrNull() ?: return "Неизвестно"
        val month = parts[1].toIntOrNull() ?: return "Неизвестно"
        return getZodiac(day, month)
    }

    // Функция для смены картинки
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
