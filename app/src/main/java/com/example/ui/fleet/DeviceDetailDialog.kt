package com.example.ui.fleet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Device
import com.example.data.model.SecurityLog

@Composable
fun DeviceDetailDialog(
  device: Device,
  onDismiss: () -> Unit,
  onDelete: (String) -> Unit,
  onTriggerAlert: (SecurityLog) -> Unit,
  onOpenRemoteCamera: ((Device) -> Unit)? = null
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.PhoneAndroid,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = device.deviceName,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            if (device.model.isNotBlank()) {
              Text(
                text = device.model,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close dialog")
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
      ) {
        // Status & Compliance Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text("Connection", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            Text(
              text = device.status.replaceFirstChar { it.uppercase() },
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = when (device.status) {
                "online" -> Color(0xFF2E7D32)
                "warning" -> Color(0xFFE65100)
                else -> Color(0xFFC62828)
              }
            )
          }

          Column(horizontalAlignment = Alignment.End) {
            Text("Compliance", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = when (device.complianceStatus) {
                "compliant" -> Color(0xFFE8F5E9)
                "violation" -> Color(0xFFFFEBEE)
                else -> Color(0xFFFFF3E0)
              }
            ) {
              Text(
                text = device.complianceStatus.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = when (device.complianceStatus) {
                  "compliant" -> Color(0xFF2E7D32)
                  "violation" -> Color(0xFFC62828)
                  else -> Color(0xFFE65100)
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Battery Gauge
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (device.batteryLevel <= 20) Icons.Default.BatteryAlert else Icons.Default.BatteryFull,
              contentDescription = null,
              tint = if (device.batteryLevel <= 20) Color(0xFFC62828) else Color(0xFF2E7D32),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Battery Level", fontSize = 13.sp, fontWeight = FontWeight.Medium)
          }
          Text("${device.batteryLevel}%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        LinearProgressIndicator(
          progress = { (device.batteryLevel.coerceIn(0L, 100L) / 100f) },
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = if (device.batteryLevel <= 20) Color(0xFFC62828) else Color(0xFF2E7D32),
          trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // CPU Usage
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Memory,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("CPU Load", fontSize = 13.sp, fontWeight = FontWeight.Medium)
          }
          Text("${device.cpuUsage}%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        LinearProgressIndicator(
          progress = { (device.cpuUsage.coerceIn(0L, 100L) / 100f) },
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = if (device.cpuUsage > 80) Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // RAM Usage
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("RAM Allocation", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
          Text(
            "${device.ramUsageMb} MB / ${device.ramTotalMb} MB",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Device Coordinates
        if (device.lastLatitude != 0.0 || device.lastLongitude != 0.0) {
          Text("Last Location", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
          Text(
            text = "Lat: ${"%.4f".format(device.lastLatitude)}, Lng: ${"%.4f".format(device.lastLongitude)}",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    },
    confirmButton = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        OutlinedButton(
          onClick = {
            onTriggerAlert(
              SecurityLog(
                deviceId = device.id,
                deviceName = device.deviceName,
                severity = "warning",
                eventType = "policy-check",
                description = "Manual compliance diagnostic check logged by administrator."
              )
            )
          },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
          Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Audit Log", fontSize = 12.sp)
        }

        if (onOpenRemoteCamera != null) {
          OutlinedButton(
            onClick = {
              onOpenRemoteCamera(device)
              onDismiss()
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
          ) {
            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Camera", fontSize = 12.sp)
          }
        }

        Button(
          onClick = { onDelete(device.id) },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
          )
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Delete", fontSize = 12.sp)
        }
      }
    }
  )
}
