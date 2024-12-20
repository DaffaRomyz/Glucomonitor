package com.daffaromyz.glucomonitor

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.core.graphics.get
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.DequantizeOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.common.ops.QuantizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException


class DigitDetector(
    private val context: Context,
    private val detectorListener: DetectorListener
) {
    private var interpreter: Interpreter? = null
    private var modelName = "yolo11s_float16.tflite"
    private var labels = mutableListOf<String>()

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    private var inputImageWidth: Int = 0 // will be inferred from TF Lite model.
    private var inputImageHeight: Int = 0 // will be inferred from TF Lite model.
    private var modelInputSize: Int = 0 // will be inferred from TF Lite model.
    private var numChannel: Int = 0
    private var numElements: Int = 0

    private var stop = false
    private var i = 0
    fun setup(isGpu: Boolean = true) {

        if (interpreter != null) {
            close()
        }

        val options = if (isGpu) {
            val compatList = CompatibilityList()

            Interpreter.Options().apply{
                if(compatList.isDelegateSupportedOnThisDevice){
                    val delegateOptions = compatList.bestOptionsForThisDevice
                    this.addDelegate(GpuDelegate(delegateOptions))
                    Log.i("DIGIT DETECTOR", "GPU delegate")
                } else {
                    this.setNumThreads(4)
                    Log.i("DIGIT DETECTOR", "GPU not support")
                }
            }
        } else {
            Interpreter.Options().apply{
                this.setNumThreads(4)
                Log.i("DIGIT DETECTOR", "CPU delegate")
            }
        }


        val model = FileUtil.loadMappedFile(context, modelName)
        interpreter = Interpreter(model, options)

        val inputShape = interpreter?.getInputTensor(0)?.shape() ?: return
        val outputShape = interpreter?.getOutputTensor(0)?.shape() ?: return

        inputImageWidth = inputShape[1]
        inputImageHeight = inputShape[2]

        numChannel = outputShape[1]
        numElements = outputShape[2]

        labels = mutableListOf<String>(",","0", "1", "2","3","4","5","6","7","8","9")
    }

    fun detect(frame: Bitmap, stopafter : Boolean, filename : String) {

        if (!stop) {
            interpreter ?: return
            if (inputImageWidth == 0) return
            if (inputImageHeight == 0) return
            if (numChannel == 0) return
            if (numElements == 0) return

            // resize bitmap to 640x640
            val resizedBitmap = Bitmap.createScaledBitmap(frame, inputImageWidth, inputImageHeight, false)

            // save bitmap for testing
//            i += 1
//            val filename = StringBuilder()
//            filename.append(context.getDir("GlucoMonitor", Context.MODE_PRIVATE))
//            filename.append("/GlucoMonitor_")
//            filename.append(i.toString())
//            filename.append(".png")
//            val file = File(filename.toString())
//            val fileOutputStream = FileOutputStream(file)
//            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
//            fileOutputStream.close()

//            Log.i("DIGIT DETECTOR", filename.toString())
//            Log.i("DIGIT DETECTOR", stopafter.toString())
//            Log.i("DIGIT DETECTOR", resizedBitmap.width.toString())
//            Log.i("DIGIT DETECTOR", resizedBitmap.height.toString())

            // Convert image to Tensor Image
            var tensorImage = TensorImage(INPUT_IMAGE_TYPE)
            tensorImage.load(resizedBitmap)

            // Normalize image if model input float
            if (INPUT_IMAGE_TYPE == DataType.FLOAT32) {
                tensorImage = imageProcessor.process(tensorImage)
            }

            // Init Output Buffer
            var output = TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), OUTPUT_IMAGE_TYPE)

            // Inference
            var inferenceTime = SystemClock.uptimeMillis()
            interpreter?.run(tensorImage.buffer, output.buffer)
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime

            // Convert to float if model output int
//            Log.i("MODEL OUTPUT DATATYPE1", output.dataType.toString())
            if (OUTPUT_IMAGE_TYPE == DataType.UINT8) {
                output = QuantizeOp(0F, 255F).apply(output)
            }
//            Log.i("MODEL OUTPUT DATATYPE2", output_casted.dataType.toString())

            // Check output
//            val array_float = output.floatArray
//            Log.i("MODEL OUTPUT FLOAT1", "${array_float[0]} ${array_float[numElements]} ${array_float[numElements*2]} ${array_float[numElements*3]} ${array_float[numElements*4]} ${array_float[numElements*5]} ${array_float[numElements*6]} ${array_float[numElements*7]} ${array_float[numElements*8]} ${array_float[numElements*9]} ${array_float[numElements*10]} ${array_float[numElements*11]} ${array_float[numElements*12]} ${array_float[numElements*13]} ${array_float[numElements*14]}")
//
//            val array_casted = output_casted.floatArray
//            Log.i("MODEL OUTPUT FLOAT2", "${array_casted[0]} ${array_casted[numElements]} ${array_casted[numElements*2]} ${array_casted[numElements*3]} ${array_casted[numElements*4]} ${array_casted[numElements*5]} ${array_casted[numElements*6]} ${array_casted[numElements*7]} ${array_casted[numElements*8]} ${array_casted[numElements*9]} ${array_casted[numElements*10]} ${array_casted[numElements*11]} ${array_casted[numElements*12]} ${array_casted[numElements*13]} ${array_casted[numElements*14]}")

            // Filter Confidence & NMS
            val bestBoxes = bestBox(output.floatArray)

            // Return if empty
            if (bestBoxes == null) {
                detectorListener.onEmptyDetect()
                return
            }

            // Sort boxes from left to right
            val centerComparator = Comparator { box1: BoundingBox, box2: BoundingBox -> ((box1.cx - box2.cx) * 100).toInt() }
            val orderedBoxes = bestBoxes.sortedWith(centerComparator)

            // Set combined glucose value
            val result  = StringBuffer()
            Log.i("BOUNDBOX VALUE", filename)
            for (box in orderedBoxes) {
                Log.i("BOUNDBOX DESCR", "CLS = ${box.clsName} CX = ${box.cx} CY = ${box.cy} W = ${box.w} H = ${box.h}")
                Log.i("BOUNDBOX VALUE", "${box.clsName} ${box.cx} ${box.cy} ${box.w} ${box.h}")
                result.append(box.clsName)
            }
            Log.i("BOUNDBOX VALUE", "END")
            // Create txt for eval
//            if (filename != "") {
//                try {
//                    val root = File(Environment.getExternalStorageDirectory(), "Notes")
//                    if (!root.exists()) {
//                        root.mkdirs()
//                    }
//                    val gpxfile = File(root, "$filename.txt")
//                    val writer = FileWriter(gpxfile)
//                    for (box in orderedBoxes) {
//                        writer.append(box.clsName)
//                        writer.append(" ")
//                        writer.append(box.cx.toString())
//                        writer.append(" ")
//                        writer.append(box.cy.toString())
//                        writer.append(" ")
//                        writer.append(box.w.toString())
//                        writer.append(" ")
//                        writer.append(box.h.toString())
//                        writer.append("\n")
//                    }
//                    writer.flush()
//                    writer.close()
//                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
//                    Log.i("TEXT SAVED", "txt saved")
//                } catch (e: IOException) {
//                    Log.i("TEXT SAVED", "txt not saved")
//                    e.printStackTrace()
//                }
//            }

            // Return box, value, time
            Log.i("INFERENCE", "Result = $result Inference Time = $inferenceTime")
            detectorListener.onDetect(bestBoxes, result.toString(), inferenceTime)
        }

        if (stopafter) {
            stop = true
        }
    }

    private fun bestBox(array: FloatArray) : List<BoundingBox>? {
        // array is arranged as cx * numelement, cy * numelement, w * numelement, h * numelement
        // the rest is conf for each possible class

        val boundingBoxes = mutableListOf<BoundingBox>()

        for (c in 0 until numElements) {
            // find class of the current box, class = best conf
            var maxConf = CONFIDENCE_THRESHOLD
            var maxIdx = -1
            var j = 4
            var arrayIdx = c + numElements * j
            while (j < numChannel){
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += numElements
            }

            // Add bounding box
            if (maxConf > CONFIDENCE_THRESHOLD) {
                val clsName = labels[maxIdx]
                val cx = array[c] // 0
                val cy = array[c + numElements] // 1
                val w = array[c + numElements * 2]
                val h = array[c + numElements * 3]
                val x1 = cx - (w/2F)
                val y1 = cy - (h/2F)
                val x2 = cx + (w/2F)
                val y2 = cy + (h/2F)
                if (x1 < 0F || x1 > 1F) continue
                if (y1 < 0F || y1 > 1F) continue
                if (x2 < 0F || x2 > 1F) continue
                if (y2 < 0F || y2 > 1F) continue

                boundingBoxes.add(
                    BoundingBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                        cx = cx, cy = cy, w = w, h = h,
                        cnf = maxConf, cls = maxIdx, clsName = clsName
                    )
                )
            }
        }

//        Log.i("DIGIT DETECTOR", boundingBoxes.size.toString())
        if (boundingBoxes.isEmpty()) return null

        return applyNMS(boundingBoxes)
    }

    private fun applyNMS(boxes: List<BoundingBox>) : MutableList<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        // compare each boxes, if IOU >= threshold keep only one
        while(sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

//        Log.i("DIGIT DETECTOR", selectedBoxes.size.toString())
        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }

    fun close() {
        interpreter?.close()
//        interpreter = null
    }

    interface DetectorListener {
        fun onEmptyDetect()
        fun onDetect(boundingBoxes: List<BoundingBox>, value: String, inferenceTime: Long)
    }

    companion object {
        private const val TAG = "DigitDetector"

        private const val FLOAT_TYPE_SIZE = 4
        private const val PIXEL_SIZE = 3

        private const val OUTPUT_CLASSES_COUNT = 11

        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.3F
        private const val IOU_THRESHOLD = 0.5F
    }
}