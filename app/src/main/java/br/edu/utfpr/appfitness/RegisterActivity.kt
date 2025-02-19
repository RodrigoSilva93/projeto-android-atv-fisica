package br.edu.utfpr.appfitness

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.utfpr.appfitness.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityRegisterBinding
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth

        binding.btnRegister.setOnClickListener {
            val nome = binding.textNome.text.toString().trim()
            val email = binding.textEmail.text.toString().trim()
            val password = binding.textSenha.text.toString().trim()

            if (nome.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val usuario = hashMapOf(
                            "uid" to (user?.uid ?: ""),
                            "nome" to nome,
                            "email" to email
                        )

                        db.collection("Pessoa").document(user!!.uid)
                            .set(usuario)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Usuário criado com sucesso.", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { 
                                Toast.makeText(this, "Erro ao criar usuário!", Toast.LENGTH_SHORT).show()
                            }
                    }
                    else Toast.makeText(this, "Erro no cadastro!", Toast.LENGTH_SHORT).show()

                }



        }
    }
}