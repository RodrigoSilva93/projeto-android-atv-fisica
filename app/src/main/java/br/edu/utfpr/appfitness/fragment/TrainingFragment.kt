package br.edu.utfpr.appfitness.fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import br.edu.utfpr.appfitness.R
import br.edu.utfpr.appfitness.data.TrainingSession
import br.edu.utfpr.appfitness.databinding.FragmentTrainingBinding
import br.edu.utfpr.appfitness.fragment.group.SelectGroupDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

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
    private var barometerValues = mutableListOf<Float>()
    private var locationValues = mutableListOf<Location>()

    private lateinit var locationManager: LocationManager
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            locationValues.add(location)
            Log.d("LocationListener", "Nova localização recebida: ${location.latitude}, ${location.longitude}")
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.d("LocationListener", "Status do provedor $provider alterado: $status")
        }
        override fun onProviderEnabled(provider: String) {
            Log.d("LocationListener", "Provedor $provider habilitado")
        }
        override fun onProviderDisabled(provider: String) {
            Log.d("LocationListener", "Provedor $provider desabilitado")
        }
    }

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

        // Mantém a tela ligada
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        registerSensor(Sensor.TYPE_ACCELEROMETER)
        registerSensor(Sensor.TYPE_HEART_RATE)
        registerSensor(Sensor.TYPE_PRESSURE)

        checkAndRequestPermissions()

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

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        Log.d("Permissions", "Permissões: $permissionsToRequest")

        if (permissionsToRequest.isNotEmpty())
            ActivityCompat.requestPermissions(requireActivity(), permissionsToRequest.toTypedArray(), 2)
        else startSensors()
    }

    //Verifica se os sensores estão disponíveis
    private fun startSensors() {
        Log.d("Permissions", "Iniciando sensores")
        val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        if (heartRateSensor != null)
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
        else Log.d("TrainingFragment", "Sensor de batimento cardíaco não disponível")

        val barometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        if (barometerSensor != null)
            sensorManager.registerListener(this, barometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
        else Log.d("TrainingFragment", "Sensor de barômetro não disponível")

        requestLocationUpdates()
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    100,
                    0.5f,
                    locationListener
                )
                Log.d("LocationListener", "Solicitando atualizações de localização via GPS")
            } else {
                Log.e("LocationListener", "GPS_PROVIDER não está habilitado")
                Toast.makeText(requireContext(), "GPS não está habilitado. Ative o GPS e tente novamente.", Toast.LENGTH_SHORT).show()
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    100,
                    0.5f,
                    locationListener
                )
                Log.d("LocationListener", "Solicitando atualizações de localização via rede")
            } else Log.e("LocationListener", "NETWORK_PROVIDER não está habilitado")

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2) {
            val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allPermissionsGranted) startSensors()
            else Toast.makeText(requireContext(), "Permissões necessárias não foram concedidas.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> addSensorData(accelerometerValues, it.values)
                Sensor.TYPE_HEART_RATE -> addSensorData(heartRateValues, it.values)
                Sensor.TYPE_PRESSURE -> addSensorData(barometerValues, it.values)
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

        AlertDialog.Builder(requireContext())
            .setTitle("Compartilhar treino")
            .setMessage("Deseja compartilhar este treino em um grupo?")
            .setPositiveButton("Sim") { _, _ ->
                val dialog = SelectGroupDialogFragment()

                dialog.onGroupsSelected = { selectedGroups ->
                    salvar(true, selectedGroups)
                }
                dialog.show(parentFragmentManager, "SelectGroupDialog")
            }
            .setNegativeButton("Não") { _, _ -> salvar(false) }
            .show()
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
        // Verifica a disponibilidade e ajusta os pesos
        val accelerometerWeight = if (accelerometerValues.isNotEmpty()) 0.30f else 0f
        val heartRateWeight = if (heartRateValues.isNotEmpty()) 0.65f else 0f
        val barometerWeight = if (barometerValues.isNotEmpty()) 0.25f else 0f
        val gpsWeight = if (locationValues.isNotEmpty()) 0.35f else 0f

        // Normaliza os dados dos sensores
        val accScore = if (accelerometerValues.isNotEmpty()) {
            val sum = accelerometerValues.sum()
            val avg = accelerometerValues.average()
            (sum / avg).toFloat()
        } else 0f
        val heartScore = if (heartRateValues.isNotEmpty()) {
            val sum = heartRateValues.sum()
            val avg = heartRateValues.average()
            (sum / avg).toFloat()
        } else 0f
        val barometerScore = if (barometerValues.isNotEmpty()) {
            val sum = barometerValues.sum()
            val avg = barometerValues.average()
            (sum / avg).toFloat()
        } else 0f
        val gpsScore = if (locationValues.isNotEmpty()) {
            val distance = calcularDistanciaPercorrida(locationValues)
            (distance / 500).toFloat() // Add 1 ponto a cada 500m
        } else 0f

        Log.d("TrainingFragment", "accScore: $accScore, heartScore: $heartScore, barometerScore: $barometerScore, gpsScore: $gpsScore")

        val score = (accScore * accelerometerWeight) +
                (heartScore * heartRateWeight) +
                (barometerScore * barometerWeight) +
                (gpsScore * gpsWeight)

        return score
    }

    private fun calcularDistanciaPercorrida(locations: List<Location>): Double {
        var distance = 0.0
        for (i in 1 until locations.size) {
            distance += locations[i - 1].distanceTo(locations[i])
        }
        return distance
    }

    private fun categorizarTreino(score: Float): String {
        return when {
            score < 5000 -> "Treino Casual"
            score < 20000 -> "Treino Moderado"
            else -> "Treino Intenso"
        }
    }

    private fun salvar(compartilharNoGrupo: Boolean, selectedGroups: List<String> = emptyList()) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("Pessoa")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val score = calcularPontuacaoNumerica()
                val treino = TrainingSession(
                    userId = userId,
                    userName = document.getString("nome") ?: "",
                    duracao = binding.tvTimer.text.toString(),
                    intensidade = categorizarTreino(score),
                    timestamp = System.currentTimeMillis(),
                    pontuacao = score
                )

                // Salva no feed do usuário
                FirebaseFirestore.getInstance().collection("Pessoa")
                    .document(userId)
                    .collection("Atividade")
                    .add(treino)
                    .addOnSuccessListener {
                        if (compartilharNoGrupo) {
                            // Compartilha o treino nos grupos selecionados
                            for (groupId in selectedGroups) {
                                FirebaseFirestore.getInstance().collection("Grupo")
                                    .document(groupId)
                                    .get()
                                    .addOnSuccessListener { groupDocument ->
                                        val groupName = groupDocument.getString("nome") ?: ""
                                        val treinoComGrupo = treino.copy(groupName = groupName)
                                        FirebaseFirestore.getInstance().collection("Grupo")
                                            .document(groupId)
                                            .collection("Atividade")
                                            .add(treinoComGrupo)
                                            .addOnSuccessListener { }
                                            .addOnFailureListener { e ->
                                                Log.e("TrainingFragment", "Erro ao compartilhar no grupo", e)
                                            }
                                    }
                            }
                            Toast.makeText(requireContext(), "Treino salvo e compartilhado!", Toast.LENGTH_SHORT).show()
                        }
                        else Toast.makeText(requireContext(), "Treino salvo!", Toast.LENGTH_SHORT).show()

                        navController.navigate(R.id.nav_home)
                    }
                    .addOnFailureListener { e -> Log.e("Training", "Erro ao salvar", e) }
            }
            .addOnFailureListener { e ->
                Log.e("TrainingFragment", "Erro ao recuperar nome do usuário", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        sensorManager.unregisterListener(this)
    }
}