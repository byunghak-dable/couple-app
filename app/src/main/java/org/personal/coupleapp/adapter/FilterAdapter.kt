package org.personal.coupleapp.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.personal.coupleapp.R
import org.personal.coupleapp.data.FilterData

class FilterAdapter(private val filterList: ArrayList<FilterData>, private val itemClickListener: ItemClickListener) : RecyclerView.Adapter<FilterAdapter
.ViewHolder>
    () {


    class ViewHolder(itemView: View, private val itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val filterName: TextView = itemView.findViewById(R.id.filterNameTV)
        val filterImage: ImageView = itemView.findViewById(R.id.filterImageIV)

        init {

            filterImage.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            if (adapterPosition != RecyclerView.NO_POSITION) {

                itemClickListener.onItemClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_filter, parent, false)
        return ViewHolder(view, itemClickListener)
    }

    override fun getItemCount(): Int {

        return filterList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val filterData = filterList[position]
        holder.filterImage.setImageResource(filterData.imageResource)
        holder.filterName.text = filterData.filterName
    }

    public interface ItemClickListener {
        fun onItemClick(itemPosition: Int)
    }
}