package com.example.gameforpmd

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.gameforpmd.data.remote.MetalRates
import com.example.gameforpmd.data.remote.RetrofitClient
import com.example.gameforpmd.ui.records.RecordsFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

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

        // 👉 проверка текущего пользователя
        lifecycleScope.launch {
            val users = MyApp.db.userDao().getAll()
            if (users.isNotEmpty()) {
                val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val currentId = prefs.getInt("current_user_id", -1)
                if (currentId == -1) {
                    saveCurrentUserId(users.first().id)
                    Log.d("MainActivity", "Выбран первый пользователь: ${users.first().name}")
                } else {
                    val selected = users.find { it.id == currentId }
                    Log.d("MainActivity", "Текущий пользователь: ${selected?.name ?: "не найден"}")
                }
            } else {
                Log.d("MainActivity", "Нет зарегистрированных пользователей")
            }
        }

        // 👉 проверка курса золота
        checkGoldRate()
    }

    private fun saveCurrentUserId(id: Int) {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("current_user_id", id).apply()
    }

    private fun checkGoldRate() {
        // Берем сегодняшнюю и вчерашнюю даты
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = sdf.format(Date())
        val yesterday = sdf.format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))

        RetrofitClient.api.getMetalRates(yesterday, today)
            .enqueue(object : Callback<MetalRates> {
                override fun onResponse(call: Call<MetalRates>, response: Response<MetalRates>) {
                    if (response.isSuccessful) {
                        val data = response.body()
                        val goldRate = data?.records?.firstOrNull { it.code == 1 }?.buy
                        if (goldRate != null) {
                            Log.d("API", "Курс золота: $goldRate ₽")

                            // Сохраняем курс в SharedPreferences
                            val prefs = getSharedPreferences("gold_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("gold_rate", goldRate).apply()

                            Toast.makeText(
                                this@MainActivity,
                                "Курс золота: $goldRate ₽",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Log.e("API", "Золото не найдено в ответе")
                        }
                    } else {
                        Log.e("API", "Ошибка ответа: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<MetalRates>, t: Throwable) {
                    Log.e("API", "Ошибка сети: ${t.message}")
                }
            })
    }
}
