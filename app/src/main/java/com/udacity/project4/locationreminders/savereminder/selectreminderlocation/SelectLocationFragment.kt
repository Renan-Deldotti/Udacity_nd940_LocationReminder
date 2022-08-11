package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var locationProvider: FusedLocationProviderClient
    private lateinit var pointOfInterest: PointOfInterest
    private val mHandler = Handler(Looper.getMainLooper())
    private var retriesSoFar = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        binding.saveLocationButton.isEnabled = false
        binding.saveLocationButton.setOnClickListener {
            onLocationSelected()
        }

        locationProvider = LocationServices.getFusedLocationProviderClient(requireContext())

        val mapsFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapsFragment.getMapAsync(this)

        return binding.root
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0

        _viewModel.selectedPOI.value?.let {
            updatePositionArea(it)
        }

        googleMap.setOnMapClickListener {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addressLocations = geocoder.getFromLocation(it.latitude, it.longitude, 1)
            if (addressLocations.size > 0) {
                if (!binding.saveLocationButton.isEnabled) {
                    binding.saveLocationButton.isEnabled = true
                }
                updatePositionArea(PointOfInterest(it, null, addressLocations[0].getAddressLine(0)))
            }
        }

        googleMap.setOnPoiClickListener {
            if (!binding.saveLocationButton.isEnabled) {
                binding.saveLocationButton.isEnabled = true
            }
            updatePositionArea(it)
        }

        val setMapStyle = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style))
        Log.d(TAG, "SelectLocationFragment.setMapStyle: $setMapStyle")

        getAndSetCurrentLocation()
    }

    private fun updatePositionArea(poi: PointOfInterest) {
        googleMap.clear()
        pointOfInterest = poi
        val pointMarker = googleMap.addMarker(MarkerOptions().position(poi.latLng).title(poi.name))
        pointMarker?.showInfoWindow()
        googleMap.addCircle(CircleOptions().center(poi.latLng).radius(25.0).fillColor(resources.getColor(R.color.colorAccent)))
    }

    private fun onLocationSelected() {
        _viewModel.selectedPOI.value = pointOfInterest
        _viewModel.reminderSelectedLocationStr.value = pointOfInterest.name
        _viewModel.latitude.value = pointOfInterest.latLng.latitude
        _viewModel.longitude.value = pointOfInterest.latLng.longitude
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }

    private fun areLocationPermissionsEnabled(): Boolean = ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    private fun shouldAskForPermission(): Boolean = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)

    @SuppressLint("MissingPermission")
    private fun getAndSetCurrentLocation() {
        if (areLocationPermissionsEnabled()) {
            googleMap.isMyLocationEnabled = true
            manageLocationServiceStatus(true)
        } else if (shouldAskForPermission()) {
            Snackbar.make(requireActivity().findViewById(android.R.id.content), R.string.select_location, Snackbar.LENGTH_LONG)
                .setAction(android.R.string.ok) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), FINE_LOCATION_PERMISSION_REQUEST)
                }
                .show()
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), FINE_LOCATION_PERMISSION_REQUEST)
        }
    }

    @SuppressLint("MissingPermission")
    private fun manageLocationServiceStatus(hasPermissions: Boolean) {
        val locationRequest = LocationSettingsRequest.Builder().addLocationRequest(LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }).build()
        LocationServices.getSettingsClient(requireActivity()).checkLocationSettings(locationRequest).apply {
            addOnFailureListener {
                if (it is ResolvableApiException && hasPermissions) {
                    startIntentSenderForResult(it.resolution.intentSender, CHANGE_LOCATION_REQUEST, null, 0, 0, 0, null)
                }
            }
            addOnCompleteListener {
                val permissionsNowEnabled = areLocationPermissionsEnabled()
                val succeed = it.isSuccessful
                Log.d(TAG, "checkLocationServiceStatus: succeed= $succeed   permissionsNowEnabled= $permissionsNowEnabled")
                if (succeed && permissionsNowEnabled) {
                    try {
                        locationProvider.lastLocation.addOnSuccessListener { location: Location? ->
                            if (location != null) {
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), DEFAULT_ZOOM_LEVEL))
                                retriesSoFar = 0
                            } else {
                                if (retriesSoFar < MAX_RETRIES) {
                                    retriesSoFar++
                                    mHandler.postDelayed({manageLocationServiceStatus(true)},1250)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "checkLocationServiceStatus: ${e.message}")
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
            Log.d(TAG, "onRequestPermissionsResult: Permission granted")
            getAndSetCurrentLocation()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (CHANGE_LOCATION_REQUEST == requestCode) {
            manageLocationServiceStatus(false)
        }
    }

    companion object {
        private const val TAG = "SelectLocationFragment"
        private const val FINE_LOCATION_PERMISSION_REQUEST = 1011
        private const val CHANGE_LOCATION_REQUEST = 1012
        private const val DEFAULT_ZOOM_LEVEL = 18F
        private const val MAX_RETRIES = 3
    }
}
