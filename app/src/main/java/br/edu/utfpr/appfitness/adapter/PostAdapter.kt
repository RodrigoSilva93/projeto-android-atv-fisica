package br.edu.utfpr.appfitness.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.utfpr.appfitness.R
import br.edu.utfpr.appfitness.data.TrainingSession

class PostAdapter(
    private val postagens: List<TrainingSession>
): RecyclerView.Adapter<PostAdapter.PostViewHolder>() {
    companion object {
        private const val MINUTE = 60L * 1000
        private const val HOUR = 60 * MINUTE
        private const val DAY = 24 * HOUR
        private const val MONTH = 30 * DAY
        private const val YEAR = 12 * MONTH
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvIntensity: TextView = itemView.findViewById(R.id.tvIntensity)
        private val tvScore: TextView = itemView.findViewById(R.id.tvScore)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(postagem: TrainingSession) {
            tvUserName.text = itemView.context.getString(R.string.session_group_share, postagem.userName)
            tvDuration.text = itemView.context.getString(R.string.session_duration, postagem.duracao)
            tvIntensity.text = itemView.context.getString(R.string.session_type, postagem.intensidade)
            tvScore.text = itemView.context.getString(R.string.session_score, String.format("%.2f", postagem.pontuacao))
            tvTimestamp.text = getRelativeTime(postagem.timestamp)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(postagens[position])
    }

    override fun getItemCount(): Int { return postagens.size }
}