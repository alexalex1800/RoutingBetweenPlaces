package com.example.multistoprouter

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multistoprouter.R
import com.example.multistoprouter.data.RouteStatus
import com.example.multistoprouter.data.RoutesRepository
import com.example.multistoprouter.location.LocationRepository
import com.example.multistoprouter.ui.MultiStopRouterApp
import com.example.multistoprouter.ui.theme.MultiStopRouterTheme
import com.example.multistoprouter.net.DirectionsApi
import com.example.multistoprouter.net.PlacesTextSearchApi
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val placesClient = providePlacesClient()
        val retrofit = provideRetrofit()
        val routesRepository = RoutesRepository(
            placesClient = placesClient,
            directionsApi = retrofit.create(DirectionsApi::class.java),
            textSearchApi = retrofit.create(PlacesTextSearchApi::class.java)
        )
        val locationRepository = LocationRepository(this)
        val factory = MainViewModelFactory(routesRepository, locationRepository)

        setContent {
            val viewModel: MainViewModel = viewModel(factory = factory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current

            MultiStopRouterTheme {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    if (result.values.any { it }) {
                        viewModel.refreshCurrentLocation()
                    } else {
                        Toast.makeText(context, getString(R.string.request_permission), Toast.LENGTH_SHORT).show()
                    }
                }

                LaunchedEffect(Unit) {
                    if (!locationRepository.hasLocationPermission(context)) {
                        permissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    } else {
                        viewModel.refreshCurrentLocation()
                    }
                }

                LaunchedEffect(uiState.status) {
                    if (uiState.status is RouteStatus.Error) {
                        Toast.makeText(context, (uiState.status as RouteStatus.Error).message, Toast.LENGTH_LONG).show()
                    }
                }

                MultiStopRouterApp(
                    uiState = uiState,
                    onStartQueryChange = viewModel::onStartQueryChange,
                    onStopoverQueryChange = viewModel::onStopoverQueryChange,
                    onDestinationQueryChange = viewModel::onDestinationQueryChange,
                    onStartSuggestionSelected = viewModel::onStartSuggestionSelected,
                    onStopoverSuggestionSelected = viewModel::onStopoverSuggestionSelected,
                    onDestinationSuggestionSelected = viewModel::onDestinationSuggestionSelected,
                    onTravelModeChange = viewModel::onTravelModeChange
                )
            }
        }
    }

    private fun providePlacesClient(): PlacesClient {
        if (!Places.isInitialized()) {
            // TODO: Supply a valid API key via local.properties using MAPS_API_KEY.
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }
        return Places.createClient(this)
    }

    private fun provideRetrofit(): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        val moshi = Moshi.Builder().build()
        return Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
}
