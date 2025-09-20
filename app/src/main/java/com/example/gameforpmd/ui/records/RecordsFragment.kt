package com.example.gameforpmd.ui.records

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gameforpmd.MyApp
import com.example.gameforpmd.R
import kotlinx.coroutines.launch

class RecordsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_records, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerRecords)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            val scores = MyApp.db.scoreDao().getBestScores()
            val users = MyApp.db.userDao().getAll().associateBy { it.id }

            requireActivity().runOnUiThread {
                recycler.adapter = RecordsAdapter(scores, users)
            }
        }
    }
}
