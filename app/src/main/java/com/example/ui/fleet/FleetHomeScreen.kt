package com.example.ui.fleet

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.SettingsInputAntenna
import com.example.data.model.Device
import com.example.data.model.Geofence
import com.example.data.model.SecurityLog
import com.example.ui.camera.RemoteCameraScreen
import com.example.ui.service.DeviceMonitorScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetHomeScreen(
  viewModel: FleetViewModel,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier
) {
  val devicesState by viewModel.devices.collectAsStateWithLifecycle()
  val securityLogsState by viewModel.securityLogs.collectAsStateWithLifecycle()
  val geofencesState by viewModel.geofences.collectAsStateWithLifecycle()
  val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  var selectedTab by remember { mutableIntStateOf(0) }
  var selectedDeviceForDetails by remember { mutableStateOf<Device?>(null) }
  var showAddGeofenceDialog by remember { mutableStateOf(false) }

  // Search & Filter States
  var deviceSearchQuery by remember { mutableStateOf("") }
  var statusFilter by remember { mutableStateOf("all") }
  var logSearchQuery by remember { mutableStateOf("") }
  var severityFilter by remember { mutableStateOf("all") }

  LaunchedEffect(actionMessage) {
    actionMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearActionMessage()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              "Fleet Manager",
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFFE8F5E9),
              modifier = Modifier.padding(top = 2.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CloudDone,
                  contentDescription = null,
                  tint = Color(0xFF2E7D32),
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  "Firestore Live",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF2E7D32)
                )
              }
            }
          }
        },
        actions = {
          IconButton(
            onClick = { viewModel.populateSampleFleet() },
            modifier = Modifier.testTag("populate_fleet_button")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Load Sample Fleet", tint = MaterialTheme.colorScheme.primary)
          }
          IconButton(
            onClick = onSignOut,
            modifier = Modifier.testTag("sign_out_button")
          ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out", tint = MaterialTheme.colorScheme.onSurface)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    floatingActionButton = {
      if (selectedTab == 2) {
        FloatingActionButton(
          onClick = { showAddGeofenceDialog = true },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add Geofence")
        }
      }
    },
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("Devices", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
          icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Security Logs", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
          icon = { Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = { Text("Geofences", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
          icon = { Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        Tab(
          selected = selectedTab == 3,
          onClick = { selectedTab = 3 },
          text = { Text("Remote Camera", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
          icon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        Tab(
          selected = selectedTab == 4,
          onClick = { selectedTab = 4 },
          text = { Text("Monitor Agent", fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal) },
          icon = { Icon(Icons.Default.SettingsInputAntenna, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
      }

      when (selectedTab) {
        0 -> {
          DevicesTabContent(
            devicesState = devicesState,
            searchQuery = deviceSearchQuery,
            onSearchChange = { deviceSearchQuery = it },
            statusFilter = statusFilter,
            onStatusFilterChange = { statusFilter = it },
            onSelectDevice = { selectedDeviceForDetails = it },
            onPopulateSample = { viewModel.populateSampleFleet() }
          )
        }
        1 -> {
          SecurityLogsTabContent(
            logsState = securityLogsState,
            searchQuery = logSearchQuery,
            onSearchChange = { logSearchQuery = it },
            severityFilter = severityFilter,
            onSeverityFilterChange = { severityFilter = it }
          )
        }
        2 -> {
          GeofencesTabContent(
            geofencesState = geofencesState,
            onAddGeofenceClick = { showAddGeofenceDialog = true }
          )
        }
        3 -> {
          RemoteCameraScreen(
            viewModel = viewModel
          )
        }
        4 -> {
          DeviceMonitorScreen()
        }
      }
    }
  }

  // Dialogs
  selectedDeviceForDetails?.let { dev ->
    DeviceDetailDialog(
      device = dev,
      onDismiss = { selectedDeviceForDetails = null },
      onDelete = { id ->
        viewModel.deleteDevice(id)
        selectedDeviceForDetails = null
      },
      onTriggerAlert = { log ->
        viewModel.addSecurityLog(log)
      },
      onOpenRemoteCamera = {
        selectedTab = 3
        selectedDeviceForDetails = null
      }
    )
  }

  if (showAddGeofenceDialog) {
    GeofenceDialog(
      onDismiss = { showAddGeofenceDialog = false },
      onSave = { geofence ->
        viewModel.saveGeofence(geofence)
        showAddGeofenceDialog = false
      }
    )
  }
}

@Composable
private fun DevicesTabContent(
  devicesState: UiState<List<Device>>,
  searchQuery: String,
  onSearchChange: (String) -> Unit,
  statusFilter: String,
  onStatusFilterChange: (String) -> Unit,
  onSelectDevice: (Device) -> Unit,
  onPopulateSample: () -> Unit
) {
  when (devicesState) {
    is UiState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }
    is UiState.Error -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Error: ${devicesState.message}", color = MaterialTheme.colorScheme.error)
      }
    }
    is UiState.Success -> {
      val allDevices = devicesState.data
      val filteredDevices = allDevices.filter { dev ->
        val matchesQuery = searchQuery.isBlank() ||
            dev.deviceName.contains(searchQuery, ignoreCase = true) ||
            dev.model.contains(searchQuery, ignoreCase = true) ||
            dev.id.contains(searchQuery, ignoreCase = true)
        val matchesStatus = when (statusFilter) {
          "online" -> dev.status == "online"
          "offline" -> dev.status == "offline"
          "violations" -> dev.complianceStatus == "violation"
          else -> true
        }
        matchesQuery && matchesStatus
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Fleet Compliance Overview Card
        item {
          val total = allDevices.size
          val violations = allDevices.count { it.complianceStatus == "violation" }
          val isClean = violations == 0

          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isClean) Color(0xFFF1F8E9) else Color(0xFFFFEBEE)
            )
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Fleet Compliance Overview",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isClean) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                Text(
                  text = if (isClean) "All $total managed devices compliant" else "$violations of $total devices require attention",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              Surface(
                shape = CircleShape,
                color = if (isClean) Color(0xFF2E7D32) else Color(0xFFC62828)
              ) {
                Text(
                  text = if (isClean) "100%" else "${((total - violations) * 100 / total.coerceAtLeast(1))}%",
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }
          }
        }

        // Search bar
        item {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by name, model, or ID...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
          )
        }

        // Filter chips
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = statusFilter == "all",
              onClick = { onStatusFilterChange("all") },
              label = { Text("All (${allDevices.size})") }
            )
            FilterChip(
              selected = statusFilter == "online",
              onClick = { onStatusFilterChange("online") },
              label = { Text("Online") }
            )
            FilterChip(
              selected = statusFilter == "offline",
              onClick = { onStatusFilterChange("offline") },
              label = { Text("Offline") }
            )
            FilterChip(
              selected = statusFilter == "violations",
              onClick = { onStatusFilterChange("violations") },
              label = { Text("Violations") }
            )
          }
        }

        if (filteredDevices.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No devices found.", color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onPopulateSample) {
                  Text("Add Sample Devices")
                }
              }
            }
          }
        } else {
          items(filteredDevices, key = { it.id }) { device ->
            DeviceCardItem(device = device, onClick = { onSelectDevice(device) })
          }
        }
      }
    }
  }
}

