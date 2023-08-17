package com.example.camerasample

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.camerasample.databinding.LayoutMainActityBinding
import com.example.camerasample.ui.PreviewImageCaptureFragment
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Chức năng: ✓Preview, analysis, capture image/video, crop, switch camera, filter
 */
class MainActivity: AppCompatActivity(), LumaListener {
    private var cameraAspectRatio: Int = AspectRatio.RATIO_4_3
    private lateinit var viewBinding: LayoutMainActityBinding

    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutMainActityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if (allPermissionsGranted()) {
            // Preview
            preview = Preview.Builder()
                .apply {
                    setTargetAspectRatio(cameraAspectRatio)
                }
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(cameraAspectRatio)
                .build()

            startCamera()
        } else {
            requestPermissions()
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.apply {
            imageCaptureButton.setOnClickListener { takePhoto() }
            saveImageButton.setOnClickListener { saveImage()/*captureVideo()*/ }

            btnChangePreviewResolution.setOnClickListener {
                var width = 0
                var height = 0

                try {
                    width = edtWidthPreview.text.toString().toInt()
                    height = edtHeightPreview.text.toString().toInt()
                } catch (_: java.lang.Exception) {
                    return@setOnClickListener
                }

                cameraExecutor.shutdown()

                preview = Preview.Builder()
                    .apply {
                    setTargetResolution(Size(width, height))
//                    setTargetAspectRatio(cameraAspectRatio)
                    }
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }

                imageCapture = ImageCapture.Builder()
                    .setTargetResolution(Size(width, height))
//                .setTargetAspectRatio(cameraAspectRatio)
                    .build()

                startCamera()
            }

            btnChangeRatio.text = "RATIO_4_3"
            btnChangeRatio.setOnClickListener {
                if (cameraAspectRatio == AspectRatio.RATIO_4_3) {
                    btnChangeRatio.text = "RATIO_16_9"
                    cameraAspectRatio = AspectRatio.RATIO_16_9
                } else {
                    btnChangeRatio.text = "RATIO_4_3"
                    cameraAspectRatio = AspectRatio.RATIO_4_3
                }

                cameraExecutor.shutdown()

                preview = Preview.Builder()
                    .apply {
                        setTargetAspectRatio(cameraAspectRatio)
                    }
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }

                imageCapture = ImageCapture.Builder()
                    .setTargetAspectRatio(cameraAspectRatio)
                    .build()

                startCamera()
            }
        }


        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun saveImage() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
//        imageCapture.takePicture(
//            outputOptions,
//            ContextCompat.getMainExecutor(this),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onError(exc: ImageCaptureException) {
//                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
//                }
//
//                override fun onImageSaved(output: ImageCapture.OutputFileResults){
//                    val msg = "Photo capture succeeded: ${output.savedUri}"
//                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
//                    Log.d(TAG, msg)
//                }
//            }
//        )

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object :
            ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                //get bitmap from image
                val bitmap = imageProxyToBitmap(image)
                super.onCaptureSuccess(image)

                val rotateBitmap = rotateBitmap(bitmap)
                val previewBitmap = cropSize(rotateBitmap)
