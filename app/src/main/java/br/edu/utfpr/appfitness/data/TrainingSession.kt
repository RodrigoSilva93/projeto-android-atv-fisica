package br.edu.utfpr.appfitness.data

data class TrainingSession(
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val duracao: String = "",
    val intensidade: String = "",
    val pontuacao: Double = 0.0
)
