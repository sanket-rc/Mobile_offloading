package com.android.mobile_application

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.tensorflow.lite.Interpreter

class DigitClassifier (private val context: Context) {
    private var interpreter: Interpreter? = null
    var isInitialized = false
        private set

    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    private var inputImageWidth: Int = 0 // will be inferred from TF Lite model.
    private var inputImageHeight: Int = 0 // will be inferred from TF Lite model.
    private var modelInputSize: Int = 0 // will be inferred from TF Lite model.

    fun initialize(version : Int): Task<Void?> {
        val task = TaskCompletionSource<Void?>()
        executorService.execute {
            try {
                initializeInterpreter(version)
                task.setResult(null)
                Log.e (TAG, "Initialized")
            } catch (e: IOException) {
                task.setException(e)
                Log.e (TAG, e.toString())
            }
        }
        return task.task
    }

    private fun initializeInterpreter(version : Int) {
        val assetManager = context.assets
        var fileName : String = ""
        when (version){
            0 -> fileName = "mnist_TL.tflite"
            1 -> fileName = "mnist_TR.tflite"
            2 -> fileName = "mnist_BL.tflite"
            3 -> fileName = "mnist_BR.tflite"
        }
        val model = loadModelFile(assetManager, fileName)
        val interpreter = Interpreter(model)

        val inputShape = interpreter.getInputTensor(0).shape()
        inputImageWidth = inputShape[1]
        inputImageHeight = inputShape[2]
        modelInputSize = FLOAT_TYPE_SIZE * inputImageWidth * inputImageHeight * PIXEL_SIZE

        this.interpreter = interpreter

        isInitialized = true
        Log.e(TAG, "Initialized TFLite interpreter.")
    }

    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, filename: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun classify(bitmap: Bitmap): FloatArray {
        Log.e(TAG, "Entering Classify");
        check(isInitialized) { "TF Lite Interpreter is not initialized yet." }

        val resizedImage = Bitmap.createScaledBitmap(
            bitmap,
            inputImageWidth,
            inputImageHeight,
            true
        )
        val byteBuffer = convertBitmapToByteBuffer(resizedImage)

        val output = Array(1) { FloatArray(OUTPUT_CLASSES_COUNT) }

        interpreter?.run(byteBuffer, output)

        val result = output[0]
        val maxIndex = result.indices.maxByOrNull { result[it] } ?: -1
        val resultString =
            "Prediction Result: %d | Confidence: %2f"
                .format(maxIndex, result[maxIndex])

        Log.e(TAG, "Leaving Classify")
        //return resultString
        return result
    }

    fun close() {
        executorService.execute {
            interpreter?.close()
            Log.e(TAG, "Closed TFLite interpreter.")
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in pixels) {
            val r = (pixelValue shr 16 and 0xFF)
            val g = (pixelValue shr 8 and 0xFF)
            val b = (pixelValue and 0xFF)

            val normalizedPixelValue = (r + g + b) / 3.0f / 255.0f
            byteBuffer.putFloat(normalizedPixelValue)
        }

        return byteBuffer
    }

    companion object {
        private const val TAG = "DigitClassifier"

        private const val FLOAT_TYPE_SIZE = 4
        private const val PIXEL_SIZE = 1

        private const val OUTPUT_CLASSES_COUNT = 10
    }
}