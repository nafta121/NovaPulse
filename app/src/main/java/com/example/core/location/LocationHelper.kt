package com.example.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.math.abs

/**
 * Utility class for retrieving the real device location via FusedLocationProviderClient,
 * Android LocationManager (GPS/Network/Passive), IP Geolocation fallback, and resolving
 * the city name via Geocoder + reverse-geocoding API fallback.
 */
object LocationHelper {

    private const val PREFS_NAME = "device_location_cache"
    private const val KEY_LAT = "lat"
    private const val KEY_LON = "lon"
    private const val KEY_CITY = "city"
    private const val KEY_COUNTRY = "country_code"
    private const val KEY_SOURCE = "source"
    private const val KEY_TIMESTAMP = "timestamp_ms"

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
    suspend fun getCurrentLocation(context: Context): Pair<Double, Double> = withContext(Dispatchers.IO) {
        // 1. Hardware / Play Services GPS & Network Location if permission is granted
        if (isLocationPermissionGranted(context)) {
            val gpsCoords = fetchHardwareOrFusedLocation(context)
            if (gpsCoords != null) {
                saveCachedLocation(context, gpsCoords.first, gpsCoords.second, source = "GPS")
                return@withContext gpsCoords
            }
        }

        // 2. Cached real GPS coordinates from previous fix
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        readCachedCoords(prefs, requireGps = true)?.let { return@withContext it }

        // 3. Real IP-based Geolocation (works without GPS lock or before permission is granted)
        fetchIpGeolocation(context)?.let { return@withContext it }

        // 4. Cached IP coordinates if offline
        readCachedCoords(prefs, requireGps = false)?.let { return@withContext it }

        // 5. Device TimeZone / Locale fallback
        val tzFallback = getTimezoneDefaultLocation()
        Pair(tzFallback.first, tzFallback.second)
    }

