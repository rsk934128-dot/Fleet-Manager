package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.AuthenticationManager
import com.example.data.repository.FleetRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.fleet.FleetHomeScreen
import com.example.ui.fleet.FleetViewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          AppNavigation()
        }
      }
    }
  }
}

@Composable
fun AppNavigation() {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val authManager = remember { AuthenticationManager(context) }
  val currentUser by authManager.authStateFlow.collectAsStateWithLifecycle(initialValue = authManager.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(
      authManager = authManager,
      onAuthSuccess = { /* Handled reactively by authStateFlow */ }
    )
  } else {
    val databaseId = context.getString(R.string.firestore_database_id)
    val viewModel: FleetViewModel = viewModel(
      key = user.uid,
      factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
          val db = FirebaseFirestore.getInstance(databaseId)
          return FleetViewModel(FleetRepository(db), user.uid) as T
        }
      }
    )

    FleetHomeScreen(
      viewModel = viewModel,
      onSignOut = {
        coroutineScope.launch {
          authManager.signOut()
        }
      }
    )
  }
}
