package br.edu.utfpr.appfitness.fragment.group

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.utfpr.appfitness.R
import br.edu.utfpr.appfitness.adapter.GroupAdapter
import br.edu.utfpr.appfitness.data.Group
import br.edu.utfpr.appfitness.databinding.FragmentGroupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GroupFragment : Fragment() {
    private var _binding: FragmentGroupBinding? = null
    private val binding get() = _binding!!
    private lateinit var navController: NavController

    private lateinit var adapter: GroupAdapter
    private val grupos = mutableListOf<Group>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupBinding.inflate(inflater, container, false)
        navController = Navigation.findNavController(requireActivity(), R.id.fragment_container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = GroupAdapter(grupos) { grupo ->
            navController.navigate(R.id.action_groupFragment_to_groupPostsFragment, bundleOf("groupId" to grupo.groupId))
        }
        binding.recyclerView.adapter = adapter

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                buscarGrupos(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                buscarGrupos(newText)
                return true
            }
        })

        binding.fabCreateGroup.setOnClickListener {
            navController.navigate(R.id.action_groupFragment_to_createGroupFragment)
        }

        buscarGrupos(null)
    }

    private fun buscarGrupos(query: String?) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        var firestoreQuery = FirebaseFirestore.getInstance()
            .collection("Grupo")
            .whereEqualTo("publico", true) // Apenas grupos públicos

        if (!query.isNullOrEmpty()) {
            firestoreQuery = firestoreQuery
                .whereGreaterThanOrEqualTo("nome", query)
                .whereLessThanOrEqualTo("nome", query + "\uf8ff")
        }

        firestoreQuery.get()
            .addOnSuccessListener { result ->
                grupos.clear()
                for (document in result) {
                    val grupo = document.toObject(Group::class.java)
                    grupo.groupId = document.id
                    grupos.add(grupo)
                }

                FirebaseFirestore.getInstance().collection("Grupo")
                    .whereArrayContains("membros", userId)
                    .get()
                    .addOnSuccessListener { resultMembros ->
                        for (document in resultMembros) {
                            val grupo = document.toObject(Group::class.java)
                            grupo.groupId = document.id
                            if (!grupos.contains(grupo)) grupos.add(grupo)
                        }
                        adapter.notifyDataSetChanged()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("GroupFragment", "Erro ao buscar grupos", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}