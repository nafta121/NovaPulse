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
 * Utility class giúp lấy vị trí hiện tại qua FusedLocationProviderClient 
 * và lấy tên thành phố thực tế qua Geocoder.
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
            if (!Geocoder.isPresent()) return formatCityName("TP. Hồ Chí Minh")
            val geocoder = Geocoder(context, Locale.getDefault())

            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val rawName = addresses?.firstOrNull()?.adminArea
                ?: addresses?.firstOrNull()?.locality
                ?: addresses?.firstOrNull()?.subAdminArea
                ?: "Vị trí hiện tại"
            formatCityName(rawName)
        } catch (e: Exception) {
            "Vị trí hiện tại"
        }
    }

    private fun formatCityName(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.contains("Hồ Chí Minh", ignoreCase = true)) {
            return "TP. Hồ Chí Minh"
        }
        if (trimmed.contains("Hà Nội", ignoreCase = true)) {
            return "Hà Nội"
        }
        if (trimmed.contains("Đà Nẵng", ignoreCase = true)) {
            return "Đà Nẵng"
        }
        var cleaned = trimmed
        if (cleaned.startsWith("Thành phố ", ignoreCase = true)) {
            cleaned = cleaned.removePrefix("Thành phố ").removePrefix("thành phố ")
        }
        if (cleaned.startsWith("Tỉnh ", ignoreCase = true)) {
            cleaned = cleaned.removePrefix("Tỉnh ").removePrefix("tỉnh ")
        }
        return if (cleaned.isNotBlank()) cleaned else "Vị trí hiện tại"
    }
}
