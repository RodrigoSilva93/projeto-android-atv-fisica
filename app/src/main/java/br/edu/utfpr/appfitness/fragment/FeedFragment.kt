package br.edu.utfpr.appfitness.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.utfpr.appfitness.ProfileActivity
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

    companion object {
        private const val REQUEST_CODE_TRAINING = 1001
    }

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
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<TrainingSession>()
            .setQuery(query, TrainingSession::class.java)
            .build()

        binding.btnStartTraining.setOnClickListener {
            val intent = Intent(requireContext(), TrainingActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_TRAINING)
        }

        adapter = FeedAdapter(options)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        binding.btnStartTraining.setOnClickListener {
            startActivity(Intent(requireContext(), TrainingActivity::class.java))
        }

        binding.btnPerfil.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_TRAINING && resultCode == Activity.RESULT_OK)
            atualizarLista()
    }

    private fun atualizarLista() {
        adapter.stopListening()
        adapter.startListening()
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onResume() {
        super.onResume()
        atualizarLista()
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