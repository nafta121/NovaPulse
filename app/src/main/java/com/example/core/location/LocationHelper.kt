package com.example.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Utility class for retrieving the current location via FusedLocationProviderClient
 * and resolving the city name via Geocoder.
 */
object LocationHelper {

    fun isLocationPermissionGranted(context: Context): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return coarse || fine
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? =
        suspendCancellableCoroutine { continuation ->
            if (!isLocationPermissionGranted(context)) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            if (continuation.isActive) continuation.resume(Pair(location.latitude, location.longitude))
                        } else {
                            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                                if (continuation.isActive) {
                                    continuation.resume(lastLoc?.let { Pair(it.latitude, it.longitude) })
                                }
                            }.addOnFailureListener {
                                if (continuation.isActive) continuation.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (continuation.isActive) {
                                continuation.resume(lastLoc?.let { Pair(it.latitude, it.longitude) })
                            }
                        }.addOnFailureListener {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    }
            } catch (e: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    fun getCityName(context: Context, lat: Double, lon: Double): String {
        return try {
            if (!Geocoder.isPresent()) return formatCityName("Ho Chi Minh City")
            val geocoder = Geocoder(context, Locale.getDefault())

            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val rawName = addresses?.firstOrNull()?.adminArea
                ?: addresses?.firstOrNull()?.locality
                ?: addresses?.firstOrNull()?.subAdminArea
                ?: "Current Location"
            formatCityName(rawName)
        } catch (e: Exception) {
            "Current Location"
        }
    }

    private fun formatCityName(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.contains("Ho Chi Minh", ignoreCase = true)) {
            return "Ho Chi Minh City"
        }
        if (trimmed.contains("Ha Noi", ignoreCase = true) || trimmed.contains("Hanoi", ignoreCase = true)) {
            return "Hanoi"
        }
        if (trimmed.contains("Da Nang", ignoreCase = true)) {
            return "Da Nang"
        }
        var cleaned = trimmed
        if (cleaned.startsWith("City ", ignoreCase = true)) {
            cleaned = cleaned.removePrefix("City ").removePrefix("city ")
        }
        if (cleaned.startsWith("Province ", ignoreCase = true)) {
            cleaned = cleaned.removePrefix("Province ").removePrefix("province ")
        }
        return if (cleaned.isNotBlank()) cleaned else "Current Location"
    }
}
