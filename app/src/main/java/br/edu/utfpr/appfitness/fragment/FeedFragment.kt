package br.edu.utfpr.appfitness.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.utfpr.appfitness.adapter.FeedAdapter
import br.edu.utfpr.appfitness.data.TrainingSession
import br.edu.utfpr.appfitness.databinding.FragmentFeedBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FeedFragment: Fragment() {
    private lateinit var adapter: FeedAdapter
    private val postagens = mutableListOf<TrainingSession>()

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = FirebaseAuth.getInstance().uid!!

        adapter = FeedAdapter(postagens)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        buscarPostagens(userId)

//        val query = FirebaseFirestore.getInstance()
//            .collection("Pessoa")
//            .document(FirebaseAuth.getInstance().uid!!)
//            .collection("Atividade")
//            .orderBy("timestamp", Query.Direction.DESCENDING)
//
//        val options = FirestoreRecyclerOptions.Builder<TrainingSession>()
//            .setQuery(query, TrainingSession::class.java)
//            .build()
//
//        adapter = FeedAdapter(options)
//        binding.recyclerView.layoutManager = LinearLayoutManager(context)
//        binding.recyclerView.adapter = adapter
    }

    private fun buscarPostagens(userId: String) {
        postagens.clear()

        FirebaseFirestore.getInstance()
            .collection("Pessoa")
            .document(userId)
            .collection("Atividade")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val postagem = document.toObject(TrainingSession::class.java)
                    postagens.add(postagem)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("FeedFragment", "Erro ao buscar postagens do usuário", e)
            }

        FirebaseFirestore.getInstance()
            .collection("Pessoa")
            .document(userId)
            .collection("Grupos")
            .get()
            .addOnSuccessListener { grupos ->
                for (grupo in grupos) {
                    val groupId = grupo.id

                    // Busca as postagens do grupo
                    FirebaseFirestore.getInstance()
                        .collection("Grupo")
                        .document(groupId)
                        .collection("Atividade")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val postagem = document.toObject(TrainingSession::class.java)
                                postagens.add(postagem)
                            }
                            adapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { e ->
                            Log.e("FeedFragment", "Erro ao buscar postagens do grupo $groupId", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FeedFragment", "Erro ao buscar grupos", e)
            }
    }


//    private fun atualizarLista() {
//        adapter.stopListening()
//        adapter.notifyDataSetChanged()
//        adapter.startListening()
//    }

//    override fun onStart() {
//        super.onStart()
//        if (::adapter.isInitialized) adapter.startListening()
//    }

//    override fun onResume() {
//        super.onResume()
//        atualizarLista()
//    }

//    override fun onStop() {
//        super.onStop()
//        if (::adapter.isInitialized) adapter.stopListening()
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}