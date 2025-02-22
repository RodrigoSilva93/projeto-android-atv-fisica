package br.edu.utfpr.appfitness

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.edu.utfpr.appfitness.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        carregarDadosUsuario()

        binding.btnEditSalvar.setOnClickListener { salvarAlteracoes() }
    }

    private fun carregarDadosUsuario() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val userId = user.uid

        FirebaseFirestore.getInstance().collection("Pessoa")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.textEditNome.setText(document.getString("nome") ?: "")
                    binding.textEditEmail.setText(user.email ?: "")
                    binding.textEditDataNascimento.setText(document.getString("dataNascimento") ?: "")
                    binding.spinnerEditGenero.setSelection(
                        when (document.getString("genero")) {
                            "Masculino" -> 0
                            "Feminino" -> 1
                            else -> 2
                        }
                    )
                }
            }
    }

    private fun salvarAlteracoes() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val userId = user.uid

        val nome = binding.textEditNome.text.toString()
        val email = binding.textEditEmail.text.toString()
        val dataNascimento = binding.textEditDataNascimento.text.toString()
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
                Toast.makeText(this, "Perfil atualizado.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao atualizar perfil.", Toast.LENGTH_SHORT).show()
                Log.e("EditProfileActivity", "Erro ao atualizar perfil", e)
            }
    }


}