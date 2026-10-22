package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

/**
 * Helper class managing CameraX initialization, preview binding,
 * lens switching, torch toggling, and capturing frames.
 */
class CameraHelper(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var currentLensFacing = CameraSelector.LENS_FACING_BACK
    private var isTorchOn = false

    companion object {
        private const val TAG = "CameraHelper"
    }

    /**
     * Bind CameraX preview and capture to a given PreviewView and LifecycleOwner.
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
        onError: (Throwable) -> Unit = {}
    ) {
        currentLensFacing = lensFacing
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing camera provider", e)
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(currentLensFacing)
            .build()

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    /**
     * Switch between front and back camera lenses.
     */
    fun switchLens(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ): Int {
        currentLensFacing = if (currentLensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindCameraUseCases(lifecycleOwner, previewView)
        return currentLensFacing
    }

    /**
     * Toggle camera flashlight/torch.
     */
    fun toggleTorch(onSuccess: (Boolean) -> Unit = {}) {
        val control: CameraControl? = camera?.cameraControl
        if (control != null && camera?.cameraInfo?.hasFlashUnit() == true) {
            isTorchOn = !isTorchOn
            control.enableTorch(isTorchOn)
            onSuccess(isTorchOn)
        }
    }

    /**
     * Capture frame and return Base64 JPEG string & Bitmap.
     */
    fun captureFrame(
        onSuccess: (base64Image: String, width: Int, height: Int) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        val capture = imageCapture ?: return

        capture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()

                    if (bitmap != null) {
                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                        val byteArray = outputStream.toByteArray()
                        val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                        ContextCompat.getMainExecutor(context).execute {
                            onSuccess(base64, bitmap.width, bitmap.height)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Frame capture failed", exception)
                    ContextCompat.getMainExecutor(context).execute {
                        onError(exception)
                    }
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val planeProxy = image.planes[0]
        val buffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        // Apply rotation if needed
        val rotation = image.imageInfo.rotationDegrees
        return if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    fun release() {
        cameraProvider?.unbindAll()
        executor.shutdown()
    }
}
