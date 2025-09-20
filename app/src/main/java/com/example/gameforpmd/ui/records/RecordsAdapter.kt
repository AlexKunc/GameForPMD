package com.example.gameforpmd.ui.records

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gameforpmd.R
import com.example.gameforpmd.data.db.Score
import com.example.gameforpmd.data.db.User
import java.text.SimpleDateFormat
import java.util.*

class RecordsAdapter(
    private val scores: List<Score>,
    private val users: Map<Int, User>
) : RecyclerView.Adapter<RecordsAdapter.RecordViewHolder>() {

    class RecordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(R.id.textName)
        val textPoints: TextView = view.findViewById(R.id.textPoints)
        val textSettings: TextView = view.findViewById(R.id.textSettings)
        val textDate: TextView = view.findViewById(R.id.textDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val score = scores[position]
        val user = users[score.userId]

        holder.textName.text = user?.name ?: "Неизвестный"
        holder.textPoints.text = "Очки: ${score.points}"
        holder.textSettings.text = "Сложность: ${score.difficulty}, " +
                "Раунд: ${score.roundDuration}с, Жуки: ${score.maxBugs}, Бонус: ${score.bonusInterval}с"

        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        holder.textDate.text = "Дата: ${sdf.format(Date(score.date))}"
    }

    override fun getItemCount() = scores.size
}
