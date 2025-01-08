package com.daffaromyz.glucomonitor.ui.camera

//import com.daffaromyz.glucomonitor.ModelHelper
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.daffaromyz.glucomonitor.BoundingBox
import com.daffaromyz.glucomonitor.DigitDetector
import com.daffaromyz.glucomonitor.database.Glucose
import com.daffaromyz.glucomonitor.database.GlucoseDao
import com.daffaromyz.glucomonitor.database.GlucoseDatabase
import com.daffaromyz.glucomonitor.databinding.FragmentCameraBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


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

    private var stop = false

    private var resultValue : String = ""

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Set database
        db = GlucoseDatabase.getDatabase(this.requireContext())
        dao = db.glucoseDao()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        cameraExecutor.shutdown()
//        cameraExecutor.awaitTermination(10, TimeUnit.SECONDS)
        Log.i("CAMERA SHUT DOWN", "camera executor")

//        imageAnalyzer?.clearAnalyzer()
//        Log.i("CAMERA SHUT DOWN", "image analyzer")

//        digitDetector?.close()
//        Log.i("CAMERA SHUT DOWN", "digit detector")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // init camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // init digit detector
        cameraExecutor.execute {
            digitDetector = DigitDetector(requireContext(), this)
            digitDetector?.setup()
        }

        // start camera
        startCamera()

        // setup click for saving value
        binding.takeValueButton.setOnClickListener { takeValue() }

        // setup click for gallery
        binding.galleryButton.setOnClickListener { pickImage() }

    }

    // Function for saving value to database
    private fun takeValue() {

        // unit is mg
        if (resultValue.toIntOrNull() is Int) {
            lifecycleScope.launch {
                dao.insert(Glucose(id = 0, value = resultValue.toInt()))
                Log.i("INSERT", "mg $resultValue")
            }
            Toast.makeText(this.requireContext(), "Reading Successfully Added", Toast.LENGTH_SHORT).show()
        }
        // unit is mmol
        else if (resultValue.toDoubleOrNull() is Double) {
            val glucoseValue = resultValue.toDouble() * 18.0156
            lifecycleScope.launch {
                dao.insert(Glucose(id = 0, value = glucoseValue.toInt()))
                Log.i("INSERT", "mmol $resultValue")
            }
            Toast.makeText(this.requireContext(), "Reading Successfully Added", Toast.LENGTH_SHORT).show()
        }
        // no value
        else {
            Toast.makeText(this.requireContext(), "Reading Failed", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to call digit detector
    private fun detectDigit(image: ImageProxy) {
        if (!stop) {
            // Copy out RGB bits to the shared bitmap buffer
            image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
            image.close()

            val matrix = Matrix().apply {
                postRotate(image.imageInfo.rotationDegrees.toFloat())
            }

            val bitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            Log.i("DIGIT DETECTOR", "from camera")
            digitDetector?.detect(bitmap, false, "")
        }
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

    private fun pickImage() {
        cameraExecutor.shutdown()

        // Pick image from gallery
        Log.i("GALLERY PICK", "button pressed")

        // setup intent
        val intent = Intent()
        intent.setType("image/*")
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT)

        // launch intent
        if (cameraExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
            Log.i("GALLERY PICK", "launching intent")
            intentLauncher.launch(intent)
        } else {
            Toast.makeText(this.requireContext(), "Open Gallery : Try Again", Toast.LENGTH_SHORT).show()
        }
    }

    private val intentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.i("GALLERY PICK", "intent return")
        if (result.resultCode == Activity.RESULT_OK) {

            val data = result.data


            // check if null
            if (data?.data != null) {
                Log.i("GALLERY PICK", "image picked")

                val uri = data.data!!
                var galleryBitmap : Bitmap

                var name =  uri.lastPathSegment.toString()
                var index = name.indexOfLast { it.toString() == "/" }
                if (index != -1) {
                    name = name.drop(index+1)
                }
                Log.i("GALLERY PICK", name)

                // get bitmap from uri
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        galleryBitmap = ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(
                                requireContext().contentResolver,
                                uri
                            )
                        )
                    } else {
                         galleryBitmap = MediaStore.Images.Media.getBitmap(
                             requireContext().contentResolver,
                             uri)
                    }
                    Log.i("GALLERY PICK", "bitmap decoded")
                    Log.i("GALLERY PICK", galleryBitmap.width.toString())
                    Log.i("GALLERY PICK", galleryBitmap.height.toString())

                    // display image
                    binding.viewFinder.visibility = View.GONE
                    binding.galleryView.visibility = View.VISIBLE

                    binding.galleryView.setImageBitmap(galleryBitmap)
                    Log.i("GALLERY PICK", "image shown")

                    // analyze image
                    Log.i("DIGIT DETECTOR", "from gallery")
                    stop = true
                    digitDetector?.detect(galleryBitmap.copy(Bitmap.Config.ARGB_8888, false), false, name)

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                if (data?.clipData != null) {

                    val clipdata = data.clipData
                    var galleryBitmap : Bitmap

                    if (clipdata != null) {
                        for (i in 0..(clipdata.itemCount-1)) {
                            val uri = clipdata.getItemAt(i).uri

                            var name =  uri.lastPathSegment.toString()
                            var index = name.indexOfLast { it.toString() == "/" }
                            if (index != -1) {
                                name = name.drop(index+1)
                            }
                            Log.i("GALLERY PICK", name)

                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    galleryBitmap = ImageDecoder.decodeBitmap(
                                        ImageDecoder.createSource(
                                            requireContext().contentResolver,
                                            uri
                                        )
                                    )
                                } else {
                                    galleryBitmap = MediaStore.Images.Media.getBitmap(
                                        requireContext().contentResolver,
                                        uri)
                                }
                                Log.i("GALLERY PICK", "bitmap decoded")
                                Log.i("GALLERY PICK", galleryBitmap.width.toString())
                                Log.i("GALLERY PICK", galleryBitmap.height.toString())

                                // display image
                                binding.viewFinder.visibility = View.GONE
                                binding.galleryView.visibility = View.VISIBLE

                                binding.galleryView.setImageBitmap(galleryBitmap)
                                Log.i("GALLERY PICK", "image shown")

                                // analyze image
                                stop = true
                                digitDetector?.detect(galleryBitmap.copy(Bitmap.Config.ARGB_8888, false), false, name)

                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
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