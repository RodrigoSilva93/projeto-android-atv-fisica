package br.edu.utfpr.appfitness.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.utfpr.appfitness.R
import br.edu.utfpr.appfitness.data.TrainingSession

class FeedAdapter(private val postagens: List<TrainingSession>):
    RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    companion object {
        private const val MINUTE = 60L * 1000
        private const val HOUR = 60 * MINUTE
        private const val DAY = 24 * HOUR
        private const val MONTH = 30 * DAY
        private const val YEAR = 12 * MONTH
    }

    class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsuario: TextView = itemView.findViewById(R.id.tvUsuario)
        val tvGrupo: TextView = itemView.findViewById(R.id.tvGrupo)
        val tvData: TextView = itemView.findViewById(R.id.tvData)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuracao)
        val tvIntensity: TextView = itemView.findViewById(R.id.tvIntensidade)
        val tvPontuacao: TextView = itemView.findViewById(R.id.tvPontuacao)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_feed, parent, false)
        return FeedViewHolder(view)
    }

    override fun getItemCount(): Int { return postagens.size }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val model = postagens[position]

        if (model.groupName.isNotEmpty()) {
            holder.tvGrupo.text = holder.itemView.context.getString(R.string.session_group, model.userName, model.groupName)
            holder.tvGrupo.visibility = View.VISIBLE
            holder.tvUsuario.visibility = View.GONE
        } else {
            holder.tvUsuario.text = holder.itemView.context.getString(R.string.session_name, model.userName)
            holder.tvGrupo.visibility = View.GONE
            holder.tvUsuario.visibility = View.VISIBLE
        }

        holder.tvData.text = getRelativeTime(model.timestamp)
        holder.tvDuration.text = holder.itemView.context.getString(R.string.session_duration, model.duracao)
        holder.tvIntensity.text = holder.itemView.context.getString(R.string.session_type, model.intensidade)
        holder.tvPontuacao.text = holder.itemView.context.getString(R.string.session_score, String.format("%.2f", model.pontuacao))
    }

    private fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < MINUTE -> "Há alguns segundos"
            diff < HOUR -> "Há ${diff / MINUTE} minutos"
            diff < DAY -> "Há ${diff / HOUR} horas"
            diff < MONTH -> "Há ${diff / DAY} dias"
            diff < YEAR -> "Há ${diff / MONTH} meses"
            else -> "Há ${diff / YEAR} anos atrás"
        }
    }
}