package com.example.ui.fleet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Geofence

@Composable
fun GeofenceDialog(
  onDismiss: () -> Unit,
  onSave: (Geofence) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var latText by remember { mutableStateOf("37.7749") }
  var lngText by remember { mutableStateOf("-122.4194") }
  var radiusMeters by remember { mutableFloatStateOf(500f) }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Configure Geofence Perimeter", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Perimeter Name") },
          placeholder = { Text("e.g. San Francisco HQ") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = latText,
            onValueChange = { latText = it },
            label = { Text("Latitude") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          Spacer(modifier = Modifier.width(8.dp))
          OutlinedTextField(
            value = lngText,
            onValueChange = { lngText = it },
            label = { Text("Longitude") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            singleLine = true
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Radius: ${radiusMeters.toInt()} meters",
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
        Slider(
          value = radiusMeters,
          onValueChange = { radiusMeters = it },
          valueRange = 50f..5000f,
          steps = 99
        )

        if (errorText != null) {
          Text(
            text = errorText!!,
            color = androidx.compose.ui.graphics.Color(0xFFC62828),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val lat = latText.toDoubleOrNull()
          val lng = lngText.toDoubleOrNull()
          if (name.isBlank()) {
            errorText = "Please specify a perimeter name."
            return@Button
          }
          if (lat == null || lat < -90.0 || lat > 90.0) {
            errorText = "Valid latitude is between -90 and 90."
            return@Button
          }
          if (lng == null || lng < -180.0 || lng > 180.0) {
            errorText = "Valid longitude is between -180 and 180."
            return@Button
          }
          onSave(
            Geofence(
              name = name.trim(),
              latitude = lat,
              longitude = lng,
              radiusMeters = radiusMeters.toLong()
            )
          )
        }
      ) {
        Text("Save Perimeter")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
