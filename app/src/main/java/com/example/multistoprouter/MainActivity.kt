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
import androidx.preference.PreferenceManager
import com.example.multistoprouter.R
import com.example.multistoprouter.data.RouteStatus
import com.example.multistoprouter.data.RoutesRepository
import com.example.multistoprouter.location.LocationRepository
import com.example.multistoprouter.net.OsrmApi
import com.example.multistoprouter.net.OverpassApi
import com.example.multistoprouter.net.PhotonApi
import com.example.multistoprouter.ui.MultiStopRouterApp
import com.example.multistoprouter.ui.theme.MultiStopRouterTheme
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.osmdroid.config.Configuration
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureOsmdroid()
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val httpClient = provideOkHttpClient()
        val routesRepository = RoutesRepository(
            photonApi = provideRetrofit("https://photon.komoot.io", httpClient, moshi).create(PhotonApi::class.java),
            overpassApi = provideRetrofit("https://overpass-api.de", httpClient, moshi).create(OverpassApi::class.java),
            osrmApi = provideRetrofit("https://router.project-osrm.org", httpClient, moshi).create(OsrmApi::class.java)
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
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
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

    private fun configureOsmdroid() {
        val context = applicationContext
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = packageName
    }

    private fun provideRetrofit(baseUrl: String, client: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }
}
