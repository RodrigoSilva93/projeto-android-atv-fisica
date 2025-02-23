package br.edu.utfpr.appfitness.fragment

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import br.edu.utfpr.appfitness.R
import br.edu.utfpr.appfitness.data.TrainingSession
import br.edu.utfpr.appfitness.databinding.FragmentTrainingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale
import kotlin.math.log10

class TrainingFragment : Fragment(), SensorEventListener {
    private var _binding: FragmentTrainingBinding? = null
    private val binding get() = _binding!!
    private lateinit var navController: NavController

    private var isRunning = false
    private var startTime = 0L
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var sensorManager: SensorManager
    private var accelerometerValues = mutableListOf<Float>()
    private var heartRateValues = mutableListOf<Float>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainingBinding.inflate(inflater, container, false)
        navController = Navigation.findNavController(requireActivity(), R.id.fragment_container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        registerSensor(Sensor.TYPE_ACCELEROMETER)
        registerSensor(Sensor.TYPE_HEART_RATE)

        binding.btnStartStop.setOnClickListener {
            if (isRunning) pararTreino()
            else iniciarTreino()
        }
    }

    private fun registerSensor(sensorType: Int) {
        sensorManager.getDefaultSensor(sensorType)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> addSensorData(accelerometerValues, it.values)
                Sensor.TYPE_HEART_RATE -> addSensorData(heartRateValues, it.values)
            }
        }
    }

    private fun addSensorData(list: MutableList<Float>, values: FloatArray) {
        val avg = values.average().toFloat()
        list.add(avg)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    private fun iniciarTreino() {
        isRunning = true
        startTime = SystemClock.elapsedRealtime()
        binding.btnStartStop.text = getString(R.string.parar_exercicio)
        handler.post(timerRunnable)
    }

    private fun pararTreino() {
        isRunning = false
        binding.btnStartStop.text = getString(R.string.iniciar_exercicio)
        handler.removeCallbacks(timerRunnable)

        Toast.makeText(requireContext(), "Treino finalizado", Toast.LENGTH_LONG).show()
        salvar()
    }

    private val timerRunnable = object: Runnable {
        override fun run() {
            val elapsedMillis = SystemClock.elapsedRealtime() - startTime
            val seconds = (elapsedMillis / 1000).toInt()
            val minutes = seconds / 60
            val hours = minutes / 60
            binding.tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
            handler.postDelayed(this, 1000)
        }
    }

    private fun calcularPontuacaoNumerica(): Float {
        val maxReadings = maxOf(accelerometerValues.size, heartRateValues.size)

        val accAvgList = if (accelerometerValues.size == maxReadings) {
            accelerometerValues.chunked(1000).map { it.average().toFloat() }
        } else {
            accelerometerValues.chunked(500).map { it.average().toFloat() }
        }

        val heartAvgList = if (heartRateValues.size == maxReadings) {
            heartRateValues.chunked(1000).map { it.average().toFloat() }
        } else {
            heartRateValues.chunked(500).map { it.average().toFloat() }
        }

        val accSum = if (accAvgList.sum() < 1) 0f else accAvgList.sum()
        val heartSum = if (heartAvgList.sum() < 1) 0f else heartAvgList.sum()

        val numerator = accSum + heartSum
        val denominator = log10(accSum + heartSum)

        return (numerator / denominator)
    }

    private fun categorizarTreino(score: Float): String {
        return when {
            score < 30 -> "Treino Casual"
            score < 70 -> "Treino Moderado"
            else -> "Treino Intenso"
        }
    }

    private fun salvar() {
        val selectedGroupName: String? = null //temporário, implementar grupos de usuários

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val userRef = Firebase.firestore.collection("Pessoa").document(userId.toString())

        userRef.get()
            .addOnSuccessListener() { document ->
                if (!isAdded || isDetached || activity == null) {
                    Log.w("Training", "Fragment não está ativo. Ignorando callback.")
                    return@addOnSuccessListener
                }

                val score = calcularPontuacaoNumerica()
                val treino = TrainingSession(
                    userId = userId.toString(),
                    userName = document.getString("nome") ?: "",
                    groupName = selectedGroupName ?: "",
                    duracao = binding.tvTimer.text.toString(),
                    intensidade = categorizarTreino(score),
                    timestamp = System.currentTimeMillis(),
                    pontuacao = score
                )

                Firebase.firestore.collection("Pessoa")
                    .document(userId.toString())
                    .collection("Atividade")
                    .add(treino)
                    .addOnSuccessListener {
                        Log.d("Training", "Treino salvo!")
                        navController.navigate(R.id.nav_home)
                    }
                    .addOnFailureListener { e -> Log.e("Training", "Erro ao salvar", e) }
            }
            .addOnFailureListener { e ->
                Log.e("TrainingFragment", "Erro ao recuperar nome usuário", e)
                Toast.makeText(requireContext(), "Erro ao carregar dados do usuário.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        sensorManager.unregisterListener(this)
    }
}