package com.example.babycareapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.babycareapp.databinding.BabysitterItemBinding
import com.example.babycareapp.models.Babysitter

class BabysitterAdapter(
    private var babysitters: List<Babysitter>, //List of the babysitters
    private val onItemClick: (Babysitter) -> Unit //Callback function
) : RecyclerView.Adapter<BabysitterAdapter.BabysitterViewHolder>() {

    //Build the empty babysitter card
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BabysitterViewHolder {
        val binding = BabysitterItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BabysitterViewHolder(binding)
    }

    //Filling out the babysitter's card with data
    override fun onBindViewHolder(holder: BabysitterViewHolder, position: Int) {
        val babysitter = babysitters[position]
        holder.bind(babysitter)
    }

    //The amount of babysitter's cards
    override fun getItemCount(): Int = babysitters.size

    //Refresh the display
    fun updateList(newList: List<Babysitter>) {
        babysitters = newList
        notifyDataSetChanged()
    }

    //Filling out the babysitter's card with data
    inner class BabysitterViewHolder(private val binding: BabysitterItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(babysitter: Babysitter) {
            //Details about the babysitter
            binding.itemLBLName.text = babysitter.name
            binding.itemLBLAddress.text = babysitter.address
            binding.itemLBLPrice.text = "₪${babysitter.price} per hour"
            binding.itemLBLAbout.text = babysitter.description

            //Load the image of the babysitter
            Glide.with(binding.itemIMGPhoto.context)
                .load(babysitter.imageUrl)
                .into(binding.itemIMGPhoto)

            //Click on the babysitter card
            binding.root.setOnClickListener {
                onItemClick(babysitter)
            }
        }
    }
}