@Composable
private fun DeviceCardItem(device: Device, onClick: () -> Unit) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("device_card_${device.id}"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Status indicator dot
        Box(
          modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(
              when (device.status) {
                "online" -> Color(0xFF2E7D32)
                "warning" -> Color(0xFFE65100)
                else -> Color(0xFFC62828)
              }
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = device.deviceName,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = device.model.ifBlank { "ID: ${device.id.take(8)}" },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = if (device.batteryLevel <= 20) Icons.Default.BatteryAlert else Icons.Default.BatteryFull,
          contentDescription = null,
          tint = if (device.batteryLevel <= 20) Color(0xFFC62828) else Color(0xFF2E7D32),
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "${device.batteryLevel}%",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

@Composable
private fun SecurityLogsTabContent(
  logsState: UiState<List<SecurityLog>>,
  searchQuery: String,
  onSearchChange: (String) -> Unit,
  severityFilter: String,
  onSeverityFilterChange: (String) -> Unit
) {
  when (logsState) {
    is UiState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }
    is UiState.Error -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Error: ${logsState.message}", color = MaterialTheme.colorScheme.error)
      }
    }
    is UiState.Success -> {
      val allLogs = logsState.data
      val filteredLogs = allLogs.filter { log ->
        val matchesQuery = searchQuery.isBlank() ||
            log.description.contains(searchQuery, ignoreCase = true) ||
            log.deviceName.contains(searchQuery, ignoreCase = true) ||
            log.eventType.contains(searchQuery, ignoreCase = true)
        val matchesSeverity = when (severityFilter) {
          "critical" -> log.severity == "critical"
          "warning" -> log.severity == "warning"
          "info" -> log.severity == "info"
          else -> true
        }
        matchesQuery && matchesSeverity
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        item {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search security logs...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
          )
        }

        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = severityFilter == "all",
              onClick = { onSeverityFilterChange("all") },
              label = { Text("All (${allLogs.size})") }
            )
            FilterChip(
              selected = severityFilter == "critical",
              onClick = { onSeverityFilterChange("critical") },
              label = { Text("Critical") }
            )
            FilterChip(
              selected = severityFilter == "warning",
              onClick = { onSeverityFilterChange("warning") },
              label = { Text("Warning") }
            )
            FilterChip(
              selected = severityFilter == "info",
              onClick = { onSeverityFilterChange("info") },
              label = { Text("Info") }
            )
          }
        }

        if (filteredLogs.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("No security incidents recorded.", color = MaterialTheme.colorScheme.outline)
            }
          }
        } else {
          items(filteredLogs, key = { it.id }) { log ->
            SecurityLogCardItem(log = log)
          }
        }
      }
    }
  }
}

