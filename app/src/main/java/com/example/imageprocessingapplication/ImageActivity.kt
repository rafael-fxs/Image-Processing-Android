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
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.InputStream
import java.io.OutputStream


class ImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageBinding

    private lateinit var imageView: ImageView
    private lateinit var imageUri: Uri

    private lateinit var bResetFilter: ImageView
    private lateinit var bGrayFilter: ImageView
    private lateinit var bNegativeFilter: ImageView
    private lateinit var bSepiaFilter: ImageView
    private lateinit var bSobelFilter: ImageView

    private lateinit var seekBarBrightness: SeekBar
    private lateinit var seekBarContrast: SeekBar

    private lateinit var originalBitmap: Bitmap
    private lateinit var actualBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this ,R.layout.activity_image)

        if (!OpenCVLoader.initDebug())
            Log.e("rafa", "Unable to load OpenCV!");
        else
            Log.d("rafa", "OpenCV loaded Successfully!");

        imageUri = Uri.parse(intent.getStringExtra("imageUri"))
        originalBitmap = loadBitmapFromUri(imageUri)
        actualBitmap = originalBitmap

        imageView = binding.imageView
        Glide.with(this)
            .load(imageUri)
            .into(imageView)


        binding.bBackMain.setOnClickListener{
            finish()
        }

        binding.bSaveImage.setOnClickListener{
            saveImageToGallery()
            finish()
        }

        startPreviewFilters()
        startSeekBars()
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
        bSobelFilter = binding.bSobelFilter
        val resolution = 0.8f

        Glide.with(this)
            .load(imageUri)
            .into(bResetFilter)

        Glide.with(this)
            .load(reduceResolution(applyFilter(FilterType.GRAY), resolution))
            .into(bGrayFilter)

        Glide.with(this)
            .load(reduceResolution(applyFilter(FilterType.NEGATIVE), resolution))
            .into(bNegativeFilter)

        Glide.with(this)
            .load(reduceResolution(applyFilter(FilterType.SEPIA), resolution))
            .into(bSepiaFilter)

        Glide.with(this)
            .load(reduceResolution(applyFilter(FilterType.SOBEL), resolution))
            .into(bSobelFilter)

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

        bSobelFilter.setOnClickListener{
            resetSeek()
            imageView.setImageBitmap(applyFilter(FilterType.SOBEL))
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
        actualBitmap = when (filterType) {
            FilterType.GRAY -> applyNativeFilter(FilterType.GRAY)
            FilterType.NEGATIVE -> applyNativeFilter(FilterType.NEGATIVE)
            FilterType.SEPIA -> applyNativeFilter(FilterType.SEPIA)
            FilterType.SOBEL -> applySobelFilter()
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
        return resultBitmap
    }
    private fun applySobelFilter(): Bitmap {
        val bitmap: Bitmap = originalBitmap
        val mat = Mat()
        org.opencv.android.Utils.bitmapToMat(bitmap, mat)

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
        org.opencv.android.Utils.matToBitmap(sobelMat, resultBitmap)

        return resultBitmap;
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
                // Redimensionar imagem (opcional)
                val resizedBitmap = resizeBitmap(bitmap) // Função para redimensionar a imagem

                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream) // Ajustar qualidade de compressão
                outputStream.close()

                // Notificar o Media Scanner para atualizar a galeria
                MediaScannerConnection.scanFile(this, arrayOf(uri.path), null, null)
                return uri
            }
        }

        return null
    }

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val newWidth = width / 2 // Largura desejada em pixels
        val newHeight = (newWidth * height) / width // Manter proporção

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun saveImageToGallery() {
        val savedImagePath = saveImage()
        if (savedImagePath != null) {
            Toast.makeText(this, "Imagem salva em: $savedImagePath", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Erro ao salvar a imagem.", Toast.LENGTH_SHORT).show()
        }
    }
}