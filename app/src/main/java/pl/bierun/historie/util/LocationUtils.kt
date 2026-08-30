package pl.bierun.historie.util

import android.location.Location
import com.google.android.gms.maps.model.LatLng

object LocationUtils {
    fun calculateDistance(point1: LatLng, point2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0]
    }
}
