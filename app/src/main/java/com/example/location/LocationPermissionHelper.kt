package com.example.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Result of a location permission request or check.
 */
data class LocationPermissionResult(
    val fineGranted: Boolean = false,
    val coarseGranted: Boolean = false
) {
    /** True if at least coarse or fine location is granted. */
    val isAnyGranted: Boolean get() = fineGranted || coarseGranted

    /** True if precise (fine) location is granted. */
    val isPrecise: Boolean get() = fineGranted

    /** True if approximate (coarse only) location is granted. */
    val isCoarseOnly: Boolean get() = coarseGranted && !fineGranted

    /** True if all location permissions are denied. */
    val isDenied: Boolean get() = !fineGranted && !coarseGranted
}

/**
 * High-level status of location permissions on the device.
 */
sealed interface LocationPermissionStatus {
    object Denied : LocationPermissionStatus
    object CoarseOnly : LocationPermissionStatus
    object Precise : LocationPermissionStatus
}

/**
 * Helper class for requesting and checking FINE and COARSE location permissions
 * in accordance with Android 12+ (API 31+) location permission guidelines.
 */
class LocationPermissionHelper(private val context: Context) {

    /** Checks whether fine (precise) location permission is granted. */
    fun hasFineLocationPermission(): Boolean =
        hasFineLocationPermission(context)

    /** Checks whether coarse (approximate) location permission is granted. */
    fun hasCoarseLocationPermission(): Boolean =
        hasCoarseLocationPermission(context)

    /** Checks whether any location permission (fine or coarse) is granted. */
    fun hasAnyLocationPermission(): Boolean =
        hasAnyLocationPermission(context)

    /** Checks whether both fine and coarse location permissions are granted. */
    fun hasAllLocationPermissions(): Boolean =
        hasAllLocationPermissions(context)

    /** Returns current [LocationPermissionStatus]. */
    fun getPermissionStatus(): LocationPermissionStatus =
        getPermissionStatus(context)

    /** Returns current granular [LocationPermissionResult]. */
    fun getPermissionResult(): LocationPermissionResult =
        getPermissionResult(context)

    /** Checks if permission rationale should be displayed to the user. */
    fun shouldShowRationale(activity: Activity): Boolean =
        shouldShowRationale(activity)

    /** Creates an Intent to navigate the user directly to the app's system settings. */
    fun createAppSettingsIntent(): Intent =
        createAppSettingsIntent(context)

    /** Requests fine and coarse location permissions using an [ActivityResultLauncher]. */
    fun requestPermissions(launcher: ActivityResultLauncher<Array<String>>) {
        requestPermissionsStatic(launcher)
    }

    /** Requests permissions via classic [ActivityCompat.requestPermissions]. */
    fun requestPermissions(activity: Activity, requestCode: Int = REQUEST_CODE_LOCATION) {
        requestPermissionsStatic(activity, requestCode)
    }

    companion object {
        const val REQUEST_CODE_LOCATION = 1001

        /**
         * The standard location permissions that should be requested together
         * to allow users to select either approximate or precise location.
         */
        val REQUIRED_PERMISSIONS: Array<String> = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        /**
         * Checks if ACCESS_FINE_LOCATION is granted.
         */
        fun hasFineLocationPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Checks if ACCESS_COARSE_LOCATION is granted.
         */
        fun hasCoarseLocationPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Checks if either fine or coarse location is granted.
         */
        fun hasAnyLocationPermission(context: Context): Boolean {
            return hasFineLocationPermission(context) || hasCoarseLocationPermission(context)
        }

        /**
         * Checks if both fine and coarse location permissions are granted.
         */
        fun hasAllLocationPermissions(context: Context): Boolean {
            return hasFineLocationPermission(context) && hasCoarseLocationPermission(context)
        }

        /**
         * Returns the current [LocationPermissionStatus] for the given [Context].
         */
        fun getPermissionStatus(context: Context): LocationPermissionStatus {
            return when {
                hasFineLocationPermission(context) -> LocationPermissionStatus.Precise
                hasCoarseLocationPermission(context) -> LocationPermissionStatus.CoarseOnly
                else -> LocationPermissionStatus.Denied
            }
        }

        /**
         * Returns a [LocationPermissionResult] snapshot reflecting currently granted permissions.
         */
        fun getPermissionResult(context: Context): LocationPermissionResult {
            return LocationPermissionResult(
                fineGranted = hasFineLocationPermission(context),
                coarseGranted = hasCoarseLocationPermission(context)
            )
        }

        /**
         * Evaluates whether the UI should show an educational rationale
         * for fine or coarse location permission.
         */
        fun shouldShowRationale(activity: Activity): Boolean {
            return ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        /**
         * Parses the result map from an [ActivityResultContracts.RequestMultiplePermissions] callback.
         */
        fun parsePermissionResult(resultMap: Map<String, Boolean>): LocationPermissionResult {
            val fine = resultMap[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarse = resultMap[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            return LocationPermissionResult(fineGranted = fine, coarseGranted = coarse)
        }

        /**
         * Launches the permission request dialog with both fine and coarse permissions.
         */
        fun requestPermissionsStatic(launcher: ActivityResultLauncher<Array<String>>) {
            launcher.launch(REQUIRED_PERMISSIONS)
        }

        /**
         * Requests permissions using traditional Activity request code.
         */
        fun requestPermissionsStatic(activity: Activity, requestCode: Int = REQUEST_CODE_LOCATION) {
            ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, requestCode)
        }

        /**
         * Creates an Intent to navigate the user directly to the app details settings screen.
         */
        fun createAppSettingsIntent(context: Context): Intent {
            return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}

/**
 * Composable helper that remembers an [ActivityResultLauncher] configured for
 * requesting fine and coarse location permissions and dispatches a structured [LocationPermissionResult].
 *
 * Example usage:
 * ```
 * val locationLauncher = rememberLocationPermissionLauncher { result ->
 *     when {
 *         result.isPrecise -> { /* Handle precise GPS */ }
 *         result.isCoarseOnly -> { /* Handle approximate location */ }
 *         else -> { /* Handle denied */ }
 *     }
 * }
 * Button(onClick = { LocationPermissionHelper.requestPermissionsStatic(locationLauncher) }) {
 *     Text("Enable Location")
 * }
 * ```
 */
@Composable
fun rememberLocationPermissionLauncher(
    onResult: (LocationPermissionResult) -> Unit
): ActivityResultLauncher<Array<String>> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val result = LocationPermissionHelper.parsePermissionResult(permissions)
        onResult(result)
    }
}
