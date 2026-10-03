package com.appmaker.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class DhikrAdapter(
    private val azkarList: MutableList<Dhikr>,
    private var selectedDhikrId: Long,
    private val onDhikrSelected: (Dhikr) -> Unit,
    private val onQuickAddClicked: (Dhikr) -> Unit,
    private val onDeleteClicked: (Dhikr) -> Unit
) : RecyclerView.Adapter<DhikrAdapter.DhikrViewHolder>() {

    fun updateSelectedId(id: Long) {
        selectedDhikrId = id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DhikrViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dhikr, parent, false)
        return DhikrViewHolder(view)
    }

    override fun onBindViewHolder(holder: DhikrViewHolder, position: Int) {
        val dhikr = azkarList[position]
        holder.bind(dhikr, dhikr.id == selectedDhikrId)
    }

    override fun getItemCount(): Int = azkarList.size

    inner class DhikrViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.cardItemDhikr)
        private val tvCategory: TextView = itemView.findViewById(R.id.itemCategory)
        private val tvProgress: TextView = itemView.findViewById(R.id.itemCounterProgress)
        private val tvText: TextView = itemView.findViewById(R.id.itemText)
        private val btnQuickAdd: MaterialButton = itemView.findViewById(R.id.itemBtnQuickAdd)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.itemBtnDelete)

        fun bind(dhikr: Dhikr, isSelected: Boolean) {
            tvCategory.text = dhikr.category
            tvProgress.text = "${dhikr.count} / ${dhikr.target}"
            tvText.text = dhikr.text

            if (isSelected) {
                card.strokeColor = Color.parseColor("#10B981")
                card.strokeWidth = 4
                card.setCardBackgroundColor(Color.parseColor("#142E28"))
            } else {
                card.strokeColor = Color.parseColor("#1E293B")
                card.strokeWidth = 2
                card.setCardBackgroundColor(Color.parseColor("#16222F"))
            }

            card.setOnClickListener {
                onDhikrSelected(dhikr)
            }

            btnQuickAdd.setOnClickListener {
                onQuickAddClicked(dhikr)
            }

            btnDelete.setOnClickListener {
                onDeleteClicked(dhikr)
            }
        }
    }
}