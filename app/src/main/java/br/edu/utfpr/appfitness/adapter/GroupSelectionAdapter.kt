package br.edu.utfpr.appfitness.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.utfpr.appfitness.R
import br.edu.utfpr.appfitness.data.Group

class GroupSelectionAdapter(
    private val grupos: List<Group>,
    private val onGroupSelected: (Group, Boolean) -> Unit
): RecyclerView.Adapter<GroupSelectionAdapter.GroupViewHolder>() {
    private val selectedGroups = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group_selection, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val grupo = grupos[position]
        holder.bind(grupo, selectedGroups.contains(grupo.groupId))
        holder.itemView.setOnClickListener {
            toggleSelection(grupo)
        }
    }

    override fun getItemCount(): Int = grupos.size

    private fun toggleSelection(grupo: Group) {
        if (selectedGroups.contains(grupo.groupId)) {
            selectedGroups.remove(grupo.groupId)
        } else {
            selectedGroups.add(grupo.groupId)
        }
        onGroupSelected(grupo, selectedGroups.contains(grupo.groupId))
        notifyDataSetChanged() // Atualiza a UI
    }

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(grupo: Group, isSelected: Boolean) {
            itemView.findViewById<TextView>(R.id.tvGroupName).text = grupo.nome

            if (isSelected) itemView.setBackgroundColor(Color.LTGRAY) // Cor de fundo para selecionado
            else itemView.setBackgroundColor(Color.TRANSPARENT) // Cor de fundo padrão
        }
    }

}