//                val previewBitmap = cropBitmapToScreen(rotateBitmap, viewBinding.viewFinder.width, viewBinding.viewFinder.height)

                PreviewImageCaptureFragment(
                    /*bitmap*/previewBitmap,
                    viewBinding.viewFinder.height,
                    viewBinding.viewFinder.width
                ).show(supportFragmentManager, "PreviewImageCaptureFragment")
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
            }

        })
    }

    fun rotateBitmap(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun cropSize(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        var desiredWidth = viewBinding.viewFinder.width
        var desiredHeight = viewBinding.viewFinder.height


        val widthRatio: Float = width.toFloat() / desiredWidth.toFloat()
        val heightRatio: Float = height.toFloat() / desiredHeight.toFloat()

        // Calculate the aspect ratio of the original image
        val aspectRatio = width.toFloat() / height.toFloat()

        // Calculate the new dimensions based on the desired width and height
        val newWidth: Int
        val newHeight: Int

        if (desiredWidth.toFloat() / desiredHeight.toFloat() > aspectRatio) {
            newWidth = (desiredWidth * widthRatio/* * aspectRatio*/).toInt()
            newHeight = (desiredHeight * widthRatio).toInt()
        } else {
            newWidth = (desiredWidth * widthRatio).toInt()
            newHeight = (desiredHeight * widthRatio/* / aspectRatio*/).toInt()
        }

        // Calculate the starting coordinates for cropping
        val startX = (width - newWidth) / 2
        val startY = (height - newHeight) / 2

        // Create the cropped Bitmap using the calculated coordinates and dimensions
        return Bitmap.createBitmap(bitmap, startX, startY, newWidth, newHeight)
    }

    fun cropBitmapToScreen(bitmap: Bitmap, screenWidth: Int, screenHeight: Int): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        // Calculate the ratio of the bitmap's width and height to the screen's width and height
        val widthRatio = screenWidth.toFloat() / bitmapWidth
        val heightRatio = screenHeight.toFloat() / bitmapHeight

        // Determine the scale factor to fit the bitmap within the screen
        val scaleFactor = if (widthRatio > heightRatio) heightRatio else widthRatio

        // Calculate the new width and height of the cropped bitmap
        val newWidth = (bitmapWidth * scaleFactor).toInt()
        val newHeight = (bitmapHeight * scaleFactor).toInt()

        // Calculate the starting position for cropping
        val startX = (bitmapWidth - newWidth) / 2
        val startY = (bitmapHeight - newHeight) / 2

        // Create a rect object to define the crop bounds
        val rect = Rect(startX, startY, startX + newWidth, startY + newHeight)

        // Create a new bitmap with the cropped region
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
    }

    /**
     *  convert image proxy to bitmap
     *  @param image
     */
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

//    private fun captureVideo() {
//        val videoCapture = this.videoCapture ?: return
//
//        viewBinding.videoCaptureButton.isEnabled = false
//
//        val curRecording = recording
//        if (curRecording != null) {
//            // Stop the current recording session.
//            curRecording.stop()
//            recording = null
//            return
//        }
//
//        // create and start a new recording session
//        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
//            .format(System.currentTimeMillis())
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
//            }
//        }
//
//        val mediaStoreOutputOptions = MediaStoreOutputOptions
//            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
//            .setContentValues(contentValues)
//            .build()
//        recording = videoCapture.output
//            .prepareRecording(this, mediaStoreOutputOptions)
//            .apply {
//                if (PermissionChecker.checkSelfPermission(this@MainActivity,
//                        Manifest.permission.RECORD_AUDIO) ==
//                    PermissionChecker.PERMISSION_GRANTED)
//                {
//                    withAudioEnabled()
//                }
//            }
//            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
//                when(recordEvent) {
//                    is VideoRecordEvent.Start -> {
//                        viewBinding.videoCaptureButton.apply {
//                            text = getString(R.string.stop_capture)
//                            isEnabled = true
//                        }
//                    }
//                    is VideoRecordEvent.Finalize -> {
//                        if (!recordEvent.hasError()) {
//                            val msg = "Video capture succeeded: " +
//                                    "${recordEvent.outputResults.outputUri}"
//                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
//                                .show()
//                            Log.d(TAG, msg)
//                        } else {
//                            recording?.close()
//                            recording = null
//                            Log.e(TAG, "Video capture ends with error: " +
//                                    "${recordEvent.error}")
//                        }
//                        viewBinding.videoCaptureButton.apply {
//                            text = getString(R.string.start_capture)
//                            isEnabled = true
//                        }
//                    }
//                }
//            }
//    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            /* TODO: Không hỗ trợ vừa phân tích + chụp + quay
                     Có thể vừa chụp + phân tích
                     Vừa quay + phân tích
                     Chụp + quay
            */
            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer(listener = this))
                }

            /*val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)*/

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture/*, imageAnalyzer*//*, videoCapture*/)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    override fun analyzeFrame(pixel: List<Int>) {
        Log.d(TAG, "Average luminosity: $pixel")
    }
}

private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    override fun analyze(image: ImageProxy) {

        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        val pixels = data.map { it.toInt() and 0xFF }
        val luma = pixels.average()
        listener.analyzeFrame(pixels)
//        listener(luma)

        image.close()
    }
}


interface LumaListener {
    fun analyzeFrame(pixel: List<Int>)
}
