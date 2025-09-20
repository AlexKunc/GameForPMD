package com.example.gameforpmd

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.gameforpmd.data.db.User
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private lateinit var editName: EditText
    private lateinit var buttonRegister: Button
    private lateinit var listUsers: ListView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editName = view.findViewById(R.id.editTextName)
        buttonRegister = view.findViewById(R.id.buttonRegister)
        listUsers = view.findViewById(R.id.listUsers)

        // обработка кнопки регистрации
        buttonRegister.setOnClickListener {
            val name = editName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Введите имя", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val existing = MyApp.db.userDao().getByName(name)
                if (existing != null) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Игрок с именем $name уже существует", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val userId = MyApp.db.userDao().insert(User(name = name)).toInt()
                saveCurrentUserId(userId)

                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Игрок $name зарегистрирован", Toast.LENGTH_SHORT).show()
                    Log.d("RegisterFragment", "Новый пользователь зарегистрирован: $name (id=$userId)")
                    loadUsers()
                }
            }
        }

        // загрузка пользователей при старте
        loadUsers()
    }

    private fun saveCurrentUserId(id: Int) {
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("current_user_id", id).apply()
    }

    private fun getCurrentUserId(): Int {
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("current_user_id", -1)
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            val users = MyApp.db.userDao().getAll()
            requireActivity().runOnUiThread {
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    users.map { it.name }
                )
                listUsers.adapter = adapter

                // выбор пользователя
                listUsers.setOnItemClickListener { _, _, position, _ ->
                    val selectedUser = users[position]
                    saveCurrentUserId(selectedUser.id)
                    Toast.makeText(requireContext(), "Выбран игрок: ${selectedUser.name}", Toast.LENGTH_SHORT).show()
                    Log.d("RegisterFragment", "Текущий пользователь: ${selectedUser.name} (id=${selectedUser.id})")
                }

                Log.d("RegisterFragment", "Users in DB: $users")
            }
        }
    }
}
