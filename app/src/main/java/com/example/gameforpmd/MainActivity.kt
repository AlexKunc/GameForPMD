package com.example.gameforpmd

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.gameforpmd.ui.records.RecordsFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        val fragments = listOf(
            GameFragment(),
            RegisterFragment(),
            RulesFragment(),
            AuthorsFragment(),
            SettingsFragment(),
            RecordsFragment()
        )
        val titles = listOf("Игра", "Регистрация", "Правила", "Авторы", "Настройки", "Рекорды")

        viewPager.adapter = ViewPagerAdapter(this, fragments)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()

        // 👉 проверяем при старте, есть ли выбранный пользователь
        lifecycleScope.launch {
            val users = MyApp.db.userDao().getAll()
            if (users.isNotEmpty()) {
                val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val currentId = prefs.getInt("current_user_id", -1)
                if (currentId == -1) {
                    // если никто не выбран — берём первого
                    saveCurrentUserId(users.first().id)
                    Log.d("MainActivity", "Выбран первый пользователь: ${users.first().name}")
                } else {
                    // если уже выбран, логируем
                    val selected = users.find { it.id == currentId }
                    Log.d("MainActivity", "Текущий пользователь: ${selected?.name ?: "не найден"}")
                }
            } else {
                Log.d("MainActivity", "Нет зарегистрированных пользователей")
            }
        }
    }

    private fun saveCurrentUserId(id: Int) {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("current_user_id", id).apply()
    }
}
