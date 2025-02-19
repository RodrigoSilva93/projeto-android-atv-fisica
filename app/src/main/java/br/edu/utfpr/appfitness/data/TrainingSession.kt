package br.edu.utfpr.appfitness.data

data class TrainingSession(
    val userId: String = "",
    val timestamp: Long = 0L,
    val duracao: String = "",
    val intensidade: String = "",
    val pontuacao: Float = 0F
)
