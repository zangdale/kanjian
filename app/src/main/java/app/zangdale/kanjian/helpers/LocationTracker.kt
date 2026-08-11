package app.zangdale.kanjian.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import android.Manifest

class LocationTracker(
    private val context: Context,
) {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    var lastLocation: Location? = null
        private set

    private val listener = LocationListener { location ->
        lastLocation = location
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (!hasLocationPermission()) {
            return
        }

        val provider = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
        ).firstOrNull { locationManager.isProviderEnabled(it) } ?: return

        lastLocation = locationManager.getLastKnownLocation(provider)
            ?: listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .firstNotNullOfOrNull { candidate ->
                    if (locationManager.isProviderEnabled(candidate)) {
                        locationManager.getLastKnownLocation(candidate)
                    } else {
                        null
                    }
                }

        locationManager.requestLocationUpdates(
            provider,
            UPDATE_INTERVAL_MS,
            MIN_DISTANCE_METERS,
            listener,
            Looper.getMainLooper(),
        )
    }

    fun stop() {
        if (!hasLocationPermission()) {
            return
        }

        locationManager.removeUpdates(listener)
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val UPDATE_INTERVAL_MS = 1000L
        private const val MIN_DISTANCE_METERS = 0f
    }
}
