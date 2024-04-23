package com.example.imageprocessingapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.imageprocessingapplication.databinding.ActivityImageBinding
import java.io.InputStream


class ImageActivity : AppCompatActivity() {
    lateinit var binding: ActivityImageBinding

    private lateinit var imageView: ImageView
    private lateinit var bResetFilter: ImageView
    private lateinit var bGrayFilter: ImageView

    private lateinit var originalBitmap: Bitmap
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this ,R.layout.activity_image)

        val imageUri = Uri.parse(intent.getStringExtra("imageUri"))
        originalBitmap = loadBitmapFromUri(imageUri)

        imageView = binding.imageView;
        Glide.with(this)
            .load(imageUri)
            .into(imageView)

        bResetFilter = binding.bResetFilter;
        bResetFilter.setOnClickListener{
            resetFilter()
        }
        Glide.with(this)
            .load(imageUri)
            .override(80, 100)
            .into(bResetFilter)

        bGrayFilter = binding.bGrayFilter;
        bGrayFilter.setOnClickListener{
            imageView.setImageBitmap(getGrayscaleFilter())
        }
        Glide.with(this)
            .load(getGrayscaleFilter())
            .override(80, 100)
            .into(bGrayFilter)

        binding.bBackMain.setOnClickListener{
            finish()
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val rotation = getRotationFromExif(uri)
        return if (rotation != 0) {
            rotateBitmap(bitmap, rotation)
        } else {
            bitmap
        }
    }

    private fun getRotationFromExif(uri: Uri): Int {
        var rotation = 0
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        inputStream.use { stream ->
            stream?.let {
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotation = 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotation = 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotation = 270
                }
            }
        }
        return rotation
    }
    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun resetFilter() {
        imageView.setImageBitmap(originalBitmap)
    }

    private fun getGrayscaleFilter(): Bitmap {
        val grayBitmap: Bitmap = originalBitmap.copy(originalBitmap.config, true)
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)

        val filter = ColorMatrixColorFilter(colorMatrix)
        val paint = Paint()
        paint.colorFilter = filter

        val canvas = Canvas(grayBitmap)
        canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
        return grayBitmap
    }
}