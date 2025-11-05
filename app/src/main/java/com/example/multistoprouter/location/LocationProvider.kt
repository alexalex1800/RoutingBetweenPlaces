package com.example.multistoprouter.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationProvider(context: Context) {
    private val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->
        val task = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        cont.invokeOnCancellation { task.cancel() }
        task.addOnSuccessListener { location ->
            if (!cont.isCompleted) cont.resume(location)
        }.addOnFailureListener {
            if (!cont.isCompleted) cont.resume(null)
        }
    }
}
