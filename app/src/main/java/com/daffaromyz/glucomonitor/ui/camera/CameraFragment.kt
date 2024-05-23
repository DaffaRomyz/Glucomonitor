package com.daffaromyz.glucomonitor.ui.camera

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.daffaromyz.glucomonitor.databinding.FragmentCameraBinding
import android.graphics.Bitmap
import android.graphics.Matrix
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageProxy
import com.daffaromyz.glucomonitor.BoundingBox
import com.daffaromyz.glucomonitor.DigitDetector
import com.daffaromyz.glucomonitor.database.Glucose
import com.daffaromyz.glucomonitor.database.GlucoseDao
import com.daffaromyz.glucomonitor.database.GlucoseDatabase
import kotlinx.coroutines.runBlocking
//import com.daffaromyz.glucomonitor.ModelHelper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(), DigitDetector.DetectorListener {

    private var _binding: FragmentCameraBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var bitmapBuffer: Bitmap
    private var imageAnalyzer: ImageAnalysis? = null

    private var digitDetector : DigitDetector? = null

    private lateinit var db : GlucoseDatabase
    private lateinit var dao : GlucoseDao

    private var resultValue : String = ""

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        val root: View = binding.root

        db = GlucoseDatabase.getDatabase(this.requireContext())
        dao = db.glucoseDao()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
        digitDetector?.close()
        db.close()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        cameraExecutor.execute {
            digitDetector = DigitDetector(requireContext(), this)
            digitDetector?.setup()
        }
        startCamera()

        binding.takeValueButton.setOnClickListener { takeValue() }
    }

    private fun takeValue() {
        if (resultValue.toIntOrNull() is Int) {
            runBlocking {
                dao.insert(Glucose(0,2, resultValue.toInt()))
                Log.i("INSERT", "mg $resultValue")
            }
            Toast.makeText(this.requireContext(), "Reading Successfully Added", Toast.LENGTH_SHORT).show()
        } else if (resultValue.toDoubleOrNull() is Double) {
            val glucoseValue = resultValue.toDouble() * 18.018
            runBlocking {
                dao.insert(Glucose(0,2, glucoseValue.toInt()))
                Log.i("INSERT", "mmol $resultValue")
            }
            Toast.makeText(this.requireContext(), "Reading Successfully Added", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this.requireContext(), "Reading Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun detectDigit(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
        image.close()

        val matrix = Matrix().apply {
            postRotate(image.imageInfo.rotationDegrees.toFloat())
        }

        val bitmap =  Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )

        digitDetector?.detect(bitmap)
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(binding.viewFinder.display.rotation)
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageAnalyzer =
                ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setTargetRotation(binding.viewFinder.display.rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    // The analyzer can then be assigned to the instance
                    .also {
                        it.setAnalyzer(cameraExecutor) { image ->
                            if (!::bitmapBuffer.isInitialized) {
                                // The image rotation and RGB image buffer are initialized only once
                                // the analyzer has started running
                                bitmapBuffer = Bitmap.createBitmap(
                                    image.width,
                                    image.height,
                                    Bitmap.Config.ARGB_8888
                                )
                            }

                            detectDigit(image)
                        }
                    }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onEmptyDetect() {
        activity?.runOnUiThread {
            binding.overlay.clear()
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, value: String, inferenceTime: Long) {
        resultValue = value
        activity?.runOnUiThread {
            binding.valueText.text = value
            binding.inferenceTimeText.text = inferenceTime.toString() + "ms"
            binding.overlay.apply {
                setResults(boundingBoxes)
                invalidate()
            }
            }
        }


    companion object {
        private const val TAG = "CameraFragment"
    }
}