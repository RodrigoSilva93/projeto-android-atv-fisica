package br.edu.utfpr.appfitness.fragment.group

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.utfpr.appfitness.R
import br.edu.utfpr.appfitness.adapter.GroupAdapter
import br.edu.utfpr.appfitness.adapter.GroupSelectionAdapter
import br.edu.utfpr.appfitness.data.Group
import br.edu.utfpr.appfitness.databinding.FragmentSelectGroupDialogBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SelectGroupDialogFragment : DialogFragment() {
    private var _binding: FragmentSelectGroupDialogBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: GroupSelectionAdapter
    private val grupos = mutableListOf<Group>()
    private val selectedGroups = mutableListOf<String>()

    var onGroupsSelected: ((List<String>) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectGroupDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = GroupSelectionAdapter(grupos) { grupo, isSelected ->
            if (isSelected) selectedGroups.add(grupo.groupId)
            else selectedGroups.remove(grupo.groupId)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        carregarGruposDoUsuario()

        binding.btnConfirmar.setOnClickListener {
            onGroupsSelected?.invoke(selectedGroups.toList())
            dismiss()
        }
    }

    private fun carregarGruposDoUsuario() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("Grupo")
            .whereArrayContains("membros", userId)
            .get()
            .addOnSuccessListener { result ->
                grupos.clear()
                for (document in result) {
                    val grupo = document.toObject(Group::class.java)
                    grupo.groupId = document.id
                    grupos.add(grupo)
                    Log.d("SelectDialogFragment", "Grupo carregado: ${grupo.nome} (ID: ${grupo.groupId})")
                }

                if (grupos.isEmpty())
                    Log.d("SelectDialogFragment", "Nenhum grupo encontrado para o usuário.")

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("SelectGroupDialog", "Erro ao buscar grupos", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}