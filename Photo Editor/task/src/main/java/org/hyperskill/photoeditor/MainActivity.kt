package org.hyperskill.photoeditor

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import com.google.android.material.slider.Slider


class MainActivity : AppCompatActivity() {

    private lateinit var ivPhoto: ImageView
    private lateinit var galleryButton: Button
    private lateinit var btnSave: Button
    private lateinit var slBrightness: Slider
    private lateinit var textView: TextView
    private lateinit var slContrast: Slider


    @RequiresApi(Build.VERSION_CODES.M)
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
        slBrightness.addOnChangeListener { _, brighness, _ ->
            val newBitmap: Bitmap = applyBrightnessThenContrast(brighness.toInt(), slContrast.value.toInt() , defaultBitmap)
            ivPhoto.setImageBitmap(newBitmap)

//            // get actual rgp on the result image
//            val newRgb = singleColor(ivPhoto.drawable.toBitmap(), 80, 90)
//            textView2.text = "Actual RGP at x= 80 y=90, $newRgb"
//
//            //get expected rgp from the default rgp and add to the slider's value
//            val expectedRgb = Triple(initialRed + value.toInt(), initialGreen + value.toInt(), initialBlue + value.toInt())
//            textView.text = "Expected RGP at x= 80 y=90, $expectedRgb"

        }

        slContrast.addOnChangeListener{ _, contrast, _ ->
            val newBitmap: Bitmap = applyBrightnessThenContrast(slBrightness.value.toInt(), contrast.toInt(), defaultBitmap)
            ivPhoto.setImageBitmap(newBitmap)

        }

        //Add listener for save button to save the current state of the image
        btnSave.setOnClickListener { _ ->

            if ( hasPermission("android.permission.WRITE_EXTERNAL_STORAGE") ) {

                val bitmap: Bitmap = (ivPhoto.drawable as BitmapDrawable?)?.bitmap!!
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                values.put(MediaStore.Images.ImageColumns.WIDTH, bitmap.width)
                values.put(MediaStore.Images.ImageColumns.HEIGHT, bitmap.height)

                val uri = this@MainActivity.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ) ?: return@setOnClickListener

                contentResolver.openOutputStream(uri).use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }
            } else {
                //ask for the permission
                val permissions = arrayOf("android.permission.WRITE_EXTERNAL_STORAGE")
                val permsRequestCode = 200
                requestPermissions(permissions, permsRequestCode)
            }
        }

    }

    private fun bindViews() {
        ivPhoto = findViewById(R.id.ivPhoto)
        galleryButton = findViewById(R.id.btnGallery)
        slBrightness = findViewById(R.id.slBrightness)
        textView = findViewById(R.id.textView)
        btnSave = findViewById(R.id.btnSave)
        slContrast = findViewById(R.id.slContrast)
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

    private fun applyBrightnessThenContrast(brightness: Int, contrast: Int, defaultBitmap: Bitmap): Bitmap {
        val newBitmap = defaultBitmap.copy()!!
        var averageBrightness = 0

        for (x in 0 until defaultBitmap.width) {
            for (y in 0 until defaultBitmap.height) {
                var newRed: Int
                var newGreen: Int
                var newBlue: Int
                val c = newBitmap.getPixel(x,y)

                newRed = Color.red(c)+brightness
                newRed= 0.coerceAtLeast(kotlin.math.min(newRed, 255))
                newGreen = Color.green(c)+brightness
                newGreen = 0.coerceAtLeast(kotlin.math.min(newGreen, 255))
                newBlue = Color.blue(c)+brightness
                newBlue = 0.coerceAtLeast(kotlin.math.min(newBlue, 255))

                newBitmap.setPixel(x,y,Color.rgb(newRed,newGreen,newBlue))
                averageBrightness += (newRed + newGreen + newBlue) / 3
            }
        }

        // no need to proceed with doing the contrast filter as its slider value is zero
        if (contrast == 0) {
            return newBitmap
        }

        averageBrightness /= (defaultBitmap.height * defaultBitmap.width)

        for (x in 0 until defaultBitmap.width) {
            for (y in 0 until defaultBitmap.height) {
                var newRed: Int
                var newGreen: Int
                var newBlue: Int
                val c = newBitmap.getPixel(x,y)

                newRed = Color.red(c)
                newGreen = Color.green(c)
                newBlue = Color.blue(c)

                val alpha:Double = (255.0+contrast)/(255.0-contrast)
                newRed = (alpha*(newRed-averageBrightness)+averageBrightness).toInt()
                newRed= 0.coerceAtLeast(kotlin.math.min(newRed, 255))
                newGreen = (alpha*(newGreen-averageBrightness)+averageBrightness).toInt()
                newGreen = 0.coerceAtLeast(kotlin.math.min(newGreen, 255))
                newBlue = (alpha*(newBlue-averageBrightness)+averageBrightness).toInt()
                newBlue = 0.coerceAtLeast(kotlin.math.min(newBlue, 255))

                newBitmap.setPixel(x,y,Color.rgb(newRed,newGreen,newBlue))
            }
        }
        return newBitmap
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

    private fun hasPermission(manifestPermission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.checkSelfPermission(manifestPermission) == PackageManager.PERMISSION_GRANTED
        } else {
            PermissionChecker.checkSelfPermission(this, manifestPermission) == PermissionChecker.PERMISSION_GRANTED
        }
    }

}