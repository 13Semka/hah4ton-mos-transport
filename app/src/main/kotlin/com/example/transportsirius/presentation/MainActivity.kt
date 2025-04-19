package com.example.transportsirius.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.transportsirius.R
import com.example.transportsirius.domain.entity.GeocoderResult
import com.example.transportsirius.domain.entity.RouteOption
import com.example.transportsirius.presentation.adapter.RouteAdapter
import com.example.transportsirius.presentation.adapter.SearchResultsAdapter
import com.example.transportsirius.presentation.base.BaseActivity
import com.google.android.material.snackbar.Snackbar
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
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    private val TAG = "MainActivity"
    private val SEARCH_DELAY = 500L
    private val MIN_SEARCH_LENGTH = 3
    private val DEFAULT_ZOOM = 15.0
    
    private val viewModel by viewModels<RouteViewModel>()
    
    private val binding by lazy {
        with(android.view.LayoutInflater.from(this)) {
            val view = inflate(R.layout.activity_main, null)
            setContentView(view)
            object {
                val fromAddressEditText = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.fromAddressEditText)
                val toAddressEditText = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.toAddressEditText)
                val getCurrentLocationButton = findViewById<android.widget.ImageButton>(R.id.getCurrentLocationButton)
                val searchButton = findViewById<android.widget.Button>(R.id.searchButton)
                val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)
                val resultText = findViewById<android.widget.TextView>(R.id.resultText)
                val searchResultsRecyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.searchResultsRecyclerView)
                val routesRecyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.routesRecyclerView)
                val mapView = findViewById<org.osmdroid.views.MapView>(R.id.mapView)
            }
        }
    }
    
    private var mLocationOverlay: MyLocationNewOverlay? = null
    private var currentPolyline: Polyline? = null
    private var fromMarker: Marker? = null
    private var toMarker: Marker? = null
    
    private val searchResultsAdapter by lazy { SearchResultsAdapter(::onSearchResultSelected) }
    private val routeAdapter by lazy { RouteAdapter(::drawRouteOnMap) }
    
    private var searchJob: Job? = null
    private var isUserTyping = false
    private var currentAddressType = AddressType.FROM
    
    enum class AddressType { FROM, TO }
    
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                Toast.makeText(this, "Разрешения на определение местоположения получены", Toast.LENGTH_SHORT).show()
                refreshCurrentLocation()
                setupMapMyLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                showLocationPermissionExplanationDialog()
            }
            else -> showLocationPermissionSettingsDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        initOSMDroid()
        setupMapView()
        setupRecyclerViews()
        setupListeners()
        observeViewModel()
        checkAndRequestLocationPermissions()
    }
    
    private fun initOSMDroid() {
        Configuration.getInstance().apply {
            load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
            userAgentValue = "TransportSirius/1.0"
            File(cacheDir, "osmdroid").apply { 
                mkdirs()
                osmdroidBasePath = this
                osmdroidTileCache = File(this, "tiles")
            }
        }
    }
    
    override fun onResume() { super.onResume(); binding.mapView.onResume() }
    override fun onPause() { super.onPause(); binding.mapView.onPause() }
    
    private fun setupMapView() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(DEFAULT_ZOOM)
            controller.setCenter(GeoPoint(55.7522, 37.6156)) // Москва
            
            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    p?.let {
                        val result = GeocoderResult(
                            name = "Выбранная точка",
                            formattedAddress = "${p.latitude}, ${p.longitude}",
                            latLng = com.example.transportsirius.domain.entity.LatLng(p.latitude, p.longitude)
                        )
                        
                        when {
                            binding.fromAddressEditText.hasFocus() -> viewModel.selectSearchResult(result, true)
                            binding.toAddressEditText.hasFocus() -> viewModel.selectSearchResult(result, false)
                        }
                        return true
                    }
                    return false
                }
                override fun longPressHelper(p: GeoPoint?) = false
            }))
        }
    }
    
    private fun setupMapMyLocation() {
        if (!hasLocationPermission()) return
        
        mLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), binding.mapView).apply {
            enableMyLocation()
            enableFollowLocation()
            binding.mapView.overlays.add(this)
        }
        
        try {
            binding.mapView.overlays.add(
                org.osmdroid.views.overlay.compass.CompassOverlay(this, binding.mapView).apply {
                    enableCompass()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error adding compass overlay", e)
        }
    }
    
    private fun updateMapWithLocations() {
        fromMarker?.let { binding.mapView.overlays.remove(it) }
        toMarker?.let { binding.mapView.overlays.remove(it) }
        
        viewModel.fromLocation.value?.let { location ->
            fromMarker = Marker(binding.mapView).apply {
                position = GeoPoint(location.latitude, location.longitude)
                title = "Откуда"
                snippet = viewModel.fromAddress.value
                icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_location)
                binding.mapView.overlays.add(this)
                binding.mapView.controller.animateTo(position)
            }
        }
        
        viewModel.toLocation.value?.let { location ->
            toMarker = Marker(binding.mapView).apply {
                position = GeoPoint(location.latitude, location.longitude)
                title = "Куда"
                snippet = viewModel.toAddress.value
                icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_destination)
                binding.mapView.overlays.add(this)
            }
        }
        
        binding.mapView.invalidate()
    }
    
    private fun drawRouteOnMap(route: RouteOption) {
        currentPolyline?.let { binding.mapView.overlays.remove(it) }
        
        val points = route.points.map { GeoPoint(it.latitude, it.longitude) }
        if (points.isEmpty()) return
        
        currentPolyline = Polyline().apply {
            setPoints(points)
            outlinePaint.color = ContextCompat.getColor(this@MainActivity, R.color.route_line)
            outlinePaint.strokeWidth = 8f
            binding.mapView.overlays.add(this)
        }
        
        try {
            val boundingBox = points.fold(
                BoundingBox(points[0].latitude, points[0].longitude, points[0].latitude, points[0].longitude)
            ) { box, point ->
                BoundingBox(
                    maxOf(box.latNorth, point.latitude),
                    maxOf(box.lonEast, point.longitude),
                    minOf(box.latSouth, point.latitude),
                    minOf(box.lonWest, point.longitude)
                )
            }
            
            binding.mapView.zoomToBoundingBox(
                BoundingBox(
                    boundingBox.latNorth + 0.01,
                    boundingBox.lonEast + 0.01,
                    boundingBox.latSouth - 0.01,
                    boundingBox.lonWest - 0.01
                ), true, 100
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error zooming to route bounds", e)
            binding.mapView.controller.animateTo(points[0])
        }
        
        binding.mapView.invalidate()
    }
    
    private fun checkAndRequestLocationPermissions() {
        if (android.os.Build.VERSION.SDK_INT < 29 && 
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            return
        }
        
        when {
            hasLocationPermission() -> {
                refreshCurrentLocation()
                setupMapMyLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                showLocationPermissionExplanationDialog()
            }
            else -> requestLocationPermissions()
        }
    }
    
    private fun showLocationPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Необходимы разрешения")
            .setMessage("Для определения вашего текущего местоположения, необходимо разрешение на доступ к геолокации устройства.")
            .setPositiveButton("Предоставить") { _, _ -> requestLocationPermissions() }
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
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                })
            }
            .setNegativeButton("Отмена") { _, _ ->
                Toast.makeText(this, "Будет использоваться местоположение по умолчанию", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun requestLocationPermissions() =
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    
    private fun refreshCurrentLocation() = viewModel.getCurrentLocation()
    
    private fun setupRecyclerViews() {
        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = searchResultsAdapter
        }
        
        binding.routesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = routeAdapter
        }
    }
    
    private fun setupListeners() {
        binding.getCurrentLocationButton.setOnClickListener {
            if (hasLocationPermission()) {
                refreshCurrentLocation()
                showSnackbar("Обновляем ваше местоположение...")
            } else {
                checkAndRequestLocationPermissions()
            }
        }
        
        binding.searchButton.setOnClickListener {
            hideSearchResults()
            viewModel.loadRoutes()
        }
        
        binding.fromAddressEditText.apply {
            setOnFocusChangeListener { _, hasFocus -> if (hasFocus) currentAddressType = AddressType.FROM }
            addTextChangedListener(createTextWatcher(AddressType.FROM))
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    autoCompleteCurrentAddress()
                    binding.toAddressEditText.requestFocus()
                    true
                } else false
            }
        }
        
        binding.toAddressEditText.apply {
            setOnFocusChangeListener { _, hasFocus -> if (hasFocus) currentAddressType = AddressType.TO }
            addTextChangedListener(createTextWatcher(AddressType.TO))
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    autoCompleteCurrentAddress()
                    hideKeyboard()
                    if (binding.fromAddressEditText.text?.isNotEmpty() == true && 
                        binding.toAddressEditText.text?.isNotEmpty() == true) {
                        hideSearchResults()
                        viewModel.loadRoutes()
                    }
                    true
                } else false
            }
        }
    }
    
    private fun createTextWatcher(type: AddressType) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            currentAddressType = type
            s?.toString()?.let { searchWithDelay(it) }
        }
        override fun afterTextChanged(s: Editable?) {}
    }
    
    private fun autoCompleteCurrentAddress() =
        viewModel.searchResults.value?.firstOrNull()?.let { onSearchResultSelected(it) }
    
    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    
    private fun searchWithDelay(query: String) {
        searchJob?.cancel()
        
        if (query.length < MIN_SEARCH_LENGTH) {
            hideSearchResults()
            return
        }
        
        searchJob = lifecycleScope.launch {
            delay(SEARCH_DELAY)
            viewModel.searchAddress(query, currentAddressType == AddressType.FROM)
            isUserTyping = false
        }
    }
    
    private fun onSearchResultSelected(result: GeocoderResult) {
        when (currentAddressType) {
            AddressType.FROM -> {
                binding.fromAddressEditText.setText(result.name)
                viewModel.setFromAddress(result)
            }
            AddressType.TO -> {
                binding.toAddressEditText.setText(result.name)
                viewModel.setToAddress(result)
            }
        }
        
        hideSearchResults()
        
        if (currentAddressType == AddressType.FROM && binding.toAddressEditText.text?.isEmpty() == true) {
            binding.toAddressEditText.requestFocus()
        } else {
            binding.fromAddressEditText.clearFocus()
            binding.toAddressEditText.clearFocus()
            hideKeyboard()
        }
        
        if (binding.fromAddressEditText.text?.isNotEmpty() == true && 
            binding.toAddressEditText.text?.isNotEmpty() == true) {
            viewModel.loadRoutes()
        }
    }
    
    private fun hideKeyboard() =
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    
    private fun hideSearchResults() { binding.searchResultsRecyclerView.visibility = View.GONE }
    private fun showSearchResults() { binding.searchResultsRecyclerView.visibility = View.VISIBLE }
    private fun showSnackbar(message: String) = 
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.fromAddress.collectLatest { address ->
                if (address.isNotEmpty() && !isUserTyping) {
                    binding.fromAddressEditText.setText(address)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.toAddress.collectLatest { address ->
                if (address.isNotEmpty() && !isUserTyping) {
                    binding.toAddressEditText.setText(address)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.fromLocation.collectLatest { location ->
                if (location != null) updateMapWithLocations()
            }
        }
        
        lifecycleScope.launch {
            viewModel.toLocation.collectLatest { location ->
                if (location != null) updateMapWithLocations()
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.searchButton.isEnabled = !isLoading
                binding.getCurrentLocationButton.isEnabled = !isLoading
            }
        }
        
        viewModel.searchResults.observe(this) { results ->
            searchResultsAdapter.submitList(results)
            if (results.isNotEmpty()) showSearchResults()
        }
        
        viewModel.routes.observe(this) { routes ->
            binding.resultText.apply {
                visibility = View.VISIBLE
                text = "Найдено маршрутов: ${routes.size}"
            }
            
            if (routes.isNotEmpty()) {
                routeAdapter.submitList(routes)
                binding.routesRecyclerView.visibility = View.VISIBLE
                drawRouteOnMap(routes.first())
            } else {
                binding.routesRecyclerView.visibility = View.GONE
                Toast.makeText(this, "Маршруты не найдены", Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                Snackbar.make(findViewById(android.R.id.content), it, Snackbar.LENGTH_LONG)
                    .setAction("OK") { viewModel.clearError() }
                    .show()
            }
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkAndRequestLocationPermissions()
        } else if (requestCode == 1) {
            Toast.makeText(this, "Без разрешения на запись карты могут не работать корректно", Toast.LENGTH_LONG).show()
            
            if (hasLocationPermission()) {
                refreshCurrentLocation()
                setupMapMyLocation()
            } else {
                requestLocationPermissions()
            }
        }
    }
}