@Composable
private fun SecurityLogCardItem(log: SecurityLog) {
  val (color, bgColor) = when (log.severity) {
    "critical" -> Color(0xFFC62828) to Color(0xFFFFEBEE)
    "warning" -> Color(0xFFE65100) to Color(0xFFFFF3E0)
    else -> Color(0xFF1976D2) to Color(0xFFE3F2FD)
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.Top
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(bgColor),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = when (log.severity) {
            "critical" -> Icons.Default.Warning
            "warning" -> Icons.Default.Warning
            else -> Icons.Default.Info
          },
          contentDescription = null,
          tint = color,
          modifier = Modifier.size(18.dp)
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = log.deviceName.ifBlank { "Device: ${log.deviceId}" },
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = bgColor
          ) {
            Text(
              text = log.severity.uppercase(),
              color = color,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = log.description,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "Event: ${log.eventType}",
          fontFamily = FontFamily.Monospace,
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.outline
        )
      }
    }
  }
}

@Composable
private fun GeofencesTabContent(
  geofencesState: UiState<List<Geofence>>,
  onAddGeofenceClick: () -> Unit
) {
  when (geofencesState) {
    is UiState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }
    is UiState.Error -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Error: ${geofencesState.message}", color = MaterialTheme.colorScheme.error)
      }
    }
    is UiState.Success -> {
      val fences = geofencesState.data
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (fences.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No geofence perimeters configured.", color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onAddGeofenceClick) {
                  Text("Create Geofence")
                }
              }
            }
          }
        } else {
          items(fences, key = { it.id }) { fence ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(fence.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                  Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                      "${fence.radiusMeters}m radius",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  "Center: ${"%.4f".format(fence.latitude)}, ${"%.4f".format(fence.longitude)}",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }
        }
      }
    }
  }
}
