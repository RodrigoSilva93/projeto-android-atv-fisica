package br.edu.utfpr.appfitness.fragment.group

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.utfpr.appfitness.adapter.PostAdapter
import br.edu.utfpr.appfitness.data.Ranking
import br.edu.utfpr.appfitness.data.TrainingSession
import br.edu.utfpr.appfitness.databinding.FragmentGroupPostBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar

class GroupPostFragment : Fragment() {
    private var _binding: FragmentGroupPostBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PostAdapter
    private val postagens = mutableListOf<TrainingSession>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupId = arguments?.getString("groupId") ?: return

        FirebaseFirestore.getInstance().collection("Grupo")
            .document(groupId)
            .get()
            .addOnSuccessListener { document ->
                val groupName = document.getString("nome") ?: ""
                val groupDescription = document.getString("descricao") ?: ""

                binding.tvGroupName.text = groupName
                binding.tvGroupDescription.text = groupDescription
            }
            .addOnFailureListener { e ->
                Log.e("GroupPostFragment", "Erro ao carregar grupo", e)
            }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = PostAdapter(postagens)
        binding.recyclerView.adapter = adapter

        carregarDadosDoGrupo(groupId)
        carregarPostagensDoGrupo(groupId)
        configurarGrafico()
        carregarRankingDoGrupo(groupId)
    }

    private fun configurarGrafico() {
        val ranking = mutableListOf<Ranking>()
        val entries = ranking.mapIndexed { index, rank ->
            BarEntry(index.toFloat(), rank.pontuacao)
        }

        val dataSet = BarDataSet(entries, "Pontuação")
        val colors = if (isDarkTheme(requireContext())) {
            listOf(Color.CYAN, Color.MAGENTA, Color.YELLOW) // Tema escuro
        } else listOf(Color.BLUE, Color.GREEN, Color.RED) // Tema claro
        dataSet.colors = colors

        dataSet.valueTextColor = if (isDarkTheme(requireContext())) {
            Color.WHITE // Tema escuro
        } else Color.BLACK // Tema claro

        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        binding.barChart.data = barData

        binding.barChart.setFitBars(true)
        binding.barChart.description.isEnabled = false
        binding.barChart.legend.isEnabled = false
        binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(ranking.map { it.userName })
        binding.barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.barChart.xAxis.granularity = 1f
        binding.barChart.xAxis.setDrawGridLines(false)
        binding.barChart.axisLeft.setDrawGridLines(false)
        binding.barChart.axisRight.isEnabled = false

        val textColor = if (isDarkTheme(requireContext())) Color.WHITE else Color.BLACK
        binding.barChart.xAxis.textColor = textColor
        binding.barChart.axisLeft.textColor = textColor

        binding.barChart.animateY(1000)
        binding.barChart.invalidate() // Atualiza o gráfico
    }

    private fun isDarkTheme(context: Context): Boolean {
        val flags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return flags == Configuration.UI_MODE_NIGHT_YES
    }

    private fun carregarRankingDoGrupo(groupId: String) {

        FirebaseFirestore.getInstance().collection("Grupo")
            .document(groupId)
            .collection("Atividade")
            .get()
            .addOnSuccessListener { result ->
                val ranking = mutableMapOf<String, Float>()
                val userNames = mutableMapOf<String, String>()

                for (document in result) {
                    val postagem = document.toObject(TrainingSession::class.java)
                    val userId = postagem.userId
                    val userName = postagem.userName
                    val pontuacao = postagem.pontuacao

                    if (ranking.containsKey(userId))
                        ranking[userId] = ranking[userId]!! + pontuacao // soma a pontuação existente
                    else ranking[userId] = pontuacao

                    userNames[userId] = userName
                }

                val rankingOrdenado = ranking.entries
                    .sortedByDescending { it.value }
                    .map { entry ->
                        val userId = entry.key
                        val userName = userNames[userId] ?: "Usuário Desconhecido"
                        Ranking(userName, entry.value)
                    }

                atualizarGrafico(rankingOrdenado)
            }
            .addOnFailureListener { e ->
                Log.e("GroupPostsFragment", "Erro ao carregar ranking", e)
            }
    }

    private fun atualizarGrafico(ranking: List<Ranking>) {
        val entries = ranking.mapIndexed { index, rank ->
            BarEntry(index.toFloat(), rank.pontuacao)
        }

        val dataSet = BarDataSet(entries, "Pontuação")
        dataSet.color = Color.BLUE
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        binding.barChart.data = barData

        binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(ranking.map { it.userName })
        binding.barChart.invalidate() // Atualiza o gráfico
    }

    private fun carregarDadosDoGrupo(groupId: String) {
        FirebaseFirestore.getInstance().collection("Grupo")
            .document(groupId)
            .get()
            .addOnSuccessListener { document ->
                binding.tvGroupName.text = document.getString("nome")
                binding.tvGroupDescription.text = document.getString("descricao")
            }
            .addOnFailureListener { e ->
                Log.e("GroupPostsFragment", "Erro ao carregar dados do grupo", e)
            }
    }

    private fun carregarPostagensDoGrupo(groupId: String) {
        FirebaseFirestore.getInstance().collection("Grupo")
            .document(groupId)
            .collection("Atividade")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                Log.d("GroupPostFragment", "Postagens carregadas: ${result.size()}, result: $result")
                postagens.clear()
                for (document in result) {
                    val postagem = document.toObject(TrainingSession::class.java)
                    postagens.add(postagem)
                    Log.d("GroupPostFragment", "Postagem: ${postagem.userName}, ${postagem.duracao}, ${postagem.intensidade}")
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("GroupPostsFragment", "Erro ao carregar postagens", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}