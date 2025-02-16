package br.edu.utfpr.appfitness

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

class TrainingActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityTrainingBinding
    private var isRunning = false
    private var startTime = 0L
    private val handler = android.os.Handler(Looper.getMainLooper())

    private lateinit var sensorManager: SensorManager
    private var accelerometerValues = mutableListOf<Float>()
    private var gyroscopeValues = mutableListOf<Float>()
    private var heartRateValues = mutableListOf<Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_training)

        binding = ActivityTrainingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        registerSensor(Sensor.TYPE_ACCELEROMETER)
        registerSensor(Sensor.TYPE_GYROSCOPE)
        registerSensor(Sensor.TYPE_HEART_RATE)

        binding.btnStartStop.setOnClickListener {
            if (isRunning)  stopTraining()
            else startTraining()
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
                Sensor.TYPE_GYROSCOPE -> addSensorData(gyroscopeValues, it.values)
                Sensor.TYPE_HEART_RATE -> addSensorData(heartRateValues, it.values)
            }
        }
    }

    private fun addSensorData(list: MutableList<Float>, values: FloatArray) {
        //os sensores serão anotados em uma lista, cada item será adicionado após
        //realizar a média de 300 leituras
        val avg = values.average().toFloat()
        list.add(avg)
        if (list.size > 300) list.removeAt(0)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    private fun startTraining() {
        isRunning = true
        startTime = SystemClock.elapsedRealtime()
        binding.btnStartStop.text = getString(R.string.parar_exercicio)
        handler.post(timerRunnable)
    }

    private fun stopTraining() {
        isRunning = false
        binding.btnStartStop.text = getString(R.string.iniciar_exercicio)
        handler.removeCallbacks(timerRunnable)

        val treinoCategoria = calcularPontuacao()
        Toast.makeText(this, "Treino finalizado: $treinoCategoria", Toast.LENGTH_LONG).show()
    }

    private val timerRunnable = object: Runnable {
        override fun run() {
            val elapsedMillis = SystemClock.elapsedRealtime() - startTime
            val seconds = (elapsedMillis / 1000).toInt()
            val minutes = seconds / 60
            val hours = minutes / 60
            binding.tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
            handler.postDelayed(this, 1000)
        }
    }

    private fun calcularPontuacao(): String {
        val accAvg = accelerometerValues.average().toFloat()
        val gyroAvg = gyroscopeValues.average().toFloat()
        val heartAvg = heartRateValues.average().toFloat()

        //pontuação varia entre 0 a 100 de acordo com a média das listas
        val score = ((accAvg + gyroAvg + heartAvg) / 3 * 100).coerceIn(0f, 100f)

        return when {
            score < 30 -> "Treino Casual"
            score < 70 -> "Treino Moderado"
            else -> "Treino Intenso"
        }
    }

    private fun salvar() {
        val treino = TrainingSession(
            userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            duracao = binding.tvTimer.text.toString(),
            intensidade = calcularPontuacao()
        )

        Firebase.firestore.collection("Atividade")
            .add(treino)
            .addOnSuccessListener { Log.d("Training", "Treino salvo!") }
            .addOnFailureListener { e -> Log.e("Training", "Erro ao salvar", e) }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}