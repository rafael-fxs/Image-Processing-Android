package com.example.imageprocessingapplication

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.Glide
import com.example.imageprocessingapplication.databinding.ActivityImageBinding
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.InputStream


class ImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageBinding

    private lateinit var imageView: ImageView
    private lateinit var imageUri: Uri

    private lateinit var bResetFilter: ImageView
    private lateinit var bGrayFilter: ImageView
    private lateinit var bNegativeFilter: ImageView
    private lateinit var bSepiaFilter: ImageView

    private lateinit var seekBarBrightness: SeekBar
    private lateinit var seekBarContrast: SeekBar

    private lateinit var originalBitmap: Bitmap
    private lateinit var actualBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this ,R.layout.activity_image)


        imageUri = Uri.parse(intent.getStringExtra("imageUri"))
        originalBitmap = loadBitmapFromUri(imageUri)

        imageView = binding.imageView
        Glide.with(this)
            .load(imageUri)
            .into(imageView)


        binding.bBackMain.setOnClickListener{
            finish()
        }

        binding.bSaveImage.setOnClickListener{
            saveImage()
        }

        startPreviewFilters()
        actualBitmap = originalBitmap
        startSeekBars()
    }

    private fun startSeekBars() {
        seekBarBrightness = binding.seekBarBrightness
        seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applyBrightness(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarContrast = binding.seekBarContrast
        seekBarContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val contrast = progress / 100f
                applyContrast(contrast)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun startPreviewFilters() {
        bResetFilter = binding.bResetFilter
        bGrayFilter = binding.bGrayFilter
        bNegativeFilter = binding.bNegativeFilter
        bSepiaFilter = binding.bSepiaFilter

        Glide.with(this)
            .load(imageUri)
            .into(bResetFilter)

        Glide.with(this)
            .load(applyFilter(FilterType.GRAY))
            .into(bGrayFilter)

        Glide.with(this)
            .load(applyFilter(FilterType.NEGATIVE))
            .into(bNegativeFilter)

        Glide.with(this)
            .load(applyFilter(FilterType.SEPIA))
            .into(bSepiaFilter)

        bResetFilter.setOnClickListener{
            resetSeek()
            imageView.setImageBitmap(applyFilter(FilterType.RESET))
        }

        bGrayFilter.setOnClickListener{
            resetSeek()
            imageView.setImageBitmap(applyFilter(FilterType.GRAY))
        }


        bNegativeFilter.setOnClickListener{
            resetSeek()
            imageView.setImageBitmap(applyFilter(FilterType.NEGATIVE))
        }


        bSepiaFilter.setOnClickListener{
            resetSeek()
            imageView.setImageBitmap(applyFilter(FilterType.SEPIA))
        }
    }

    private fun getExifOrientation(uri: Uri): Int {
        val exifInterface = ExifInterface(contentResolver.openInputStream(uri)!!)
        return exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val rotation = getExifOrientation(uri)
        return rotateBitmap(bitmap, rotation)
    }

    private fun rotateBitmap(bitmap: Bitmap, rotation: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(getRotationAngle(rotation))
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun getRotationAngle(rotation: Int): Float {
        return when (rotation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }

    private fun applyFilter(filterType: FilterType): Bitmap {
        if (filterType == FilterType.RESET) {
            return originalBitmap
        }

        val resultBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val paint = Paint()
        val colorMatrix = getColorMatrix(filterType)
        val filter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = filter
        canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
        actualBitmap = resultBitmap
        return resultBitmap
    }

    private fun getColorMatrix(filterType: FilterType): ColorMatrix {
        return when (filterType) {
            FilterType.GRAY -> ColorMatrix().apply { setSaturation(0f) }
            FilterType.NEGATIVE -> ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))
            FilterType.SEPIA -> ColorMatrix().apply {
                setScale(1f, 0.8f, 0.5f, 1f)
            }
            else -> ColorMatrix()
        }
    }

    private fun applyBrightness(brightnessValue: Int) {
        val adjustedBitmap = adjustBrightness(brightnessValue)
        imageView.setImageBitmap(adjustedBitmap)
    }
    private fun adjustBrightness(value: Int): Bitmap {
        val resultBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val colorMatrix = ColorMatrix()
        colorMatrix.set(floatArrayOf(
            1f, 0f, 0f, 0f, value.toFloat(),
            0f, 1f, 0f, 0f, value.toFloat(),
            0f, 0f, 1f, 0f, value.toFloat(),
            0f, 0f, 0f, 1f, 0f
        ))

        val colorFilter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = colorFilter

        canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
        actualBitmap = resultBitmap
        return resultBitmap
    }

    private fun applyContrast(contrastValue: Float) {
        val adjustedBitmap = adjustContrast(contrastValue)
        imageView.setImageBitmap(adjustedBitmap)
    }

    private fun adjustContrast(value: Float): Bitmap {
        val resultBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val colorMatrix = ColorMatrix()
        val scale = value + 1f
        val translate = -(128 * scale) + 128
        colorMatrix.set(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        val colorFilter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = colorFilter

        canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
        return resultBitmap
    }

    private fun resetSeek() {
        seekBarBrightness.progress = 0
        seekBarContrast.progress = 0
    }

    private fun saveImage() {
        val fileName: String = System.currentTimeMillis().toString().replace(":", ".") + ".jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, fileName)
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        val contentResolver = this.contentResolver
        val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            val fileDescriptor = contentResolver.openFileDescriptor(it, "w")?.fileDescriptor
            val outputStream = FileOutputStream(fileDescriptor)

            // Calculating the desired quality for a 1MB file size
            val targetFileSize = 1024 * 1024 // 1MB in bytes
            var quality = 100
            var bitmapStream: ByteArrayOutputStream? = null
            var bitmapBytes: ByteArray? = null

            while (outputStream.channel.size() > targetFileSize && quality > 0) {
                bitmapStream = ByteArrayOutputStream()
                actualBitmap.compress(Bitmap.CompressFormat.JPEG, quality, bitmapStream)
                bitmapBytes = bitmapStream.toByteArray()
                quality -= 5 // Decrease quality gradually
            }

            bitmapBytes?.let {
                outputStream.write(it)
            }

            outputStream.flush()
            outputStream.close()
            bitmapStream?.close()
        }

        Toast.makeText(
            this@ImageActivity,
            "Imagem salva!",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }
}