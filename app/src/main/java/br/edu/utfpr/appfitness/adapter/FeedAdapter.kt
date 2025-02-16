package br.edu.utfpr.appfitness.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.utfpr.appfitness.R
import br.edu.utfpr.appfitness.data.TrainingSession
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class FeedAdapter(options: FirestoreRecyclerOptions<TrainingSession>):
    FirestoreRecyclerAdapter<TrainingSession, FeedAdapter.FeedViewHolder>(options) {

    class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvData)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuracao)
        val tvIntensity: TextView = itemView.findViewById(R.id.tvIntensidade)
        val tvPontuacao: TextView = itemView.findViewById(R.id.tvPontuacao)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_feed, parent, false)
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int, model: TrainingSession) {
        holder.tvDate.text = model.timestamp.toString()
        holder.tvDuration.text = "Duração: ${model.duracao}"
        holder.tvIntensity.text = "Intensidade: ${model.intensidade}"
        holder.tvPontuacao.text = "Pontuação: ${model.pontuacao}"
    }
}