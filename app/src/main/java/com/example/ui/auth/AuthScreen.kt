package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.AuthenticationManager

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier,
  authManager: AuthenticationManager? = null,
  viewModel: AuthViewModel? = null
) {
  val context = LocalContext.current
  val manager = authManager ?: remember { AuthenticationManager(context) }
  val authViewModel: AuthViewModel = viewModel ?: viewModel(
    factory = AuthViewModel.Factory(manager)
  )
  val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
  val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("auth_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = "Fleet Security Logo",
              modifier = Modifier.size(36.dp),
              tint = MaterialTheme.colorScheme.primary
            )
          }

          Spacer(modifier = Modifier.height(20.dp))

          Text(
            text = "Fleet Manager",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Enterprise Device Diagnostics & Security Monitoring Portal",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(32.dp))

          if (errorMessage != null) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
            ) {
              Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 13.sp,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center
              )
            }
          }

          Button(
            onClick = {
              val activity = context.findActivity()
              if (activity != null) {
                authViewModel.signInWithGoogle(activity) {
                  onAuthSuccess()
                }
              }
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("google_sign_in_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            )
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
              )
            } else {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "Sign in with Google",
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 15.sp
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "Secured with Firebase Authentication and Firestore",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}

private fun Context.findActivity(): Activity? {
  var currentContext = this
  while (currentContext is ContextWrapper) {
    if (currentContext is Activity) {
      return currentContext
    }
    currentContext = currentContext.baseContext
  }
  return null
}

