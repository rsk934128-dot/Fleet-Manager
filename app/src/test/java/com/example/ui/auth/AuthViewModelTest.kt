package com.example.ui.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AuthenticationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthViewModelTest {

  private lateinit var context: Context
  private lateinit var authManager: AuthenticationManager
  private lateinit var viewModel: AuthViewModel

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(
        context,
        FirebaseOptions.Builder()
          .setApplicationId("com.example")
          .setProjectId("ai-studio-test")
          .setApiKey("fake-api-key-test")
          .build()
      )
    }
    authManager = AuthenticationManager(context)
    viewModel = AuthViewModel(authManager)
  }

  @Test
  fun initialStates_areCorrect() {
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.errorMessage.value)
    assertNotNull(viewModel.authManager)
  }

  @Test
  fun clearError_resetsErrorMessage() {
    viewModel.clearError()
    assertNull(viewModel.errorMessage.value)
  }

  @Test
  fun factory_createsAuthViewModelInstance() {
    val factory = AuthViewModel.Factory(authManager)
    val createdVm = factory.create(AuthViewModel::class.java)
    assertNotNull(createdVm)
    assertFalse(createdVm.isLoading.value)
  }
}
