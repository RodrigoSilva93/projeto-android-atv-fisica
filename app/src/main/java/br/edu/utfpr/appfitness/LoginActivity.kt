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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    private lateinit var btGoogleSignIn: SignInButton

    val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth
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
                    .addOnCompleteListener(this) {task ->
                        if (task.isSuccessful) {
                            Log.d("LOGIN", "signInWithCredential:success")
                            val user = auth.currentUser

                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Log.w("LOGIN", "signInWithCredential:failure", task.exception)
                        }
                    }
            }
            else -> {
                Log.d("LOGIN", "No ID token!")
            }
        }
    }
}