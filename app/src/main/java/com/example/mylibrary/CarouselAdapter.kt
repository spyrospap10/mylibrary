package com.example.mylibrary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class CarouselItem(val imageRes: Int, val title: String, val desc: String)

class CarouselAdapter(private val items: List<CarouselItem>) : RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder>() {

    class CarouselViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivCarouselImage)
        val tvTitle: TextView = view.findViewById(R.id.tvCarouselTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvCarouselDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_carousel, parent, false)
        return CarouselViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
        val item = items[position]
        holder.ivImage.setImageResource(item.imageRes)
        holder.tvTitle.text = item.title
        holder.tvDesc.text = item.desc
    }

    override fun getItemCount(): Int = items.size
}