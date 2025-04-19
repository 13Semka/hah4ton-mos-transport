package com.example.transportsirius.presentation.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.transportsirius.R
import com.example.transportsirius.databinding.FragmentLocationBinding
import com.example.transportsirius.domain.entity.GeocoderResult
import com.example.transportsirius.domain.entity.LatLng
import com.example.transportsirius.presentation.RouteViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Фрагмент для отображения карты и работы с локациями.
 * Используется для выбора начальной и конечной точек маршрута.
 */
@AndroidEntryPoint
class LocationFragment : Fragment() {
    companion object {
        private const val TAG = "LocationFragment"
        private const val DEFAULT_ZOOM = 15.0
        
        fun newInstance() = LocationFragment()
    }

    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!
    
    private val routeViewModel: RouteViewModel by activityViewModels()
    
    private var mLocationOverlay: MyLocationNewOverlay? = null
    private var currentPolyline: Polyline? = null
    private var fromMarker: Marker? = null
    private var toMarker: Marker? = null
    private var selectedMarker: Marker? = null
    
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                Toast.makeText(requireContext(), R.string.updating_location, Toast.LENGTH_SHORT).show()
                routeViewModel.getCurrentLocation()
                setupMapMyLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                showLocationPermissionExplanationDialog()
            }
            else -> showLocationPermissionSettingsDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMap()
        setupObservers()
        setupListeners()
        checkAndRequestLocationPermissions()
    }

    private fun setupMap() {
        binding.mapView.apply {
            setMultiTouchControls(true)
            controller.setZoom(DEFAULT_ZOOM)
            controller.setCenter(GeoPoint(55.7522, 37.6156)) // Москва
            
            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    p?.let {
                        showSelectedLocation(it)
                        return true
                    }
                    return false
                }
                override fun longPressHelper(p: GeoPoint?) = false
            }))
        }
    }
    
    private fun showSelectedLocation(point: GeoPoint) {
        selectedMarker?.let { binding.mapView.overlays.remove(it) }
        
        selectedMarker = Marker(binding.mapView).apply {
            position = point
            title = getString(R.string.set_as_origin)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_location)
            binding.mapView.overlays.add(this)
        }
        binding.mapView.invalidate()
        
        // Показываем карточку с информацией о маркере
        showMarkerInfo(point)
    }
    
    private fun showMarkerInfo(point: GeoPoint) {
        binding.markerInfoContainer.visibility = View.VISIBLE
        binding.markerTitle.text = getString(R.string.set_as_origin)
        binding.markerAddress.text = "${point.latitude}, ${point.longitude}"
        
        // Обратное геокодирование для получения адреса
        lifecycleScope.launch {
            try {
                val location = LatLng(point.latitude, point.longitude)
                val results = routeViewModel.reverseGeocode(location)
                if (results.isNotEmpty()) {
                    binding.markerAddress.text = results[0].formattedAddress
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reverse geocoding", e)
            }
        }
        
        binding.btnSetOrigin.setOnClickListener {
            val result = GeocoderResult(
                name = binding.markerAddress.text.toString(),
                formattedAddress = binding.markerAddress.text.toString(),
                latLng = LatLng(point.latitude, point.longitude)
            )
            routeViewModel.setFromAddress(result)
            binding.markerInfoContainer.visibility = View.GONE
        }
        
        binding.btnSetDestination.setOnClickListener {
            val result = GeocoderResult(
                name = binding.markerAddress.text.toString(),
                formattedAddress = binding.markerAddress.text.toString(),
                latLng = LatLng(point.latitude, point.longitude)
            )
            routeViewModel.setToAddress(result)
            binding.markerInfoContainer.visibility = View.GONE
        }
    }
    
    private fun setupObservers() {
        // Подписка на обновления маршрута от ViewModel
        routeViewModel.routes.observe(viewLifecycleOwner) { routes ->
            if (routes.isNotEmpty()) {
                updateRouteOnMap(routes.first())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            routeViewModel.fromLocation.collectLatest { location ->
                if (location != null) updateMapWithLocations()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            routeViewModel.toLocation.collectLatest { location ->
                if (location != null) updateMapWithLocations()
            }
        }
    }

    private fun setupListeners() {
        binding.btnMyLocation.setOnClickListener {
            if (hasLocationPermission()) {
                routeViewModel.getCurrentLocation()
                Toast.makeText(requireContext(), getString(R.string.updating_location), Toast.LENGTH_SHORT).show()
            } else {
                checkAndRequestLocationPermissions()
            }
        }
    }

    private fun updateRouteOnMap(route: Any?) {
        if (route !is com.example.transportsirius.domain.entity.RouteOption) return
        
        currentPolyline?.let { binding.mapView.overlays.remove(it) }
        
        val points = route.points.map { GeoPoint(it.latitude, it.longitude) }
        if (points.isEmpty()) return
        
        currentPolyline = Polyline().apply {
            setPoints(points)
            outlinePaint.color = ContextCompat.getColor(requireContext(), R.color.route_line)
            outlinePaint.strokeWidth = 8f
            binding.mapView.overlays.add(this)
        }
        
        try {
            val boundingBox = points.fold(
                org.osmdroid.util.BoundingBox(
                    points[0].latitude, points[0].longitude, 
                    points[0].latitude, points[0].longitude
                )
            ) { box, point ->
                org.osmdroid.util.BoundingBox(
                    maxOf(box.latNorth, point.latitude),
                    maxOf(box.lonEast, point.longitude),
                    minOf(box.latSouth, point.latitude),
                    minOf(box.lonWest, point.longitude)
                )
            }
            
            binding.mapView.zoomToBoundingBox(
                org.osmdroid.util.BoundingBox(
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

    private fun updateMapWithLocations() {
        fromMarker?.let { binding.mapView.overlays.remove(it) }
        toMarker?.let { binding.mapView.overlays.remove(it) }
        
        routeViewModel.fromLocation.value?.let { location ->
            fromMarker = Marker(binding.mapView).apply {
                position = GeoPoint(location.latitude, location.longitude)
                title = getString(R.string.from)
                snippet = routeViewModel.fromAddress.value
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_location)
                binding.mapView.overlays.add(this)
            }
        }
        
        routeViewModel.toLocation.value?.let { location ->
            toMarker = Marker(binding.mapView).apply {
                position = GeoPoint(location.latitude, location.longitude)
                title = getString(R.string.to)
                snippet = routeViewModel.toAddress.value
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_destination)
                binding.mapView.overlays.add(this)
            }
        }
        
        // Если оба маркера установлены, зумируем карту, чтобы видеть оба маркера
        if (fromMarker != null && toMarker != null) {
            val points = listOf(fromMarker!!.position, toMarker!!.position)
            try {
                val boundingBox = points.fold(
                    org.osmdroid.util.BoundingBox(
                        points[0].latitude, points[0].longitude, 
                        points[0].latitude, points[0].longitude
                    )
                ) { box, point ->
                    org.osmdroid.util.BoundingBox(
                        maxOf(box.latNorth, point.latitude),
                        maxOf(box.lonEast, point.longitude),
                        minOf(box.latSouth, point.latitude),
                        minOf(box.lonWest, point.longitude)
                    )
                }
                
                binding.mapView.zoomToBoundingBox(
                    org.osmdroid.util.BoundingBox(
                        boundingBox.latNorth + 0.01,
                        boundingBox.lonEast + 0.01,
                        boundingBox.latSouth - 0.01,
                        boundingBox.lonWest - 0.01
                    ), true, 100
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error zooming to markers", e)
            }
        } else {
            // Если только один маркер, зумируем к нему
            fromMarker?.let { binding.mapView.controller.animateTo(it.position) }
            toMarker?.let { binding.mapView.controller.animateTo(it.position) }
        }
        
        binding.mapView.invalidate()
    }

    private fun setupMapMyLocation() {
        if (!hasLocationPermission()) return
        
        mLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), binding.mapView).apply {
            enableMyLocation()
            enableFollowLocation()
            binding.mapView.overlays.add(this)
        }
        
        try {
            binding.mapView.overlays.add(
                org.osmdroid.views.overlay.compass.CompassOverlay(requireContext(), binding.mapView).apply {
                    enableCompass()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error adding compass overlay", e)
        }
        
        binding.mapView.invalidate()
    }

    private fun checkAndRequestLocationPermissions() {
        when {
            hasLocationPermission() -> {
                routeViewModel.getCurrentLocation()
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
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.permissions_needed)
            .setMessage(R.string.location_permission_rationale)
            .setPositiveButton(R.string.grant) { _, _ -> requestLocationPermissions() }
            .setNegativeButton(R.string.cancel) { _, _ -> 
                Toast.makeText(requireContext(), R.string.permissions_required, Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun showLocationPermissionSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.permissions_denied)
            .setMessage(R.string.location_permission_denied_message)
            .setPositiveButton(R.string.settings) { _, _ -> 
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireActivity().packageName, null)
                })
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                Toast.makeText(requireContext(), R.string.default_location_used, Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun requestLocationPermissions() =
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 