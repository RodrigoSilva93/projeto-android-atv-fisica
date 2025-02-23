package br.edu.utfpr.appfitness.fragment.group

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import br.edu.utfpr.appfitness.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import br.edu.utfpr.appfitness.databinding.FragmentCreateGroupBinding

class CreateGroupFragment : Fragment() {
    private var _binding: FragmentCreateGroupBinding? = null
    private val binding get() = _binding!!
    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateGroupBinding.inflate(inflater, container, false)
        navController = Navigation.findNavController(requireActivity(), R.id.fragment_container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCreateGroup.setOnClickListener { criarGrupo() }
    }

    private fun criarGrupo() {
        val nome = binding.etGroupName.text.toString()
        val descricao = binding.etGroupDescription.text.toString()
        val publico = binding.switchPublic.isChecked
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val grupo = hashMapOf(
            "nome" to nome,
            "descricao" to descricao,
            "publico" to publico,
            "adminId" to userId,
            "membros" to listOf(userId) // Adiciona o criador como membro
        )

        FirebaseFirestore.getInstance().collection("Grupo")
            .add(grupo)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Grupo criado com sucesso!", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
            .addOnFailureListener { e ->
                Log.e("CreateGroupFragment", "Erro ao criar grupo", e)
                Toast.makeText(requireContext(), "Erro ao criar grupo.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}