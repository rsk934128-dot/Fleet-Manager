package com.example.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.camera.CameraHelper
import com.example.data.model.CameraSnapshot
import com.example.data.model.Device
import com.example.ui.fleet.FleetViewModel
import com.example.ui.fleet.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RemoteCameraScreen(
    viewModel: FleetViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val devicesState by viewModel.devices.collectAsStateWithLifecycle()
    val snapshotsState by viewModel.cameraSnapshots.collectAsStateWithLifecycle()

    var selectedDevice by remember { mutableStateOf<Device?>(null) }
    var selectedSnapshotForDetail by remember { mutableStateOf<CameraSnapshot?>(null) }
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionGranted = isGranted
    }

    // Camera helper state
    val cameraHelper = remember { CameraHelper(context) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var currentLensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var showLocalCameraView by remember { mutableStateOf(false) }
    var remoteStreamActive by remember { mutableStateOf(false) }
    var captureSuccessBanner by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraHelper.release()
        }
    }

    LaunchedEffect(captureSuccessBanner) {
        if (captureSuccessBanner != null) {
            kotlinx.coroutines.delay(4000)
            captureSuccessBanner = null
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("remote_camera_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Camera Icon",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "রিমোট ক্যামেরা ও নজরদারি",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Remote Surveillance & Live Camera Control",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Notification Banner
        if (captureSuccessBanner != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = captureSuccessBanner ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Section 1: Fleet Device Remote Commander
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ফ্লিট ডিভাইস নির্বাচন (Target Fleet Device)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (remoteStreamActive) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (remoteStreamActive) Color(0xFF2E7D32) else Color(0xFF757575))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (remoteStreamActive) "লাইভ ফিড সক্রিয়" else "স্ট্যান্ডবাই মোড",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remoteStreamActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal device selector chips
                    when (devicesState) {
                        is UiState.Success -> {
                            val deviceList = (devicesState as UiState.Success<List<Device>>).data
                            if (deviceList.isEmpty()) {
                                Text(
                                    text = "কোন ডিভাইস পাওয়া যায়নি। প্রথমে স্যাম্পল ডিভাইস যোগ করুন।",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(deviceList, key = { it.id }) { dev ->
                                        val isSelected = selectedDevice?.id == dev.id || (selectedDevice == null && dev == deviceList.first())
                                        if (selectedDevice == null && isSelected) {
                                            selectedDevice = dev
                                        }

                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedDevice = dev },
                                            label = {
                                                Text(
                                                    text = dev.deviceName.ifBlank { dev.id },
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.PhoneAndroid,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        is UiState.Loading -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ডিভাইস লোড হচ্ছে...", fontSize = 12.sp)
                            }
                        }
                        is UiState.Error -> {
                            Text("ডিভাইস লোড ত্রুটি", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Remote Control Actions
                    val currentTarget = selectedDevice
                    Text(
                        text = "রিমোট কমান্ড ও অ্যাকশন (Remote Camera Commands)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (currentTarget != null) {
                                    viewModel.triggerRemoteSnapshot(
                                        device = currentTarget,
                                        lens = if (currentLensFacing == CameraSelector.LENS_FACING_BACK) "back" else "front",
                                        reason = "Manual Remote Trigger"
                                    )
                                    captureSuccessBanner = "${currentTarget.deviceName} এর জন্য রিমোট স্ন্যাপশট সফলভাবে সম্পন্ন হয়েছে!"
                                }
                            },
                            enabled = currentTarget != null,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("remote_capture_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("স্ন্যাপশট তুলুন", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                remoteStreamActive = !remoteStreamActive
                                if (remoteStreamActive) {
                                    captureSuccessBanner = "লাইভ স্ট্রিম কানেক্টেড: ${currentTarget?.deviceName ?: "Fleet Device"}"
                                }
                            },
                            enabled = currentTarget != null,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                if (remoteStreamActive) Icons.Default.Videocam else Icons.Default.Sensors,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (remoteStreamActive) "ফিড থামান" else "লাইভ ফিড", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Section 2: Local Camera Diagnostic & Viewfinder
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "স্থানীয় ক্যামেরা প্রিভিউ ও অডিট",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Local Camera Viewfinder & Frame Audit",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        OutlinedButton(
                            onClick = { showLocalCameraView = !showLocalCameraView },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (showLocalCameraView) "প্রিভিউ লুকান" else "ক্যামেরা খুলুন", fontSize = 12.sp)
                        }
                    }

                    AnimatedVisibility(visible = showLocalCameraView) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            if (!cameraPermissionGranted) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "ক্যামেরা ব্যবহারের অনুমতি প্রয়োজন",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = "Camera permission is required to run live optical audits and capture surveillance frames.",
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text("অনুমতি দিন (Grant Permission)")
                                        }
                                    }
                                }
                            } else {
                                // Camera Viewfinder Container
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black)
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            PreviewView(ctx).apply {
                                                previewViewRef = this
                                                cameraHelper.startCamera(
                                                    lifecycleOwner = lifecycleOwner,
                                                    previewView = this,
                                                    lensFacing = currentLensFacing
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Viewfinder Top Overlay info
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                            .align(Alignment.TopCenter),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.Black.copy(alpha = 0.65f)
                                        ) {
                                            Text(
                                                text = if (currentLensFacing == CameraSelector.LENS_FACING_BACK) "BACK LENS (পিছনের লেন্স)" else "FRONT LENS (সামনের লেন্স)",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.Black.copy(alpha = 0.65f)
                                        ) {
                                            Text(
                                                text = "1080p • 30 FPS",
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF69F0AE),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Bottom Camera Control Bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                                )
                                            )
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Torch Button
                                            IconButton(
                                                onClick = {
                                                    cameraHelper.toggleTorch { active ->
                                                        isTorchEnabled = active
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.2f))
                                            ) {
                                                Icon(
                                                    imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                                    contentDescription = "Toggle Torch",
                                                    tint = if (isTorchEnabled) Color(0xFFFFD54F) else Color.White
                                                )
                                            }

                                            // Shutter Button
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(CircleShape)
                                                    .border(3.dp, Color.White, CircleShape)
                                                    .background(Color.White.copy(alpha = 0.3f))
                                                    .clickable(enabled = !isCapturing) {
                                                        isCapturing = true
                                                        cameraHelper.captureFrame(
                                                            onSuccess = { base64Data, w, h ->
                                                                isCapturing = false
                                                                val localSnapshot = CameraSnapshot(
                                                                    deviceId = selectedDevice?.id ?: "local-audit-device",
                                                                    deviceName = selectedDevice?.deviceName ?: "Local Auditor",
                                                                    timestamp = System.currentTimeMillis(),
                                                                    cameraFacing = if (currentLensFacing == CameraSelector.LENS_FACING_BACK) "back" else "front",
                                                                    resolution = "${w}x${h}",
                                                                    triggerReason = "Local Diagnostic Capture",
                                                                    base64Data = base64Data,
                                                                    torchActive = isTorchEnabled,
                                                                    notes = "Captured directly via local device camera audit."
                                                                )
                                                                viewModel.saveCameraSnapshot(localSnapshot)
                                                                captureSuccessBanner = "স্থানীয় ক্যামেরা স্ন্যাপশট সফলভাবে সংরক্ষিত হয়েছে!"
                                                            },
                                                            onError = {
                                                                isCapturing = false
                                                            }
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isCapturing) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        color = Color.White,
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(46.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.White)
                                                    )
                                                }
                                            }

                                            // Lens Switcher Button
                                            IconButton(
                                                onClick = {
                                                    previewViewRef?.let { pView ->
                                                        currentLensFacing = cameraHelper.switchLens(lifecycleOwner, pView)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.2f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Cameraswitch,
                                                    contentDescription = "Switch Camera Lens",
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Captured Surveillance Snapshots Gallery
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ক্যাপচার্ড স্ন্যাপশট ও নজরদারি হিস্টোরি",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Surveillance Snapshots & Audit Gallery",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Snapshots list items
        when (snapshotsState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text("Error loading snapshots: ${(snapshotsState as UiState.Error).message}", color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                val snapshots = (snapshotsState as UiState.Success<List<CameraSnapshot>>).data
                if (snapshots.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "কোন স্ন্যাপশট সংরক্ষিত নেই",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "রিমোট কমান্ড বা স্থানীয় ক্যামেরা দিয়ে প্রথম স্ন্যাপশট তুলুন।",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(snapshots, key = { it.id }) { snapshot ->
                        SnapshotCardItem(
                            snapshot = snapshot,
                            onClick = { selectedSnapshotForDetail = snapshot },
                            onDelete = { viewModel.deleteCameraSnapshot(snapshot.id) }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedSnapshotForDetail?.let { snap ->
        SnapshotDetailDialog(
            snapshot = snap,
            onDismiss = { selectedSnapshotForDetail = null },
            onDelete = {
                viewModel.deleteCameraSnapshot(snap.id)
                selectedSnapshotForDetail = null
            }
        )
    }
}

@Composable
private fun SnapshotCardItem(
    snapshot: CameraSnapshot,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(snapshot.timestamp) { dateFormat.format(Date(snapshot.timestamp)) }

    // Decode Base64 if present
    val decodedBitmap = remember(snapshot.base64Data) {
        if (!snapshot.base64Data.isNullOrBlank()) {
            try {
                val bytes = Base64.decode(snapshot.base64Data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("snapshot_card_${snapshot.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail preview or Placeholder Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (decodedBitmap != null) {
                    Image(
                        bitmap = decodedBitmap.asImageBitmap(),
                        contentDescription = "Snapshot Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = snapshot.deviceName.ifBlank { "ডিভাইস: ${snapshot.deviceId}" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (snapshot.cameraFacing == "back") "পিছনের লেন্স" else "সামনের লেন্স",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "ট্রিগার: ${snapshot.triggerReason}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Text(
                        text = snapshot.resolution,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun SnapshotDetailDialog(
    snapshot: CameraSnapshot,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val formattedTime = remember(snapshot.timestamp) { dateFormat.format(Date(snapshot.timestamp)) }

    val decodedBitmap = remember(snapshot.base64Data) {
        if (!snapshot.base64Data.isNullOrBlank()) {
            try {
                val bytes = Base64.decode(snapshot.base64Data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "স্ন্যাপশট অডিট রেকর্ড",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (decodedBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                    ) {
                        Image(
                            bitmap = decodedBitmap.asImageBitmap(),
                            contentDescription = "Full Snapshot Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text("ডিভাইস: ${snapshot.deviceName} (${snapshot.deviceId})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("লেন্স: ${if (snapshot.cameraFacing == "back") "পিছনের ক্যামেরা (Back Camera)" else "সামনের ক্যামেরা (Front Camera)"}", fontSize = 12.sp)
                Text("রেজোলিউশন: ${snapshot.resolution}", fontSize = 12.sp)
                Text("ট্রিগার কারণ: ${snapshot.triggerReason}", fontSize = 12.sp)
                Text("টাইমস্ট্যাম্প: $formattedTime", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

                if (snapshot.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("নোটস: ${snapshot.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ডিলিট করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}
