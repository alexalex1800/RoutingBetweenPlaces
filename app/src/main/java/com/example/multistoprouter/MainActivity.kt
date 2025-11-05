package com.example.multistoprouter

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.multistoprouter.location.LocationProvider
import com.example.multistoprouter.net.NetworkModule
import com.example.multistoprouter.repository.DirectionsRepository
import com.example.multistoprouter.repository.PlacesRepository
import com.example.multistoprouter.repository.RoutePlanner
import com.example.multistoprouter.ui.MainScreen
import com.example.multistoprouter.ui.theme.MultiStopRouterTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.libraries.places.api.Places
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val placesRepository by lazy {
        if (!Places.isInitialized()) {
            // TODO: Replace with a secure key injection strategy for production.
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY, Locale.getDefault())
        }
        PlacesRepository(Places.createClient(this))
    }

    private val routePlanner by lazy {
        val directionsRepository = DirectionsRepository(NetworkModule.provideDirectionsApi(BuildConfig.DEBUG))
        RoutePlanner(placesRepository, directionsRepository)
    }

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(placesRepository, routePlanner, BuildConfig.MAPS_API_KEY)
    }

    private val locationProvider by lazy { LocationProvider(this) }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MultiStopRouterTheme {
                val uiState by viewModel.uiState.collectAsState()
                val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

                LaunchedEffect(Unit) {
                    permissionState.launchPermissionRequest()
                }

                LaunchedEffect(permissionState.status) {
                    if (permissionState.status.isGranted) {
                        val location = locationProvider.getCurrentLocation()
                        if (location != null) {
                            viewModel.setCurrentLocation(location)
                        }
                    } else if (permissionState.status.shouldShowRationale) {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.request_permission_rationale),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                uiState.errorMessage?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }

                MainScreen(
                    uiState = uiState,
                    onStartQueryChanged = viewModel::onStartQueryChanged,
                    onStartSuggestionSelected = viewModel::selectStartSuggestion,
                    onViaQueryChanged = viewModel::onViaQueryChanged,
                    onDestinationQueryChanged = viewModel::onDestinationQueryChanged,
                    onDestinationSuggestionSelected = viewModel::selectDestinationSuggestion,
                    onModeSelected = viewModel::onTravelModeSelected,
                    hasLocationPermission = permissionState.status.isGranted
                )
            }
        }
    }
}
