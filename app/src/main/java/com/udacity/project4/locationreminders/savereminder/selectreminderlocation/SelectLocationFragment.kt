package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var locationProvider: FusedLocationProviderClient
    private lateinit var pointOfInterest: PointOfInterest

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        checkForLocationPermissions()

        locationProvider = LocationServices.getFusedLocationProviderClient(requireContext())

        val mapsFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapsFragment.getMapAsync(this)

        binding.saveLocationButton.isEnabled = false
        binding.saveLocationButton.setOnClickListener {
            onLocationSelected()
        }


        return binding.root
    }

    override fun onMapReady(p0: GoogleMap?) {
        p0?.let {
            googleMap = it

            val setMapStyle = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style)
            )
            Log.d(TAG, "SelectLocationFragment.setMapStyle: $setMapStyle")

            googleMap.setOnPoiClickListener { poi ->
                googleMap.clear()
                pointOfInterest = poi
                val marker = googleMap.addMarker(MarkerOptions().position(poi.latLng).title(poi.name))
                marker.showInfoWindow()
                if (!binding.saveLocationButton.isEnabled) {
                    binding.saveLocationButton.isEnabled = true
                }
            }

            getAndSetCurrentLocation()
        }
    }

    private fun onLocationSelected() {
        _viewModel.selectedPOI.value = pointOfInterest
        _viewModel.reminderSelectedLocationStr.value = pointOfInterest.name
        _viewModel.latitude.value = pointOfInterest.latLng.latitude
        _viewModel.longitude.value = pointOfInterest.latLng.longitude
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }

    private fun checkForLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), FINE_LOCATION_PERMISSION_REQUEST)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getAndSetCurrentLocation() {
        if (!googleMap.isMyLocationEnabled) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), FINE_LOCATION_PERMISSION_REQUEST)
            return
        }

        locationProvider.lastLocation.addOnSuccessListener {
            val newCameraLatLng = LatLng(it.latitude, it.longitude)
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(newCameraLatLng, DEFAULT_ZOOM_LEVEL)
            )
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
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "SelectLocationFragment"
        private const val FINE_LOCATION_PERMISSION_REQUEST = 1011
        private const val DEFAULT_ZOOM_LEVEL = 12F
    }
}
