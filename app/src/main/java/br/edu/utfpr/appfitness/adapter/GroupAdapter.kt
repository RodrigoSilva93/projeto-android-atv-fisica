package br.edu.utfpr.appfitness.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import br.edu.utfpr.appfitness.R
import br.edu.utfpr.appfitness.data.Group
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class GroupAdapter(
    private val grupos: List<Group>,
    private val onGroupSelected: (Group) -> Unit
): RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {
    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvGroupName: TextView = itemView.findViewById(R.id.tvGroupName)
        private val tvGroupDescription: TextView = itemView.findViewById(R.id.tvGroupDescription)
        private val btnJoinGroup: Button = itemView.findViewById(R.id.btnJoinGroup)

        fun bind(group: Group) {
            tvGroupName.text = group.nome
            tvGroupDescription.text = group.descricao

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val isMember = group.membros.contains(userId)

            if (isMember) btnJoinGroup.visibility = View.GONE
            else {
                btnJoinGroup.visibility = View.VISIBLE
                btnJoinGroup.setOnClickListener { entrarNoGrupo(group.groupId) }
            }

            itemView.setOnClickListener { onGroupSelected(group) }
        }

        private fun entrarNoGrupo(groupId: String) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

            FirebaseFirestore.getInstance().collection("Grupo")
                .document(groupId)
                .update("membros", FieldValue.arrayUnion(userId))
                .addOnSuccessListener {
                    Toast.makeText(itemView.context, "Você entrou no grupo!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("GroupAdapter", "Erro ao entrar no grupo", e)
                    Toast.makeText(itemView.context, "Erro ao entrar no grupo.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val grupo = grupos[position]
        holder.bind(grupo)
        holder.itemView.setOnClickListener { onGroupSelected(grupo) }
    }

    override fun getItemCount(): Int = grupos.size

}