package org.hyperskill.photoeditor

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.set
import com.google.android.material.slider.Slider


class MainActivity : AppCompatActivity() {

    private lateinit var ivPhoto: ImageView
    private lateinit var galleryButton: Button
    private lateinit var slBrightness: Slider
    private lateinit var textView: TextView
    private lateinit var textView2: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()

        //do not change this line
        ivPhoto.setImageBitmap(createBitmap())

        var defaultBitmap = (ivPhoto.drawable as BitmapDrawable?)?.bitmap!!
//        val (initialRed, initialGreen, initialBlue) = singleColor(defaultBitmap, 80, 90)



        //Add listener for gallery button to receive images from gallery
        val getImageIntent = Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedImageUri: Uri = result.data!!.data!!
                ivPhoto.setImageURI(selectedImageUri)
                defaultBitmap = (ivPhoto.drawable as BitmapDrawable?)?.bitmap!!
            }
        }
        galleryButton.setOnClickListener {
            resultLauncher.launch(getImageIntent)
        }



        //Add listener for slider to change the brightness
        slBrightness.addOnChangeListener { _, value, _ ->
            val newBitmap: Bitmap = applyBrightness(value.toInt(), defaultBitmap)
            ivPhoto.setImageBitmap(newBitmap)

//            // get actual rgp on the result image
//            val newRgb = singleColor(ivPhoto.drawable.toBitmap(), 80, 90)
//            textView2.text = "Actual RGP at x= 80 y=90, $newRgb"
//
//            //get expected rgp from the default rgp and add to the slider's value
//            val expectedRgb = Triple(initialRed + value.toInt(), initialGreen + value.toInt(), initialBlue + value.toInt())
//            textView.text = "Expected RGP at x= 80 y=90, $expectedRgb"

        }

    }

    private fun bindViews() {
        ivPhoto = findViewById(R.id.ivPhoto)
        galleryButton = findViewById(R.id.btnGallery)
        slBrightness = findViewById(R.id.slBrightness)
        textView = findViewById(R.id.textView)
        textView2 = findViewById(R.id.textView2)

    }

    // do not change this function
    fun createBitmap(): Bitmap {
        val width = 200
        val height = 100
        val pixels = IntArray(width * height)
        // get pixel array from source

        var R: Int
        var G: Int
        var B: Int
        var index: Int

        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                index = y * width + x
                // get color
                R = x % 100 + 40
                G = y % 100 + 80
                B = (x+y) % 100 + 120

                pixels[index] = Color.rgb(R,G,B)

            }
        }
        // output bitmap
        val bitmapOut = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bitmapOut.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmapOut
    }

    private fun singleColor(source: Bitmap, x: Int = 70, y: Int = 60): Triple<Int, Int, Int> {
        val pixel = source.getPixel(x, y)

        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)

        return  Triple(blue,red,green)
    }

    private fun applyBrightness(value: Int, defaultBitmap: Bitmap): Bitmap {
        val bitmap = defaultBitmap.copy()!!
        for (x in 0 until defaultBitmap.width) {
            for (y in 0 until defaultBitmap.height) {

                val existingColor = bitmap.getPixel(x, y)

                var newBlue = Color.blue(existingColor) + value

                var newRed = Color.red(existingColor) + value

                var newGreen = Color.green(existingColor) + value


                newBlue = if (newBlue > 255) {
                    255
                } else if (newBlue < 0) {
                    0
                } else {
                    newBlue
                }

                newRed = if (newRed > 255) {
                    255
                } else if (newRed < 0) {
                    0
                } else {
                    newRed
                }

                newGreen = if (newGreen > 255) {
                    255
                } else if (newGreen < 0) {
                    0
                } else {
                    newGreen
                }

                val newColor = Color.rgb(newRed, newGreen, newBlue)
                bitmap.setPixel(x, y, newColor)
            }
        }
        return bitmap
    }

//    private fun applyBrightness2(value: Float) {
//        val floatArray = floatArrayOf(
//            1f, 0f, 0f, 0f, value,
//            0f, 1f, 0f, 0f, value,
//            0f, 0f, 1f, 0f, value,
//            0f, 0f, 0f, 1f, 0f)
//        val colorMatrix = ColorMatrix(floatArray)
//        ivPhoto.colorFilter = ColorMatrixColorFilter(colorMatrix)
//    }

    private fun Bitmap.copy(): Bitmap? = copy(Bitmap.Config.RGB_565, true)

}