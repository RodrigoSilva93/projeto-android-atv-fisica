package br.edu.utfpr.appfitness

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.utfpr.appfitness.databinding.ActivityProfileBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            binding.tvUsername.text = user.displayName ?: "Nome não encontrado"
            binding.tvEmail.text = user.email ?: "Email não encontrado"
        }

        binding.btnEditar.setOnClickListener {
            //TODO - Implementar editar perfil
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnExcluir.setOnClickListener {
            excluirConta()
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
    }

    private fun excluirContaGoogle() {
        val user = FirebaseAuth.getInstance().currentUser

        user?.getIdToken(true)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val idToken = task.result?.token
                Log.d("PROFILE", "idToken: $idToken")
                if (idToken != null) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    user.reauthenticate(credential).addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            user.delete().addOnCompleteListener { deleteTask ->
                                if (deleteTask.isSuccessful) {
                                    Toast.makeText(
                                        this,
                                        "Conta excluída com sucesso.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(this, "Erro ao excluir conta.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "Erro ao reautenticar.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.w("PROFILE", "Erro ao recuperar token", task.exception)
                    Toast.makeText(this, "Erro ao recuperar token.", Toast.LENGTH_SHORT).show()
                }
            }
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
                if (senha.isNotEmpty()) {
                    reautenticarEExcluir(user.email!!, senha)
                } else {
                    Toast.makeText(this, "Senha não pode estar vazia.", Toast.LENGTH_SHORT).show()
                }
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
                FirebaseFirestore.getInstance().collection("Pessoa").document(userId)
                    .delete()
                    .addOnSuccessListener {
                        user.delete().addOnCompleteListener { deleteTask ->
                            if (deleteTask.isSuccessful) {
                                Toast.makeText(this, "Conta excluída com sucesso.", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "Erro ao excluir conta.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao excluir conta.", Toast.LENGTH_SHORT).show()
                    }

            } else {
                Toast.makeText(this, "Senha incorreta.", Toast.LENGTH_SHORT).show()
            }
        }
    }



}