package br.edu.utfpr.appfitness.fragment

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import br.edu.utfpr.appfitness.LoginActivity
import br.edu.utfpr.appfitness.R
import br.edu.utfpr.appfitness.data.Group
import br.edu.utfpr.appfitness.databinding.FragmentProfileBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore


class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var isEditing = false

    companion object {
        private const val RC_REAUTHENTICATE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        carregarDadosDoUsuario()

        binding.btnEditar.setOnClickListener { toggleEditMode(true) }
        binding.btnSalvar.setOnClickListener { salvarAlteracoes() }
        binding.btnExcluir.setOnClickListener { excluirConta() }
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(activity, LoginActivity::class.java))
        }
    }

    private fun carregarDadosDoUsuario() {
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            val userId = user.uid

            FirebaseFirestore.getInstance().collection("Pessoa").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        binding.etUsername.setText(document.getString("nome") ?: "Nome não encontrado")
                        binding.etEmail.setText(user.email ?: "Email não encontrado")

                        val dataNascimento = document.getString("dataNascimento") ?: ""
                        binding.etDataNascimento.setText(dataNascimento)

                        binding.spinnerEditGenero.setSelection(
                            when (document.getString("genero")) {
                                "Masculino" -> 0
                                "Feminino" -> 1
                                else -> 2
                            }
                        )
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileFragment", "Erro ao carregar dados do usuário", e)
                    Toast.makeText(requireContext(), "Erro ao buscar dados do usuário.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun toggleEditMode(enable: Boolean) {
        isEditing = enable
        binding.etUsername.isEnabled = enable
        binding.etEmail.isEnabled = enable
        binding.etDataNascimento.isEnabled = enable
        binding.spinnerEditGenero.isEnabled = enable
        binding.btnEditar.visibility = if (enable) View.GONE else View.VISIBLE
        binding.btnSalvar.visibility = if (enable) View.VISIBLE else View.GONE
    }

    private fun salvarAlteracoes() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userId = user.uid

        val nome = binding.etUsername.text.toString()
        val email = binding.etEmail.text.toString()
        val dataNascimento = binding.etDataNascimento.text.toString()
        val genero = binding.spinnerEditGenero.selectedItem.toString()

        val userData = hashMapOf(
            "nome" to nome,
            "email" to email,
            "dataNascimento" to dataNascimento,
            "genero" to genero
        )

        FirebaseFirestore.getInstance().collection("Pessoa")
            .document(userId)
            .update(userData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Perfil atualizado.", Toast.LENGTH_SHORT).show()
                toggleEditMode(false)
                atualizarAtividades(userId, nome)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao atualizar perfil.", Toast.LENGTH_SHORT).show()
                Log.e("ProfileFragment", "Erro ao atualizar perfil", e)
            }
    }

    private fun atualizarAtividades(userId: String, nome: String) {
        FirebaseFirestore.getInstance().collection("Pessoa")
            .document(userId)
            .collection("Atividade")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = FirebaseFirestore.getInstance().batch()
                for (document in querySnapshot.documents) {
                    val atividadeRef = FirebaseFirestore.getInstance()
                        .collection("Pessoa")
                        .document(userId)
                        .collection("Atividade")
                        .document(document.id)
                    batch.update(atividadeRef, "userName", nome)
                }

                batch.commit()
                    .addOnSuccessListener {
                        Log.d("ProfileFragment", "Atividades atualizadas com sucesso.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileFragment", "Erro ao atualizar atividades", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Erro ao buscar atividades", e)
            }

    }

    private fun excluirConta() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = user.uid

        FirebaseFirestore.getInstance()
            .collection("Pessoa")
            .document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.contains("loginMethod")) {
                    val loginMethod = document.getString("loginMethod")

                    when (loginMethod) {
                        "email_password" -> solicitarSenhaParaExcluirConta()
                        "google" -> excluirContaGoogle()
                        else -> Toast.makeText(requireContext(), "Método de login desconhecido.", Toast.LENGTH_SHORT).show()
                    }
                }
                else Toast.makeText(requireContext(), "Erro ao buscar dados do usuário.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e -> Log.e("ProfileFragment", "Erro ao buscar dados do Firestore", e) }
    }

    private fun excluirContaGoogle() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.your_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_REAUTHENTICATE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_REAUTHENTICATE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account?.idToken, null)

            val user = FirebaseAuth.getInstance().currentUser
            user?.reauthenticate(credential)?.addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) excluirUsuarioFirebase(user)
                else Toast.makeText(requireContext(), "Erro ao reautenticar.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.e("ProfileFragment", "Erro ao reautenticar com Google", e)
        }
    }

    private fun solicitarSenhaParaExcluirConta() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user?.email == null) return

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT

        AlertDialog.Builder(requireContext())
            .setTitle("Confirme sua Senha")
            .setMessage("Digite sua senha para excluir a conta.")
            .setView(input)
            .setPositiveButton("Confirmar") { _, _ ->
                val senha = input.text.toString()
                if (senha.isNotEmpty()) reautenticarEExcluir(user.email!!, senha)
                else Toast.makeText(requireContext(), "Senha não pode estar vazia.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun reautenticarEExcluir(email: String, senha: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val credential = EmailAuthProvider.getCredential(email, senha)

        user?.reauthenticate(credential)?.addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) excluirUsuarioFirebase(user)
            else Toast.makeText(requireContext(), "Senha incorreta.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun excluirSubcolecao(userId: String, subcolecao: String, onComplete: () -> Unit) {
        FirebaseFirestore.getInstance()
            .collection("Pessoa")
            .document(userId)
            .collection(subcolecao)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = FirebaseFirestore.getInstance().batch()
                for (document in querySnapshot.documents) {
                    batch.delete(document.reference)
                }

                batch.commit()
                    .addOnSuccessListener { onComplete() }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Erro ao excluir atividades do usuário.", Toast.LENGTH_SHORT).show()
                        Log.e("ProfileActivity", "Erro ao excluir atividades do Firestore", e)
                        onComplete()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Erro ao buscar subcoleção $subcolecao", e)
            }
    }

    private fun removerUsuarioDosGrupos(userId: String, onComplete: () -> Unit) {
        FirebaseFirestore.getInstance().collection("Grupo")
            .whereArrayContains("membros", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = FirebaseFirestore.getInstance().batch()
                for (document in querySnapshot.documents) {
                    val grupoId = document.id
                    val grupo = document.toObject(Group::class.java)
                    if (grupo != null) {
                        if (grupo.adminId == userId) batch.delete(document.reference)
                        else {
                            val novosMembros = grupo.membros.toMutableList().apply { remove(userId) }
                            batch.update(document.reference, "membros", novosMembros)
                        }
                    }
                }
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("ProfileFragment", "Usuário removido dos grupos com sucesso.")
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileFragment", "Erro ao remover usuário dos grupos", e)
                        onComplete()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Erro ao buscar grupos do usuário", e)
                onComplete()
            }

    }

    private fun excluirUsuarioFirebase(user: FirebaseUser?) {
        val userId = user?.uid ?: return

        removerUsuarioDosGrupos(userId) {
            excluirSubcolecao(userId, "Atividade") {
                // Exclui os dados do Firestore
                FirebaseFirestore.getInstance().collection("Pessoa")
                    .document(userId).delete()
                    .addOnSuccessListener {
                        // Exclui a conta do Firebase Authentication
                        user.delete().addOnCompleteListener { deleteTask ->
                            if (deleteTask.isSuccessful) {
                                Toast.makeText(
                                    requireContext(),
                                    "Conta excluída com sucesso.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                startActivity(Intent(requireContext(), LoginActivity::class.java))
                            } else Toast.makeText(
                                requireContext(),
                                "Erro ao excluir conta.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(
                            "ProfileFragment",
                            "Erro ao excluir dados do Firestore",
                            e
                        )
                    }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}