package com.example.imageprocessingapplication

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.Glide
import com.example.imageprocessingapplication.databinding.ActivityImageBinding
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils.bitmapToMat
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.InputStream
import java.io.OutputStream


class ImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageBinding

    private lateinit var imageView: ImageView
    private lateinit var imageUri: Uri

    private lateinit var seekBarBrightness: SeekBar
    private lateinit var seekBarContrast: SeekBar

    private lateinit var originalBitmap: Bitmap
    private lateinit var actualBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this ,R.layout.activity_image)

        if (!OpenCVLoader.initDebug())
            Log.e("OpenCVLoader", "Unable to load OpenCV!");
        else
            Log.d("OpenCVLoader", "OpenCV loaded Successfully!");

        imageUri = Uri.parse(intent.getStringExtra("imageUri"))
        imageView = binding.imageView
        originalBitmap = loadBitmapFromUri(imageUri)
        actualBitmap = originalBitmap

        initFilterButton(binding.bResetFilter, FilterType.RESET)
        initFilterButton(binding.bGrayFilter, FilterType.GRAY)
        initFilterButton(binding.bNegativeFilter, FilterType.NEGATIVE)
        initFilterButton(binding.bSepiaFilter, FilterType.SEPIA)
        initFilterButton(binding.bSobelFilter, FilterType.SOBEL)
        initFilterButton(binding.bEmbossFilter, FilterType.EMBOSS)
        initFilterButton(binding.bBlurFilter, FilterType.BLUR)
        loadImageWithGlide(binding.imageView, originalBitmap)

        binding.bBackMain.setOnClickListener{
            finish()
        }

        binding.bSaveImage.setOnClickListener{
            saveImageToGallery()
            finish()
        }

        startSeekBars()
    }

    private fun loadImageWithGlide(imageView: ImageView, bitmap: Bitmap,) {
        Glide.with(this)
            .load(bitmap)
            .into(imageView)
    }

    private fun initFilterButton(button: ImageView, filterType: FilterType) {
        button.setOnClickListener {
            resetSeek()
            imageView.setImageBitmap(applyFilter(filterType))
        }

        loadImageWithGlide(button, applyFilter(filterType))
    }

    private fun reduceResolution(inputBitmap: Bitmap, scaleFactor: Float): Bitmap {
        val width = (inputBitmap.width * scaleFactor).toInt()
        val height = (inputBitmap.height * scaleFactor).toInt()
        return Bitmap.createScaledBitmap(inputBitmap, width, height, true)
    }


    private fun startSeekBars() {
        seekBarBrightness = binding.seekBarBrightness
        seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                seekBarContrast.progress = 0
                applyBrightness(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarContrast = binding.seekBarContrast
        seekBarContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                seekBarBrightness.progress = 0
                val contrast = progress / 100f
                applyContrast(contrast)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
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
        actualBitmap = when (filterType) {
            FilterType.GRAY -> applyNativeFilter(FilterType.GRAY)
            FilterType.NEGATIVE -> applyNativeFilter(FilterType.NEGATIVE)
            FilterType.SEPIA -> applyNativeFilter(FilterType.SEPIA)
            FilterType.SOBEL -> applySobelFilter()
            FilterType.EMBOSS -> applyEmbossFilter()
            FilterType.BLUR -> applyBlurFilter()
            else -> originalBitmap
        }
        return actualBitmap
    }

    private fun applyNativeFilter(filterType: FilterType): Bitmap {
        val bitmap: Bitmap = originalBitmap
        val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(getColorMatrix(filterType))
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
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

        actualBitmap = resultBitmap
        return resultBitmap
    }
    private fun applySobelFilter(): Bitmap {
        val bitmap: Bitmap = originalBitmap
        val mat = Mat()
        bitmapToMat(bitmap, mat)

        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)
        val sobelX = Mat()
        val sobelY = Mat()
        Imgproc.Sobel(grayMat, sobelX, CvType.CV_16S, 1, 0)
        Imgproc.Sobel(grayMat, sobelY, CvType.CV_16S, 0, 1)
        Core.convertScaleAbs(sobelX, sobelX)
        Core.convertScaleAbs(sobelY, sobelY)
        val sobelMat = Mat()
        Core.addWeighted(sobelX, 0.5, sobelY, 0.5, 0.0, sobelMat)

        val resultBitmap = Bitmap.createBitmap(sobelMat.cols(), sobelMat.rows(), Bitmap.Config.ARGB_8888)
        matToBitmap(sobelMat, resultBitmap)

        mat.release()
        grayMat.release()
        sobelX.release()
        sobelY.release()
        sobelMat.release()
        return resultBitmap;
    }

    private fun applyEmbossFilter(): Bitmap {
        val bitmap: Bitmap = originalBitmap
        val mat = Mat()
        bitmapToMat(bitmap, mat)

        val embossMat = Mat()
        Imgproc.cvtColor(mat, embossMat, Imgproc.COLOR_BGR2GRAY)

        val kernel = Mat(3, 3, CvType.CV_32F)
        kernel.put(0, 0, -2.0, -1.0, 0.0)
        kernel.put(1, 0, -1.0, 2.0, 1.0) // Adiciona um deslocamento de intensidade
        kernel.put(2, 0, 0.0, 1.0, 2.0)

        Imgproc.filter2D(embossMat, embossMat, CvType.CV_8U, kernel, Point(-1.0, -1.0), 0.0)

        val resultBitmap = Bitmap.createBitmap(embossMat.cols(), embossMat.rows(), Bitmap.Config.ARGB_8888)
        matToBitmap(embossMat, resultBitmap)

        mat.release()
        embossMat.release()

        return resultBitmap
    }

    private fun applyBlurFilter(): Bitmap {
        val bitmap: Bitmap = originalBitmap
        val mat = Mat()
        bitmapToMat(bitmap, mat)

        val mask = Mat(mat.size(), CvType.CV_8U)
        mask.setTo(Scalar(0.0))
        val center = Point(mat.width() / 2.0, mat.height() / 2.0)
        Imgproc.circle(mask, center, (mat.width() / 5), Scalar(255.0), -1)

        val blurMat = Mat()
        val size = Size(45.0, 45.0)
        Imgproc.GaussianBlur(mat, blurMat, size, 0.0, 0.0, Core.BORDER_DEFAULT)
        mat.copyTo(blurMat, mask)

        val resultBitmap = Bitmap.createBitmap(blurMat.cols(), blurMat.rows(), Bitmap.Config.ARGB_8888)

        matToBitmap(blurMat, resultBitmap)

        mat.release()
        mask.release()
        blurMat.release()

        return resultBitmap
    }


    private fun resetSeek() {
        seekBarBrightness.progress = 0
        seekBarContrast.progress = 0
    }


    private fun saveImage(): Uri? {
        val bitmap = actualBitmap
        val imageName: String = System.currentTimeMillis().toString().replace(":", ".") + ".jpg"
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())

        val saveUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentResolver = this.contentResolver
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } else {
            val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val imageFile = File(storageDir, imageName)
            Uri.fromFile(imageFile)
        }

        saveUri?.let { uri ->
            val outputStream: OutputStream? = contentResolver.openOutputStream(uri)
            if (outputStream != null) {
                val resizedBitmap = resizeBitmap(bitmap)
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()

                MediaScannerConnection.scanFile(this, arrayOf(uri.path), null, null)
                return uri
            }
        }

        return null
    }

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val newWidth = width / 2
        val newHeight = (newWidth * height) / width

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun saveImageToGallery() {
        val savedImagePath = saveImage()
        if (savedImagePath != null) {
            Toast.makeText(this, "Image saved at: $savedImagePath", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Error saving image.", Toast.LENGTH_SHORT).show()
        }
    }
}