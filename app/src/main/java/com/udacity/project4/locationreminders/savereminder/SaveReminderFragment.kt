package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.isQorHigher
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var reminderDataItem: ReminderDataItem
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java).apply {
            action = GeofenceBroadcastReceiver.ACTION_SAVED_GEOFENCE_BR
        }
        val intentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(requireContext(), 0, intent, intentFlags)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            reminderDataItem = ReminderDataItem(title, description, location, latitude, longitude)

            if(_viewModel.validateEnteredData(reminderDataItem)) {
                checkAllPermissions()
            }
        }
    }

    private fun checkAllPermissions() {
        val arePermissionsGranted = arePermissionsGranted()
        Log.d(TAG, "checkAllPermissions: $arePermissionsGranted")
        if (arePermissionsGranted) {
            manageLocationServiceStatus(arePermissionsGranted)
        } else {
            requestLocationPermissions()
        }
    }

    private fun arePermissionsGranted() : Boolean {
        val isLocationPermissionGranted =
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val isBackgroundPermissionGranted =
            if (isQorHigher()) {
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        return isLocationPermissionGranted && isBackgroundPermissionGranted
    }

    private fun requestLocationPermissions() {
        if (arePermissionsGranted()) {return}
        val permissionsNeeded = if (isQorHigher()) {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val requestCode = if (isQorHigher()) {
            PERMISSION_FINE_LOCATION_AND_BACKGROUND_REQUEST_CODE
        } else {
            PERMISSION_FINE_LOCATION_REQUEST_CODE
        }
        requestPermissions(permissionsNeeded, requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (PERMISSION_FINE_LOCATION_REQUEST_CODE == requestCode
            || PERMISSION_FINE_LOCATION_AND_BACKGROUND_REQUEST_CODE == requestCode) {
            val arePermissionsGrantedNow = grantResults.none {
                PackageManager.PERMISSION_DENIED == it
            }
            if (!arePermissionsGrantedNow) {
                Snackbar.make(requireActivity().findViewById(android.R.id.content), R.string.permissions_not_granted, Snackbar.LENGTH_LONG)
                    .setAction("Allow") {
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }.show()
            } else {
                manageLocationServiceStatus(true)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (CHANGE_LOCATION_REQUEST == requestCode) {
            manageLocationServiceStatus(false)
        }
    }

    private fun manageLocationServiceStatus(permissions: Boolean) {
        val locationRequest = LocationSettingsRequest.Builder().addLocationRequest(LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }).build()
        LocationServices.getSettingsClient(requireActivity()).checkLocationSettings(locationRequest).apply {
            addOnFailureListener {
                if (it is ResolvableApiException && permissions) {
                    startIntentSenderForResult(it.resolution.intentSender, CHANGE_LOCATION_REQUEST, null, 0, 0, 0, null)
                }
            }
            addOnCompleteListener {
                if (it.isSuccessful) {
                    addGeofence()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence() {
        if (!arePermissionsGranted()) {
            requestLocationPermissions()
        }
        if(this::reminderDataItem.isInitialized) {
            val geofence = Geofence.Builder()
                .setRequestId(reminderDataItem.id)
                .setCircularRegion(reminderDataItem.latitude!!, reminderDataItem.longitude!!, 100f)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()
            geofencingClient.addGeofences(request, geofencePendingIntent)?.run {
                addOnSuccessListener {
                    _viewModel.validateAndSaveReminder(reminderDataItem)
                }
                addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Failed to add geofence",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    companion object {
        private const val TAG = "SaveReminderFragment"
        private const val PERMISSION_FINE_LOCATION_REQUEST_CODE = 1101
        private const val PERMISSION_FINE_LOCATION_AND_BACKGROUND_REQUEST_CODE = 1102
        private const val CHANGE_LOCATION_REQUEST = 1012
    }
}
