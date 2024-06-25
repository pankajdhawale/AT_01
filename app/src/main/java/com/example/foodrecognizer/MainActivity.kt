package com.example.foodrecognizer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var textViewResult: TextView
    private lateinit var tflite: Interpreter
    private lateinit var labels: List<String>

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        textViewResult = findViewById(R.id.textViewResult)
        val buttonCapture: Button = findViewById(R.id.buttonCapture)

        // Load the TFLite model and labels
        tflite = Interpreter(loadModelFile("model.tflite"))
        labels = loadLabels("labels.txt")

        buttonCapture.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val photo: Bitmap = data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(photo)
            recognizeImage(photo)
        }
    }

    private fun recognizeImage(bitmap: Bitmap) {
        val image = TensorImage.fromBitmap(bitmap)

        // Assuming a model with one output and it returns a list of probabilities for each label
        val outputs = arrayOf(FloatArray(labels.size))
        tflite.run(image.buffer, outputs)

        // Get the highest probability label
        val maxIndex = outputs[0].indices.maxByOrNull { outputs[0][it] } ?: -1
        val recognizedLabel = labels[maxIndex]
        val calories = getCaloriesForFood(recognizedLabel)

        textViewResult.text = "Recognized: $recognizedLabel\nCalories: $calories"
    }

    private fun loadModelFile(filename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabels(filename: String): List<String> {
        return assets.open(filename).bufferedReader().useLines { it.toList() }
    }

    private fun getCaloriesForFood(food: String): Int {
        // Mock database of food calorie information
        val foodCalories = mapOf(
            "apple" to 95,
            "banana" to 105,
            "carrot" to 25
        )
        return foodCalories[food] ?: 0
    }
}
