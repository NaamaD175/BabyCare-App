package com.example.babycareapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.babycareapp.databinding.BabysitterItemBinding
import com.example.babycareapp.models.Babysitter

class BabysitterAdapter(
    private var babysitters: List<Babysitter>,
    private val onItemClick: (Babysitter) -> Unit
) : RecyclerView.Adapter<BabysitterAdapter.BabysitterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BabysitterViewHolder {
        val binding = BabysitterItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BabysitterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BabysitterViewHolder, position: Int) {
        val babysitter = babysitters[position]
        holder.bind(babysitter)
    }

    override fun getItemCount(): Int = babysitters.size

    fun updateList(newList: List<Babysitter>) {
        babysitters = newList
        notifyDataSetChanged()
    }

    inner class BabysitterViewHolder(private val binding: BabysitterItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(babysitter: Babysitter) {
            binding.itemLBLName.text = babysitter.name
            binding.itemLBLAddress.text = babysitter.address
            binding.itemLBLPrice.text = "₪${babysitter.price} per hour"
            binding.itemLBLAbout.text = babysitter.description

            Glide.with(binding.itemIMGPhoto.context)
                .load(babysitter.imageUrl)
                .into(binding.itemIMGPhoto)

            binding.root.setOnClickListener {
                onItemClick(babysitter)
            }
        }
    }
}
