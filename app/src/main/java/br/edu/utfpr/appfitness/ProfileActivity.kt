package br.edu.utfpr.appfitness

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import br.edu.utfpr.appfitness.databinding.ActivityProfileBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding

    companion object {
        private const val RC_REAUTHENTICATE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        carregarDadosDoUsuario()

        binding.btnEditar.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnExcluir.setOnClickListener { excluirConta() }
    }

    private fun carregarDadosDoUsuario() {
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            val userId = user.uid

            FirebaseFirestore.getInstance().collection("Pessoa").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        binding.tvUsername.text = document.getString("nome") ?: "Nome não encontrado"
                        binding.tvEmail.text = user.email ?: "Email não encontrado"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileActivity", "Erro ao carregar dados do usuário", e)
                    Toast.makeText(this, "Erro ao buscar dados do usuário.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun excluirConta() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = user.uid

        FirebaseFirestore.getInstance().collection("Pessoa")
            .document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.contains("loginMethod")) {
                    val loginMethod = document.getString("loginMethod")

                    when (loginMethod) {
                        "email_password" -> solicitarSenhaParaExcluirConta()
                        "google" -> excluirContaGoogle()
                        else -> Toast.makeText(this, "Método de login desconhecido.", Toast.LENGTH_SHORT).show()
                    }
                }
                else Toast.makeText(this, "Erro ao buscar dados do usuário.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar dados do usuário.", Toast.LENGTH_SHORT).show()
                Log.e("ProfileActivity", "Erro ao buscar dados do Firestore", e)
            }
    }

    private fun excluirContaGoogle() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.your_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_REAUTHENTICATE)
    }

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
                if (authTask.isSuccessful) {
                    val userId = user.uid
                    excluirSubcolecao(userId, "Atividade") {
                        FirebaseFirestore.getInstance().collection("Pessoa")
                            .document(userId)
                            .delete()
                            .addOnSuccessListener {
                                // Após excluir os dados do Firestore, exclui a conta do Firebase Authentication
                                user.delete().addOnCompleteListener { deleteTask ->
                                    if (deleteTask.isSuccessful) {
                                        Toast.makeText(this, "Conta excluída com sucesso.", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    }
                                    else Toast.makeText(this, "Erro ao excluir conta.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Erro ao excluir dados do usuário.", Toast.LENGTH_SHORT).show()
                                Log.e("ProfileActivity", "Erro ao excluir dados do Firestore", e)
                            }
                    }
                }
                else Toast.makeText(this, "Erro ao reautenticar.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.e("ProfileActivity", "Erro ao reautenticar com Google", e)
            Toast.makeText(this, "Erro ao reautenticar com Google.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun solicitarSenhaParaExcluirConta() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user?.email == null) return

        val input = EditText(this)
        input.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT

        AlertDialog.Builder(this)
            .setTitle("Confirme sua Senha")
            .setMessage("Digite sua senha para excluir a conta.")
            .setView(input)
            .setPositiveButton("Confirmar") { _, _ ->
                val senha = input.text.toString()
                if (senha.isNotEmpty()) reautenticarEExcluir(user.email!!, senha)
                else Toast.makeText(this, "Senha não pode estar vazia.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun reautenticarEExcluir(email: String, senha: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val credential = EmailAuthProvider.getCredential(email, senha)

        user?.reauthenticate(credential)?.addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                val userId = user.uid

                excluirSubcolecao(userId, "Atividade") {
                    // Exclui os dados do Firestore
                    FirebaseFirestore.getInstance().collection("Pessoa")
                        .document(userId)
                        .delete()
                        .addOnSuccessListener {
                            // Após excluir os dados do Firestore, exclui a conta do Firebase Authentication
                            user.delete().addOnCompleteListener { deleteTask ->
                                if (deleteTask.isSuccessful) {
                                    Toast.makeText(this, "Conta excluída com sucesso.", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }
                                else Toast.makeText(this, "Erro ao excluir conta.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Erro ao excluir dados do usuário.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            else Toast.makeText(this, "Senha incorreta.", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this, "Erro ao excluir atividades do usuário.", Toast.LENGTH_SHORT).show()
                        Log.e("ProfileActivity", "Erro ao excluir atividades do Firestore", e)
                        onComplete()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Erro ao buscar subcoleção $subcolecao", e)
            }
    }
}