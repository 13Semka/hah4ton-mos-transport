package com.example.transportsirius.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.transportsirius.R
import com.example.transportsirius.domain.entity.GeocoderResult
import com.example.transportsirius.presentation.adapter.RouteAdapter
import com.example.transportsirius.presentation.adapter.SearchResultAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private val viewModel: RouteViewModel by viewModels()
    
    // UI элементы
    private lateinit var fromAddressEditText: TextInputEditText
    private lateinit var toAddressEditText: TextInputEditText
    private lateinit var getCurrentLocationButton: ImageButton
    private lateinit var searchButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var resultText: TextView
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var routesRecyclerView: RecyclerView
    
    // Адаптеры
    private lateinit var searchResultAdapter: SearchResultAdapter
    private lateinit var routeAdapter: RouteAdapter
    
    // Для обработки ввода с задержкой
    private var searchJob: Job? = null
    private var isEditingFromAddress = true
    
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        when {
            fineLocationGranted || coarseLocationGranted -> {
                // Разрешения получены, можно получить местоположение
                Log.d(TAG, "Location permissions granted")
                Toast.makeText(this, "Разрешения на определение местоположения получены", Toast.LENGTH_SHORT).show()
                refreshCurrentLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                // Пользователь отклонил разрешения, но еще можно показать объяснение
                showLocationPermissionExplanationDialog()
            }
            else -> {
                // Пользователь отклонил разрешения и выбрал "больше не спрашивать"
                showLocationPermissionSettingsDialog()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Инициализация UI элементов
        initViews()
        setupRecyclerViews()
        setupListeners()
        observeViewModel()
        
        // Запрашиваем разрешения на локацию при создании активности
        checkAndRequestLocationPermissions()
    }
    
    private fun checkAndRequestLocationPermissions() {
        when {
            hasLocationPermission() -> {
                // Разрешения уже есть, ничего делать не нужно
                Log.d(TAG, "Location permissions already granted")
                refreshCurrentLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                // Пользователь ранее отказал в разрешениях, показываем объяснение
                showLocationPermissionExplanationDialog()
            }
            else -> {
                // Запрашиваем разрешения впервые
                requestLocationPermissions()
            }
        }
    }
    
    private fun showLocationPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Необходимы разрешения")
            .setMessage("Для определения вашего текущего местоположения, необходимо разрешение на доступ к геолокации устройства.")
            .setPositiveButton("Предоставить") { _, _ ->
                requestLocationPermissions()
            }
            .setNegativeButton("Отмена") { _, _ ->
                Toast.makeText(this, "Для определения местоположения необходимы разрешения", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showLocationPermissionSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Разрешения отклонены")
            .setMessage("Без разрешения на доступ к геолокации мы не сможем определить ваше местоположение. Пожалуйста, предоставьте разрешение в настройках приложения.")
            .setPositiveButton("Настройки") { _, _ ->
                // Открываем настройки приложения
                openAppSettings()
            }
            .setNegativeButton("Отмена") { _, _ ->
                Toast.makeText(this, "Будет использоваться местоположение по умолчанию", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
    
    private fun requestLocationPermissions() {
        Log.d(TAG, "Requesting location permissions")
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    private fun refreshCurrentLocation() {
        Log.d(TAG, "Refreshing current location")
        viewModel.getCurrentLocation()
    }
    
    private fun initViews() {
        fromAddressEditText = findViewById(R.id.fromAddressEditText)
        toAddressEditText = findViewById(R.id.toAddressEditText)
        getCurrentLocationButton = findViewById(R.id.getCurrentLocationButton)
        searchButton = findViewById(R.id.searchButton)
        progressBar = findViewById(R.id.progressBar)
        resultText = findViewById(R.id.resultText)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        routesRecyclerView = findViewById(R.id.routesRecyclerView)
    }
    
    private fun setupRecyclerViews() {
        // Настраиваем адаптер для результатов поиска
        searchResultAdapter = SearchResultAdapter { result ->
            onSearchResultSelected(result)
        }
        searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = searchResultAdapter
        }
        
        // Настраиваем адаптер для маршрутов
        routeAdapter = RouteAdapter()
        routesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = routeAdapter
        }
    }
    
    @OptIn(FlowPreview::class)
    private fun setupListeners() {
        // Кнопка определения текущего местоположения
        getCurrentLocationButton.setOnClickListener {
            if (hasLocationPermission()) {
                refreshCurrentLocation()
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Обновляем ваше местоположение...",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                checkAndRequestLocationPermissions()
            }
        }
        
        // Кнопка поиска маршрутов
        searchButton.setOnClickListener {
            hideSearchResults()
            viewModel.loadRoutes()
        }
        
        // Слушатель для поля "Откуда"
        fromAddressEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                isEditingFromAddress = true
                searchWithDelay(s.toString())
            }
        })
        
        // Слушатель для поля "Куда"
        toAddressEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                isEditingFromAddress = false
                searchWithDelay(s.toString())
            }
        })
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun searchWithDelay(query: String) {
        searchJob?.cancel()
        
        if (query.length < 3) {
            hideSearchResults()
            return
        }
        
        searchJob = lifecycleScope.launch {
            delay(500) // Задержка для избежания частых запросов
            viewModel.searchAddress(query, isEditingFromAddress)
        }
    }
    
    private fun onSearchResultSelected(result: GeocoderResult) {
        viewModel.selectSearchResult(result, isEditingFromAddress)
        hideSearchResults()
    }
    
    private fun hideSearchResults() {
        searchResultsRecyclerView.visibility = View.GONE
    }
    
    private fun observeViewModel() {
        // Наблюдаем за адресом "Откуда"
        lifecycleScope.launch {
            viewModel.fromAddress.collectLatest { address ->
                if (address.isNotEmpty()) {
                    fromAddressEditText.setText(address)
                }
            }
        }
        
        // Наблюдаем за адресом "Куда"
        lifecycleScope.launch {
            viewModel.toAddress.collectLatest { address ->
                if (address.isNotEmpty()) {
                    toAddressEditText.setText(address)
                }
            }
        }
        
        // Наблюдаем за состоянием загрузки
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                searchButton.isEnabled = !isLoading
                getCurrentLocationButton.isEnabled = !isLoading
            }
        }
        
        // Наблюдаем за результатами поиска адресов
        viewModel.searchResults.observe(this) { results ->
            if (results.isEmpty()) {
                searchResultsRecyclerView.visibility = View.GONE
            } else {
                searchResultAdapter.submitList(results)
                searchResultsRecyclerView.visibility = View.VISIBLE
            }
        }
        
        // Наблюдаем за маршрутами
        viewModel.routes.observe(this) { routes ->
            resultText.visibility = View.VISIBLE
            resultText.text = "Найдено маршрутов: ${routes.size}"
            
            if (routes.isNotEmpty()) {
                routeAdapter.submitList(routes)
                routesRecyclerView.visibility = View.VISIBLE
            } else {
                routesRecyclerView.visibility = View.GONE
                Toast.makeText(this, "Маршруты не найдены", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Наблюдаем за ошибками
        viewModel.error.observe(this) { error ->
            if (error != null) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    error,
                    Snackbar.LENGTH_LONG
                ).setAction("OK") {
                    viewModel.clearError()
                }.show()
            }
        }
    }
}