    private fun readCachedCoords(
        prefs: android.content.SharedPreferences,
        requireGps: Boolean
    ): Pair<Double, Double>? {
        if (requireGps && prefs.getString(KEY_SOURCE, "") != "GPS") return null
        val lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull() ?: return null
        val lon = prefs.getString(KEY_LON, null)?.toDoubleOrNull() ?: return null
        return Pair(lat, lon)
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchHardwareOrFusedLocation(context: Context): Pair<Double, Double>? {
        val now = System.currentTimeMillis()
        val freshWindowMs = 30 * 60 * 1000L
        val fusedClient = runCatching { LocationServices.getFusedLocationProviderClient(context) }.getOrNull()
        var fallbackFused: Location? = null

        // 1. Check fast cached Fused lastLocation first (<10ms)
        if (fusedClient != null) {
            val lastFused = withTimeoutOrNull(1500L) {
                suspendCancellableCoroutine<Location?> { cont ->
                    try {
                        fusedClient.lastLocation
                            .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                            .addOnCanceledListener { if (cont.isActive) cont.resume(null) }
                    } catch (_: Exception) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }
            if (lastFused != null) {
                if (now - lastFused.time <= freshWindowMs) {
                    return Pair(lastFused.latitude, lastFused.longitude)
                }
                fallbackFused = lastFused
            }
        }

        // 2. Check Android native LocationManager (GPS, Network, Passive)
        val systemBest = try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            var bestLocation: Location? = null
            if (lm != null) {
                for (provider in providers) {
                    val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
                    if (bestLocation == null || loc.time > bestLocation.time) {
                        bestLocation = loc
                    }
                }
            }
            bestLocation
        } catch (_: Exception) {
            null
        }

        if (systemBest != null && now - systemBest.time <= freshWindowMs) {
            return Pair(systemBest.latitude, systemBest.longitude)
        }

        // 3. Request active location fix if cached locations are stale or missing
        if (fusedClient != null) {
            val freshLoc = withTimeoutOrNull(4000L) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val tokenSource = CancellationTokenSource()
                    cont.invokeOnCancellation { tokenSource.cancel() }
                    try {
                        fusedClient.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            tokenSource.token
                        ).addOnSuccessListener { loc ->
                            if (cont.isActive) cont.resume(loc)
                        }.addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }.addOnCanceledListener {
                            if (cont.isActive) cont.resume(null)
                        }
                    } catch (_: Exception) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }
            if (freshLoc != null) {
                return Pair(freshLoc.latitude, freshLoc.longitude)
            }
        }

        val finalLoc = systemBest ?: fallbackFused
        return finalLoc?.let { Pair(it.latitude, it.longitude) }
    }

    private fun fetchIpGeolocation(context: Context): Pair<Double, Double>? {
        val client = com.example.NovaPulseApp.sharedHttpClient
        val endpoints = listOf(
            "https://ipwho.is/",
            "https://get.geojs.io/v1/ip/geo.json"
        )
        for (url in endpoints) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body?.string() ?: return@use
                    val json = JSONObject(body)
                    if (json.has("success") && !json.optBoolean("success", true)) return@use
                    val lat = json.optString("latitude", "").toDoubleOrNull()
                        ?: json.optDouble("latitude", Double.NaN).takeIf { !it.isNaN() }
                    val lon = json.optString("longitude", "").toDoubleOrNull()
                        ?: json.optDouble("longitude", Double.NaN).takeIf { !it.isNaN() }
                    val city = json.optString("city", "").trim()
                    val country = json.optString("country_code", "").trim().uppercase(Locale.ROOT)
                    if (lat != null && lon != null) {
                        saveCachedLocation(
                            context = context,
                            lat = lat,
                            lon = lon,
                            city = city.takeIf { it.isNotBlank() },
                            countryCode = country.takeIf { it.isNotBlank() },
                            source = "IP"
                        )
                        return Pair(lat, lon)
                    }
                }
            } catch (_: Exception) {
                // Try next endpoint
            }
        }
        return null
    }

    suspend fun getCityName(context: Context, lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        // 1. Try Android Geocoder first
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val addr = addresses?.firstOrNull()
                if (addr != null) {
                    val rawName = addr.locality
                        ?: addr.subAdminArea
                        ?: addr.adminArea
                    val countryCode = addr.countryCode?.uppercase(Locale.ROOT)
                    if (!rawName.isNullOrBlank()) {
                        val formatted = formatCityName(rawName)
                        saveCachedLocation(context, lat, lon, city = formatted, countryCode = countryCode)
                        return@withContext formatted
                    }
                }
            }
        } catch (_: Exception) {
            // Fall through to HTTP reverse geocoding
        }

        // 2. Try BigDataCloud free reverse geocoding API (no API key required)
        try {
            val lang = Locale.getDefault().language.ifBlank { "en" }
            val url = "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$lat&longitude=$lon&localityLanguage=$lang"
            val req = Request.Builder().url(url).get().build()
            com.example.NovaPulseApp.sharedHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val city = json.optString("city", "").ifBlank {
                            json.optString("locality", "").ifBlank {
                                json.optString("principalSubdivision", "")
                            }
                        }
                        val countryCode = json.optString("countryCode", "").uppercase(Locale.ROOT)
                        if (city.isNotBlank()) {
                            val formatted = formatCityName(city)
                            saveCachedLocation(context, lat, lon, city = formatted, countryCode = countryCode.takeIf { it.isNotBlank() })
                            return@withContext formatted
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fall through to cached city
        }

        // 3. Check cached city if coordinates match closely
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedLat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull()
        val cachedLon = prefs.getString(KEY_LON, null)?.toDoubleOrNull()
        val cachedCity = prefs.getString(KEY_CITY, null)
        if (cachedLat != null && cachedLon != null && !cachedCity.isNullOrBlank()) {
            if (abs(lat - cachedLat) < 0.35 && abs(lon - cachedLon) < 0.35) {
                return@withContext formatCityName(cachedCity)
            }
        }

        // 4. Timezone fallback city name
        getTimezoneDefaultLocation().third
    }

    fun getLastKnownCityName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedCity = prefs.getString(KEY_CITY, null)
        if (!cachedCity.isNullOrBlank()) {
            return formatCityName(cachedCity)
        }
        return getTimezoneDefaultLocation().third
    }

    /**
     * Detects the user's real country code (e.g. "ID", "US", "SG", "MY", "VN")
     * from SIM/Network, cached GPS/IP location, TimeZone, or system Locale.
     */
    fun getDetectedCountryCode(context: Context? = null): String {
        if (context != null) {
            try {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                val netCountry = tm?.networkCountryIso?.trim()?.uppercase(Locale.ROOT)
                if (!netCountry.isNullOrBlank() && netCountry.length == 2) return netCountry
                val simCountry = tm?.simCountryIso?.trim()?.uppercase(Locale.ROOT)
                if (!simCountry.isNullOrBlank() && simCountry.length == 2) return simCountry
            } catch (_: Exception) {}

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cachedCountry = prefs.getString(KEY_COUNTRY, null)?.trim()?.uppercase(Locale.ROOT)
            if (!cachedCountry.isNullOrBlank() && cachedCountry.length == 2) return cachedCountry
        }

        val tzId = TimeZone.getDefault().id
        when {
            tzId.startsWith("Asia/Jakarta", ignoreCase = true) ||
                tzId.startsWith("Asia/Pontianak", ignoreCase = true) ||
                tzId.startsWith("Asia/Makassar", ignoreCase = true) ||
                tzId.startsWith("Asia/Jayapura", ignoreCase = true) -> return "ID"
            tzId.startsWith("Asia/Singapore", ignoreCase = true) -> return "SG"
            tzId.startsWith("Asia/Kuala_Lumpur", ignoreCase = true) ||
                tzId.startsWith("Asia/Kuching", ignoreCase = true) -> return "MY"
            tzId.startsWith("Asia/Bangkok", ignoreCase = true) -> return "TH"
            tzId.startsWith("Asia/Manila", ignoreCase = true) -> return "PH"
            tzId.startsWith("Asia/Tokyo", ignoreCase = true) -> return "JP"
            tzId.startsWith("Asia/Seoul", ignoreCase = true) -> return "KR"
            tzId.startsWith("Asia/Ho_Chi_Minh", ignoreCase = true) ||
                tzId.startsWith("Asia/Saigon", ignoreCase = true) -> return "VN"
        }

        val localeCountry = Locale.getDefault().country.uppercase(Locale.ROOT)
        return if (localeCountry.length == 2) localeCountry else "ID"
    }

    fun getTimezoneDefaultLocation(): Triple<Double, Double, String> {
        val tzId = TimeZone.getDefault().id
        return when {
            tzId.equals("Asia/Jakarta", ignoreCase = true) || tzId.equals("Asia/Pontianak", ignoreCase = true) ->
                Triple(-6.2088, 106.8456, "Jakarta")
            tzId.equals("Asia/Makassar", ignoreCase = true) ->
                Triple(-5.1477, 119.4327, "Makassar")
            tzId.equals("Asia/Jayapura", ignoreCase = true) ->
                Triple(-2.5337, 140.7181, "Jayapura")
            tzId.equals("Asia/Singapore", ignoreCase = true) ->
                Triple(1.3521, 103.8198, "Singapore")
            tzId.equals("Asia/Kuala_Lumpur", ignoreCase = true) ->
                Triple(3.1390, 101.6869, "Kuala Lumpur")
            tzId.equals("Asia/Bangkok", ignoreCase = true) ->
                Triple(13.7563, 100.5018, "Bangkok")
            tzId.equals("Asia/Manila", ignoreCase = true) ->
                Triple(14.5995, 120.9842, "Manila")
            tzId.equals("Asia/Tokyo", ignoreCase = true) ->
                Triple(35.6762, 139.6503, "Tokyo")
            tzId.equals("Asia/Seoul", ignoreCase = true) ->
                Triple(37.5665, 126.9780, "Seoul")
            tzId.equals("Asia/Ho_Chi_Minh", ignoreCase = true) || tzId.equals("Asia/Saigon", ignoreCase = true) ->
                Triple(10.8231, 106.6297, "Ho Chi Minh City")
            else -> Triple(-6.2088, 106.8456, "Jakarta")
        }
    }

    private fun saveCachedLocation(
        context: Context,
        lat: Double,
        lon: Double,
        city: String? = null,
        countryCode: String? = null,
        source: String? = null
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putString(KEY_LAT, lat.toString())
            .putString(KEY_LON, lon.toString())
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
        if (!city.isNullOrBlank()) editor.putString(KEY_CITY, city)
        if (!countryCode.isNullOrBlank()) editor.putString(KEY_COUNTRY, countryCode)
        if (!source.isNullOrBlank()) editor.putString(KEY_SOURCE, source)
        editor.apply()
    }

    private fun formatCityName(raw: String): String {
        val trimmed = raw.trim()
        var cleaned = trimmed
            .replace(Regex("^(Kota|Kabupaten|City of|City|Province|Provinsi|Daerah Khusus Ibukota)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(Regency|City|Province)$", RegexOption.IGNORE_CASE), "")
            .trim()
        if (cleaned.equals("DKI Jakarta", ignoreCase = true) || cleaned.contains("Jakarta", ignoreCase = true)) {
            cleaned = trimmed.replace(Regex("^(Daerah Khusus Ibukota|DKI)\\s+", RegexOption.IGNORE_CASE), "").trim()
        }
        return if (cleaned.isNotBlank()) cleaned else "Current Location"
    }
}
