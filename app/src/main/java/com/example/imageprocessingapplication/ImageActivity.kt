package com.example.imageprocessingapplication

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.imageprocessingapplication.databinding.ActivityImageBinding
import com.example.imageprocessingapplication.databinding.ActivityMainBinding

class ImageActivity : AppCompatActivity() {

    lateinit var binding: ActivityImageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this ,R.layout.activity_image)

        val imageUri = intent.getStringExtra("imageUri")
        val imageView: ImageView = binding.imageView;

        Glide.with(this)
            .load(imageUri)
            .into(imageView)

        binding.bBackMain.setOnClickListener{
            finish()
        }
    }
}