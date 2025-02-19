package br.edu.utfpr.appfitness

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.SignInButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var auth: FirebaseAuth

    private lateinit var btGoogleSignIn: SignInButton
    private lateinit var emailEditText: TextInputEditText
    private lateinit var senhaEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var registrarButton: Button

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        btGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        emailEditText = findViewById(R.id.textEmail)
        senhaEditText = findViewById(R.id.textSenha)
        loginButton = findViewById(R.id.btnLogin)
        registrarButton = findViewById(R.id.btnRegistrar)

        oneTapClient = Identity.getSignInClient(this)

        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.your_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            ).build()

        btGoogleSignIn.setOnClickListener { tratarLogin() }

        registrarButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = senhaEditText.text.toString().trim()

            if (email.isEmpty()) {
                emailEditText.error = "Preencha o email"
                emailEditText.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                senhaEditText.error = "Preencha a senha"
                senhaEditText.requestFocus()
                return@setOnClickListener
            }

            loginUsuario(email, password)
        }
    }

    override fun onStart() {
        super.onStart()

//        var currentUser = auth.currentUser
    }

    private fun loginUsuario(email: String, senha: String) {
        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LOGIN", "Login bem-sucedido: ${auth.currentUser?.email}")
                    Toast.makeText(this, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Log.e("LOGIN", "Erro ao fazer login", task.exception)
                    Toast.makeText(this, "Erro ao fazer login. Verifique seu e-mail e senha.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun tratarLogin() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener (this) { result ->
                startIntentSenderForResult(
                    result.pendingIntent.intentSender,
                    2, null, 0,
                    0,0, null
                )
            }
            .addOnFailureListener(this) { e ->
                e.localizedMessage?.let { Log.d("LOGIN", it) }
            }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        val googleCredential = oneTapClient.getSignInCredentialFromIntent(data)
        val idToken = googleCredential.googleIdToken

        when {
            idToken != null -> {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val usuario = hashMapOf(
                                "uid" to (user?.uid ?: ""),
                                "nome" to (user?.displayName ?: ""),
                                "email" to (user?.email ?: ""),
                                "loginMethod" to "google"
                            )

                            db.collection("Pessoa").document(user!!.uid)
                                .set(usuario, SetOptions.merge())
                                .addOnSuccessListener {
                                    Log.d("LOGIN", "Usuário criado com sucesso.")
                                }
                                .addOnFailureListener { e ->
                                    Log.w("LOGIN", "Erro ao criar usuário", e)
                                }

                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        else Log.w("LOGIN", "signInWithCredential:failure", task.exception)
                    }
            }
            else -> {
                Log.d("LOGIN", "No ID token!")
            }
        }
    }
}