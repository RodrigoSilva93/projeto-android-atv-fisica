package br.edu.utfpr.appfitness.data

data class Group(
    var groupId: String = "",
    val nome: String = "",
    val descricao: String = "",
    val publico: Boolean = true,
    val adminId: String = "",
    val membros: List<String> = emptyList()
)
