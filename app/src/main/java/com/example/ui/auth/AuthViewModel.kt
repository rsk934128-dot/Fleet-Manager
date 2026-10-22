package com.example.ui.auth

import android.app.Activity
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthenticationManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
  val authManager: AuthenticationManager
) : ViewModel() {

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  fun signInWithGoogle(activity: Activity, onSuccess: () -> Unit = {}) {
    if (_isLoading.value) return
    _isLoading.value = true
    _errorMessage.value = null

    viewModelScope.launch {
      try {
        val result = authManager.signInWithGoogle(activity)
        _isLoading.value = false
        result.onSuccess {
          onSuccess()
        }.onFailure { err ->
          if (err !is GetCredentialCancellationException && err !is CancellationException) {
            _errorMessage.value = err.localizedMessage ?: "Sign-in authentication failed"
          }
        }
      } catch (e: CancellationException) {
        _isLoading.value = false
      } catch (e: Exception) {
        _isLoading.value = false
        _errorMessage.value = e.localizedMessage ?: "Sign-in authentication failed"
      }
    }
  }

  fun clearError() {
    _errorMessage.value = null
  }

  class Factory(private val authManager: AuthenticationManager) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return AuthViewModel(authManager) as T
    }
  }
}
