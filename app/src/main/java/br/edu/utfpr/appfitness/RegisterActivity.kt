package br.edu.utfpr.appfitness

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    private lateinit var btnRegister: Button
    private lateinit var nomeEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var senhaEditText: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth

        btnRegister = findViewById(R.id.btnRegister)
        nomeEditText = findViewById(R.id.textNome)
        emailEditText = findViewById(R.id.textEmail)
        senhaEditText = findViewById(R.id.textSenha)


        btnRegister.setOnClickListener {
            val nome = nomeEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = senhaEditText.text.toString().trim()

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
                            "email" to email,
                            "loginMethod" to "email_password"
                        )

                        db.collection("Pessoa").document(user!!.uid)
                            .set(usuario)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Usuário criado com sucesso.", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Log.e("REGISTER", "Erro ao salvar usuário", e)
                                Toast.makeText(this, "Erro ao criar usuário!", Toast.LENGTH_SHORT).show()
                            }
                    }
                    else {
                        Log.e("REGISTER", "Erro no cadastro", task.exception)
                        Toast.makeText(this, "Erro no cadastro!", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}