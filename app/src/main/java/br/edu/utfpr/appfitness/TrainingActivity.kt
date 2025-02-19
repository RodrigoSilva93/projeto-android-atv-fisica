package br.edu.utfpr.appfitness

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import br.edu.utfpr.appfitness.data.TrainingSession
import br.edu.utfpr.appfitness.databinding.ActivityTrainingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale
import kotlin.math.log10

class TrainingActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityTrainingBinding
    private var isRunning = false
    private var startTime = 0L
    private val handler = android.os.Handler(Looper.getMainLooper())

    private lateinit var sensorManager: SensorManager
    private var accelerometerValues = mutableListOf<Float>()
    private var heartRateValues = mutableListOf<Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTrainingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        registerSensor(Sensor.TYPE_ACCELEROMETER)
        registerSensor(Sensor.TYPE_GYROSCOPE)
        registerSensor(Sensor.TYPE_HEART_RATE)

        binding.btnStartStop.setOnClickListener {
            if (isRunning)  pararTreino()
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

        Toast.makeText(this, "Treino finalizado", Toast.LENGTH_LONG).show()
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
        val score = calcularPontuacaoNumerica()
        val treino = TrainingSession(
            userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            duracao = binding.tvTimer.text.toString(),
            intensidade = categorizarTreino(score),
            timestamp = System.currentTimeMillis(),
            pontuacao = score
        )

        Firebase.firestore.collection("Pessoa")
            .document(FirebaseAuth.getInstance().currentUser?.uid ?: "")
            .collection("Atividade")
            .add(treino)
            .addOnSuccessListener {
                Log.d("Training", "Treino salvo!")
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e -> Log.e("Training", "Erro ao salvar", e) }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}