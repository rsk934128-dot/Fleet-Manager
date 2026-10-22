package com.example.location

import android.Manifest
import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocationPermissionHelperTest {

    private lateinit var app: Application
    private lateinit var shadowApp: ShadowApplication
    private lateinit var helper: LocationPermissionHelper

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        shadowApp = shadowOf(app)
        shadowApp.denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        helper = LocationPermissionHelper(app)
    }

    @Test
    fun requiredPermissions_includesFineAndCoarse() {
        assertEquals(2, LocationPermissionHelper.REQUIRED_PERMISSIONS.size)
        assertTrue(LocationPermissionHelper.REQUIRED_PERMISSIONS.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(LocationPermissionHelper.REQUIRED_PERMISSIONS.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    @Test
    fun initialPermissions_areDenied() {
        assertFalse(helper.hasFineLocationPermission())
        assertFalse(helper.hasCoarseLocationPermission())
        assertFalse(helper.hasAnyLocationPermission())
        assertFalse(helper.hasAllLocationPermissions())
        assertEquals(LocationPermissionStatus.Denied, helper.getPermissionStatus())

        val result = helper.getPermissionResult()
        assertTrue(result.isDenied)
        assertFalse(result.isAnyGranted)
        assertFalse(result.isPrecise)
        assertFalse(result.isCoarseOnly)
    }

    @Test
    fun grantCoarsePermission_statusIsCoarseOnly() {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

        assertFalse(helper.hasFineLocationPermission())
        assertTrue(helper.hasCoarseLocationPermission())
        assertTrue(helper.hasAnyLocationPermission())
        assertFalse(helper.hasAllLocationPermissions())
        assertEquals(LocationPermissionStatus.CoarseOnly, helper.getPermissionStatus())

        val result = helper.getPermissionResult()
        assertFalse(result.isDenied)
        assertTrue(result.isAnyGranted)
        assertFalse(result.isPrecise)
        assertTrue(result.isCoarseOnly)
    }

    @Test
    fun grantFinePermission_statusIsPrecise() {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        assertTrue(helper.hasFineLocationPermission())
        assertTrue(helper.hasAnyLocationPermission())
        assertEquals(LocationPermissionStatus.Precise, helper.getPermissionStatus())

        val result = helper.getPermissionResult()
        assertTrue(result.isAnyGranted)
        assertTrue(result.isPrecise)
    }

    @Test
    fun grantBothPermissions_hasAllPermissions() {
        shadowApp.grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        assertTrue(helper.hasFineLocationPermission())
        assertTrue(helper.hasCoarseLocationPermission())
        assertTrue(helper.hasAnyLocationPermission())
        assertTrue(helper.hasAllLocationPermissions())
        assertEquals(LocationPermissionStatus.Precise, helper.getPermissionStatus())
    }

    @Test
    fun parsePermissionResult_mapsCorrectly() {
        val noneGranted = LocationPermissionHelper.parsePermissionResult(emptyMap())
        assertTrue(noneGranted.isDenied)

        val coarseOnly = LocationPermissionHelper.parsePermissionResult(
            mapOf(
                Manifest.permission.ACCESS_FINE_LOCATION to false,
                Manifest.permission.ACCESS_COARSE_LOCATION to true
            )
        )
        assertTrue(coarseOnly.isCoarseOnly)
        assertTrue(coarseOnly.isAnyGranted)
        assertFalse(coarseOnly.isPrecise)

        val bothGranted = LocationPermissionHelper.parsePermissionResult(
            mapOf(
                Manifest.permission.ACCESS_FINE_LOCATION to true,
                Manifest.permission.ACCESS_COARSE_LOCATION to true
            )
        )
        assertTrue(bothGranted.isPrecise)
        assertTrue(bothGranted.isAnyGranted)
        assertFalse(bothGranted.isCoarseOnly)
    }

    @Test
    fun createAppSettingsIntent_targetsApplicationDetailsSettings() {
        val intent = helper.createAppSettingsIntent()
        assertEquals(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertNotNull(intent.data)
        assertEquals("package:${app.packageName}", intent.data.toString())
    }

    @Test
    fun requestPermissions_launchesExpectedPermissions() {
        var launchedPermissions: Array<String>? = null
        val testLauncher = object : ActivityResultLauncher<Array<String>>() {
            override val contract: androidx.activity.result.contract.ActivityResultContract<Array<String>, *>
                get() = throw UnsupportedOperationException()

            override fun launch(input: Array<String>, options: ActivityOptionsCompat?) {
                launchedPermissions = input
            }

            override fun unregister() {}
        }

        helper.requestPermissions(testLauncher)
        assertNotNull(launchedPermissions)
        assertEquals(2, launchedPermissions!!.size)
        assertTrue(launchedPermissions!!.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(launchedPermissions!!.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
    }
}
