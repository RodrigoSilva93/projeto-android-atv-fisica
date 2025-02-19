package br.edu.utfpr.appfitness

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import br.edu.utfpr.appfitness.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.SignInButton
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
    private lateinit var binding: ActivityLoginBinding
    private lateinit var btGoogleSignIn: SignInButton

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        btGoogleSignIn = findViewById(R.id.btnGoogleSignIn)

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

        binding.textRegistro.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()

//        var currentUser = auth.currentUser
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
                                "email" to (user?.email ?: "")
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