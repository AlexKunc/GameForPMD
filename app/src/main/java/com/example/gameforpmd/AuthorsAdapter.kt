package com.example.gameforpmd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

data class Author(
    val name: String,
    val photoRes: Int
)

class AuthorsAdapter(
    private val inflater: LayoutInflater,
    private val items: List<Author>
) : BaseAdapter() {

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Any = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: ViewHolder
        val view: View

        if (convertView == null) {
            view = inflater.inflate(R.layout.item_author, parent, false)
            holder = ViewHolder(
                view.findViewById(R.id.imageAvatar),
                view.findViewById(R.id.textName)
            )
            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }

        val item = items[position]
        holder.image.setImageResource(item.photoRes)
        holder.name.text = item.name

        return view
    }

    private data class ViewHolder(
        val image: ImageView,
        val name: TextView
    )
}
