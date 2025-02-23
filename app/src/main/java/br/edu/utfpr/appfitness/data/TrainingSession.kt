package br.edu.utfpr.appfitness.data

data class TrainingSession(
    val userId: String = "",
    val userName: String = "",
    var groupName: String = "", //opcional
    val timestamp: Long = 0L,
    val duracao: String = "",
    val intensidade: String = "",
    val pontuacao: Float = 0F
)
