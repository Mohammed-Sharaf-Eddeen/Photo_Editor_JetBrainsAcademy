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
import kotlinx.coroutines.*
import kotlin.math.pow


class MainActivity : AppCompatActivity() {

    private lateinit var ivPhoto: ImageView
    private lateinit var galleryButton: Button
    private lateinit var btnSave: Button
    private lateinit var slBrightness: Slider
    private lateinit var textView: TextView
    private lateinit var slContrast: Slider
    private lateinit var slSaturation: Slider
    private lateinit var slGamma: Slider

    private lateinit var defaultBitmap: Bitmap
    private var lastJob: Job? = null


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()

        //do not change this line
        ivPhoto.setImageBitmap(createBitmap())

        defaultBitmap = (ivPhoto.drawable as BitmapDrawable?)?.bitmap!!



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
        slBrightness.addOnChangeListener { _, _, _ ->
            onSliderChanges()
        }

        slContrast.addOnChangeListener{ _, _, _ ->
           onSliderChanges()

        }

        slSaturation.addOnChangeListener{ _, _, _ ->
           onSliderChanges()

        }

        slGamma.addOnChangeListener{ _, _, _ ->
            onSliderChanges()

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
        slSaturation = findViewById(R.id.slSaturation)
        slGamma = findViewById(R.id.slGamma)
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

    private fun applyAllFilters(brightness: Int, contrast: Int, saturation: Int, gamma: Double, defaultBitmap: Bitmap): Bitmap {
        val newBitmap = defaultBitmap.copy()!!
        var averageBrightness = 0

        //Apply Brightness
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

        // no need to proceed with other filters
        if (contrast == 0 && saturation == 0 && gamma.compareTo(1) == 0) {
            return newBitmap
        }

        //Apply Contrast
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

        // no need to proceed with other filters
        if (saturation == 0 && gamma.compareTo(1) == 0) {
            return newBitmap
        }

        //Apply Saturation
        for (x in 0 until defaultBitmap.width) {
            for (y in 0 until defaultBitmap.height) {
                var newRed: Int
                var newGreen: Int
                var newBlue: Int
                val c = newBitmap.getPixel(x,y)

                newRed = Color.red(c)
                newGreen = Color.green(c)
                newBlue = Color.blue(c)
                val alpha: Double = (255.0 + saturation)/(255.0 - saturation)
                val rgbAverage = (newBlue + newGreen + newRed) / 3

                newRed = (alpha*(newRed-rgbAverage)+rgbAverage).toInt()
                newRed= 0.coerceAtLeast(kotlin.math.min(newRed, 255))
                newGreen = (alpha*(newGreen-rgbAverage)+rgbAverage).toInt()
                newGreen = 0.coerceAtLeast(kotlin.math.min(newGreen, 255))
                newBlue = (alpha*(newBlue-rgbAverage)+rgbAverage).toInt()
                newBlue = 0.coerceAtLeast(kotlin.math.min(newBlue, 255))

                newBitmap.setPixel(x,y,Color.rgb(newRed,newGreen,newBlue))
            }
        }

        // no need to proceed with other filters
        if (gamma.compareTo(1) == 0) {
            return newBitmap
        }

        //Apply gamma
        for (x in 0 until defaultBitmap.width) {
            for (y in 0 until defaultBitmap.height) {
                var newRed: Int
                var newGreen: Int
                var newBlue: Int
                val c = newBitmap.getPixel(x,y)

                newRed = Color.red(c)
                newGreen = Color.green(c)
                newBlue = Color.blue(c)

                newRed = (255.0 * ((newRed / 255.0).pow(gamma))).toInt()
                newRed= 0.coerceAtLeast(kotlin.math.min(newRed, 255))
                newGreen = (255.0 * ((newGreen / 255.0).pow(gamma))).toInt()
                newGreen = 0.coerceAtLeast(kotlin.math.min(newGreen, 255))
                newBlue = (255.0 * ((newBlue / 255.0).pow(gamma))).toInt()
                newBlue = 0.coerceAtLeast(kotlin.math.min(newBlue, 255))

                newBitmap.setPixel(x,y,Color.rgb(newRed, newGreen, newBlue))
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

    private fun onSliderChanges() {

        lastJob?.cancel()  // we don't need the lastJob computations anymore. If it was already computed nothing happens.

        lastJob = GlobalScope.launch(Dispatchers.Default) {
            //  the execution inside this block is already asynchronous as you can see by the print below

            // if the current image is null, we have nothing to do with it
            val bitmap = (ivPhoto.drawable as BitmapDrawable?)?.bitmap ?: return@launch

            // if you need to make some computations and wait for the result, you can use the async block
            // it will schedule a new coroutine task and return a Deferred object that will have the returned value
            val allFiltersAppliedCopyDeferred: Deferred<Bitmap> = this.async {
                /* invoke your computation that returns a value */
                return@async applyAllFilters(
                    slBrightness.value.toInt(),
                    slContrast.value.toInt(),
                    slSaturation.value.toInt(),
                    slGamma.value.toDouble(),
                    defaultBitmap)
            }
            // here we wait for the result
            val newBitmap: Bitmap = allFiltersAppliedCopyDeferred.await()

            runOnUiThread {
                // here we are already on the main thread, as you can see on the println below
                ivPhoto.setImageBitmap(newBitmap)
            }
        }
    }

}