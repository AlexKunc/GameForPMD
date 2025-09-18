package com.example.gameforpmd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment

class AuthorsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_authors, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listView = view.findViewById<ListView>(R.id.listAuthors)

        val authors = listOf(
            Author("Александр Кунц", R.drawable.author1),
            Author("Александр Леонов", R.drawable.author2),
        )

        listView.adapter = AuthorsAdapter(layoutInflater, authors)

         listView.setOnItemClickListener { _, _, position, _ ->
             val a = authors[position]
             Toast.makeText(requireContext(), a.name + " группа ИП-212", Toast.LENGTH_SHORT).show()
         }
    }
}
