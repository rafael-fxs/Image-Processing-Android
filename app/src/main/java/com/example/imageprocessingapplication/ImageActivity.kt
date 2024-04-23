package com.example.imageprocessingapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.Glide
import com.example.imageprocessingapplication.databinding.ActivityImageBinding
import java.io.InputStream


class ImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageBinding

    private lateinit var imageView: ImageView
    private lateinit var imageUri: Uri

    private lateinit var bResetFilter: ImageView
    private lateinit var bGrayFilter: ImageView
    private lateinit var bNegativeFilter: ImageView
    private lateinit var bSepiaFilter: ImageView

    private lateinit var originalBitmap: Bitmap

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

        startPreviewFilters()
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
            imageView.setImageBitmap(applyFilter(FilterType.RESET))
        }

        bGrayFilter.setOnClickListener{
            imageView.setImageBitmap(applyFilter(FilterType.GRAY))
        }


        bNegativeFilter.setOnClickListener{
            imageView.setImageBitmap(applyFilter(FilterType.NEGATIVE))
        }


        bSepiaFilter.setOnClickListener{
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
}