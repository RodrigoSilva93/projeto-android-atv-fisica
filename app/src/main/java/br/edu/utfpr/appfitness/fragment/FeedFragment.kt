package br.edu.utfpr.appfitness.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.utfpr.appfitness.TrainingActivity
import br.edu.utfpr.appfitness.adapter.FeedAdapter
import br.edu.utfpr.appfitness.data.TrainingSession
import br.edu.utfpr.appfitness.databinding.FragmentFeedBinding
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FeedFragment: Fragment() {
    private lateinit var adapter: FeedAdapter

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

        val query = FirebaseFirestore.getInstance()
            .collection("Pessoa")
            .document(FirebaseAuth.getInstance().uid!!)
            .collection("Atividade")
            .orderBy("data", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<TrainingSession>()
            .setQuery(query, TrainingSession::class.java)
            .build()

        binding.btnStartTraining.setOnClickListener {
            val intent = Intent(requireContext(), TrainingActivity::class.java)
            startActivity(intent)
        }

        adapter = FeedAdapter(options)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        binding.btnStartTraining.setOnClickListener {
            val intent = Intent(requireContext(), TrainingActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}