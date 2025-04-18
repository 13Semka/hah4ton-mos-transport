package com.example.transportsirius.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.preference.PreferenceManager
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.transportsirius.R
import com.example.transportsirius.domain.entity.GeocoderResult
import com.example.transportsirius.domain.entity.RouteOption
import com.example.transportsirius.presentation.adapter.RouteAdapter
import com.example.transportsirius.presentation.adapter.SearchResultsAdapter
import com.example.transportsirius.presentation.base.BaseActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val SEARCH_DELAY = 500L // ms
        private const val MIN_SEARCH_LENGTH = 3
        private const val DEFAULT_ZOOM = 15.0
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
    
    // OSM Карта
    private lateinit var mapView: MapView
    private var mLocationOverlay: MyLocationNewOverlay? = null
    private var currentRoute: RouteOption? = null
    private var currentPolyline: Polyline? = null
    private var fromMarker: Marker? = null
    private var toMarker: Marker? = null
    
    // Адаптеры
    private lateinit var searchResultsAdapter: SearchResultsAdapter
    private lateinit var routeAdapter: RouteAdapter
    
    // Для обработки ввода с задержкой
    private var searchJob: Job? = null
    private var isEditingFromAddress = true
    private var isUserTyping = false
    
    // Тип адреса, с которым сейчас работаем
    private var currentAddressType = AddressType.FROM
    
    enum class AddressType {
        FROM, TO
    }
    
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
                setupMapMyLocation()
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
        
        // Инициализация конфигурации OSMDroid
        initOSMDroid()
        
        // Инициализация UI элементов
        initViews()
        setupMapView()
        setupRecyclerViews()
        setupListeners()
        observeViewModel()
        
        // Настраиваем обработчики событий для полей ввода
        setupInputListeners()
        
        // Запрашиваем разрешения на локацию при создании активности
        checkAndRequestLocationPermissions()
    }
    
    private fun initOSMDroid() {
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        
        // Устанавливаем User-Agent для запросов к серверам OSM
        Configuration.getInstance().userAgentValue = "TransportSirius/1.0"
        
        // Настраиваем каталог для кэша карт
        val osmCacheDir = File(cacheDir, "osmdroid")
        if (!osmCacheDir.exists()) {
            osmCacheDir.mkdirs()
        }
        Configuration.getInstance().osmdroidBasePath = osmCacheDir
        Configuration.getInstance().osmdroidTileCache = File(osmCacheDir, "tiles")
    }
    
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
    
    private fun setupMapView() {
        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        
        // Настройка начального положения и масштаба
        val mapController = mapView.controller
        mapController.setZoom(DEFAULT_ZOOM)
        
        // По умолчанию центрируем карту на Москве
        val startPoint = GeoPoint(55.7522, 37.6156)
        mapController.setCenter(startPoint)
        
        // Добавляем обработчик нажатия на карту
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    // В зависимости от того, какое поле сейчас в фокусе
                    val result = GeocoderResult(
                        name = "Выбранная точка",
                        formattedAddress = "${p.latitude}, ${p.longitude}",
                        latLng = com.example.transportsirius.domain.entity.LatLng(p.latitude, p.longitude)
                    )
                    
                    if (fromAddressEditText.hasFocus()) {
                        isEditingFromAddress = true
                        viewModel.selectSearchResult(result, true)
                    } else if (toAddressEditText.hasFocus()) {
                        isEditingFromAddress = false
                        viewModel.selectSearchResult(result, false)
                    }
                    
                    return true
                }
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        }
        
        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        mapView.overlays.add(mapEventsOverlay)
    }
    
    private fun setupMapMyLocation() {
        if (hasLocationPermission()) {
            // Настраиваем отображение текущего местоположения
            mLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
            mLocationOverlay?.enableMyLocation()
            mLocationOverlay?.enableFollowLocation()
            mapView.overlays.add(mLocationOverlay)
            
            // Добавляем кнопку для центрирования на текущем местоположении
            try {
                val compassOverlay = org.osmdroid.views.overlay.compass.CompassOverlay(
                    this, mapView
                )
                compassOverlay.enableCompass()
                mapView.overlays.add(compassOverlay)
            } catch (e: Exception) {
                Log.e(TAG, "Error adding compass overlay", e)
            }
        }
    }
    
    private fun updateMapWithLocations() {
        // Очищаем предыдущие маркеры
        fromMarker?.let { mapView.overlays.remove(it) }
        toMarker?.let { mapView.overlays.remove(it) }
        
        // Добавляем маркер "Откуда", если координаты есть
        viewModel.fromLocation.value?.let { location ->
            val fromGeoPoint = GeoPoint(location.latitude, location.longitude)
            fromMarker = Marker(mapView).apply {
                position = fromGeoPoint
                title = "Откуда"
                snippet = viewModel.fromAddress.value
                icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_location)
            }
            mapView.overlays.add(fromMarker)
            
            // Перемещаем камеру к начальной точке
            mapView.controller.animateTo(fromGeoPoint)
        }
        
        // Добавляем маркер "Куда", если координаты есть
        viewModel.toLocation.value?.let { location ->
            val toGeoPoint = GeoPoint(location.latitude, location.longitude)
            toMarker = Marker(mapView).apply {
                position = toGeoPoint
                title = "Куда"
                snippet = viewModel.toAddress.value
                icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_destination)
            }
            mapView.overlays.add(toMarker)
        }
        
        mapView.invalidate()
    }
    
    private fun drawRouteOnMap(route: RouteOption) {
        // Сначала удаляем предыдущую линию маршрута
        currentPolyline?.let { mapView.overlays.remove(it) }
        
        // Преобразуем координаты из domain модели в GeoPoint
        val points = mutableListOf<GeoPoint>()
        for (point in route.points) {
            points.add(GeoPoint(point.latitude, point.longitude))
        }
        
        // Создаем и добавляем полилинию на карту
        currentPolyline = Polyline().apply {
            setPoints(points)
            outlinePaint.color = ContextCompat.getColor(this@MainActivity, R.color.purple_500)
            outlinePaint.strokeWidth = 8f
            infoWindow = null
        }
        mapView.overlays.add(currentPolyline)
        currentRoute = route
        
        // Если у маршрута есть точки, перемещаем камеру, чтобы показать весь маршрут
        if (points.isNotEmpty()) {
            try {
                // Создаем BoundingBox вручную
                var north = -90.0
                var south = 90.0
                var east = -180.0
                var west = 180.0
                
                for (point in points) {
                    north = north.coerceAtLeast(point.latitude)
                    south = south.coerceAtMost(point.latitude)
                    east = east.coerceAtLeast(point.longitude)
                    west = west.coerceAtMost(point.longitude)
                }
                
                // Добавляем отступы
                val padding = 0.01 // ~1 км
                north += padding
                south -= padding
                east += padding
                west -= padding
                
                val boundingBox = BoundingBox(north, east, south, west)
                mapView.zoomToBoundingBox(boundingBox, true, 100)
            } catch (e: Exception) {
                Log.e(TAG, "Error zooming to route bounds", e)
                // Если не удалось показать весь маршрут, показываем первую точку
                if (points.isNotEmpty()) {
                    mapView.controller.animateTo(points[0])
                }
            }
        }
        
        mapView.invalidate()
    }
    
    private fun checkAndRequestLocationPermissions() {
        // Сначала проверяем разрешение на запись для OSMDroid для API < 29
        if (android.os.Build.VERSION.SDK_INT < 29 && 
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
            return
        }
        
        // Затем проверяем разрешения на геолокацию
        when {
            hasLocationPermission() -> {
                // Разрешения уже есть, ничего делать не нужно
                Log.d(TAG, "Location permissions already granted")
                refreshCurrentLocation()
                setupMapMyLocation()
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
        // Инициализация UI элементов
        fromAddressEditText = findViewById(R.id.fromAddressEditText)
        toAddressEditText = findViewById(R.id.toAddressEditText)
        getCurrentLocationButton = findViewById(R.id.getCurrentLocationButton)
        searchButton = findViewById(R.id.searchButton)
        progressBar = findViewById(R.id.progressBar)
        resultText = findViewById(R.id.resultText)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        routesRecyclerView = findViewById(R.id.routesRecyclerView)
        
        // Обработчик для кнопки текущего местоположения
        getCurrentLocationButton.setOnClickListener {
            viewModel.getCurrentLocation()
        }
        
        // Обработчик для кнопки поиска маршрута
        searchButton.setOnClickListener {
            viewModel.loadRoutes()
        }
    }
    
    private fun setupRecyclerViews() {
        // Настраиваем адаптер для результатов поиска
        searchResultsAdapter = SearchResultsAdapter { result ->
            onSearchResultSelected(result)
        }
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchResultsRecyclerView.adapter = searchResultsAdapter
        
        // Настраиваем адаптер для маршрутов
        routeAdapter = RouteAdapter { route ->
            // Обработчик нажатия на маршрут
            drawRouteOnMap(route)
        }
        routesRecyclerView.layoutManager = LinearLayoutManager(this)
        routesRecyclerView.adapter = routeAdapter
    }
    
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
    }
    
    private fun setupInputListeners() {
        // Обработчик события фокуса на поле "Откуда"
        fromAddressEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                currentAddressType = AddressType.FROM
            }
        }
        
        // Обработчик события фокуса на поле "Куда"
        toAddressEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                currentAddressType = AddressType.TO
            }
        }
        
        // Обработчик ввода текста для поля "Откуда"
        fromAddressEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentAddressType = AddressType.FROM
                s?.toString()?.let { query ->
                    searchWithDelay(query)
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Обработчик ввода текста для поля "Куда"
        toAddressEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentAddressType = AddressType.TO
                s?.toString()?.let { query ->
                    searchWithDelay(query)
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
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
        
        if (query.length < MIN_SEARCH_LENGTH) {
            hideSearchResults()
            return
        }
        
        searchJob = lifecycleScope.launch {
            delay(SEARCH_DELAY) // Задержка для избежания частых запросов
            viewModel.searchAddress(query, isEditingFromAddress)
            isUserTyping = false
        }
    }
    
    private fun onSearchResultSelected(result: GeocoderResult) {
        // Обновляем соответствующее поле в зависимости от текущего типа адреса
        when (currentAddressType) {
            AddressType.FROM -> {
                fromAddressEditText.setText(result.name)
                viewModel.setFromAddress(result)
            }
            AddressType.TO -> {
                toAddressEditText.setText(result.name)
                viewModel.setToAddress(result)
            }
        }
        
        // Скрываем список результатов
        hideSearchResults()
        
        // Если заполнены оба поля, выполняем поиск маршрута автоматически
        if (fromAddressEditText.text?.isNotEmpty() == true && toAddressEditText.text?.isNotEmpty() == true) {
            viewModel.loadRoutes()
        }
        
        // Переносим фокус на следующее поле или скрываем клавиатуру если оба поля заполнены
        if (currentAddressType == AddressType.FROM && toAddressEditText.text?.isEmpty() == true) {
            toAddressEditText.requestFocus()
        } else {
            // Убираем фокус с полей ввода
            fromAddressEditText.clearFocus()
            toAddressEditText.clearFocus()
            
            // Скрываем клавиатуру
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
    }
    
    private fun hideSearchResults() {
        searchResultsRecyclerView.visibility = View.GONE
    }
    
    private fun showSearchResults() {
        searchResultsRecyclerView.visibility = View.VISIBLE
    }
    
    private fun searchAddress(query: String) {
        if (query.length < 3) {
            hideSearchResults()
            return
        }
        
        // Показываем список результатов поиска
        showSearchResults()
        
        // Запускаем поиск адреса в зависимости от текущего типа адреса
        when (currentAddressType) {
            AddressType.FROM -> viewModel.searchAddress(query, true)
            AddressType.TO -> viewModel.searchAddress(query, false)
        }
    }
    
    private fun observeViewModel() {
        // Наблюдаем за адресом "Откуда"
        lifecycleScope.launch {
            viewModel.fromAddress.collectLatest { address ->
                if (address.isNotEmpty() && !isUserTyping) {
                    fromAddressEditText.setText(address)
                }
            }
        }
        
        // Наблюдаем за адресом "Куда"
        lifecycleScope.launch {
            viewModel.toAddress.collectLatest { address ->
                if (address.isNotEmpty() && !isUserTyping) {
                    toAddressEditText.setText(address)
                }
            }
        }
        
        // Наблюдаем за изменениями локаций для обновления карты
        lifecycleScope.launch {
            viewModel.fromLocation.collectLatest { location ->
                if (location != null) {
                    updateMapWithLocations()
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.toLocation.collectLatest { location ->
                if (location != null) {
                    updateMapWithLocations()
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
            searchResultsAdapter.submitList(results)
            
            // Показываем результаты поиска, когда они доступны
            showSearchResults()
        }
        
        // Наблюдаем за маршрутами
        viewModel.routes.observe(this) { routes ->
            resultText.visibility = View.VISIBLE
            resultText.text = "Найдено маршрутов: ${routes.size}"
            
            if (routes.isNotEmpty()) {
                routeAdapter.submitList(routes)
                routesRecyclerView.visibility = View.VISIBLE
                
                // Отображаем первый маршрут на карте
                drawRouteOnMap(routes.first())
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
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == 1 && grantResults.isNotEmpty() && 
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Если получили разрешение на запись, теперь проверяем разрешения на геолокацию
            checkAndRequestLocationPermissions()
        } else if (requestCode == 1) {
            // Разрешение на запись не получено
            Toast.makeText(
                this, 
                "Без разрешения на запись карты могут не работать корректно",
                Toast.LENGTH_LONG
            ).show()
            // Все равно продолжаем и проверяем разрешения на геолокацию
            when {
                hasLocationPermission() -> {
                    refreshCurrentLocation()
                    setupMapMyLocation()
                }
                else -> {
                    requestLocationPermissions()
                }
            }
        }
    }
    
    // Функция для отображения секции маршрутов
    private fun showRoutesSection() {
        routesRecyclerView.visibility = View.VISIBLE
        resultText.visibility = View.VISIBLE
    }
    
    // Функция для скрытия секции маршрутов
    private fun hideRoutesSection() {
        routesRecyclerView.visibility = View.GONE
        resultText.visibility = View.GONE
    }
}