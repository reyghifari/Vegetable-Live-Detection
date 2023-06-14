package com.hann.vegetablelivedetection

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.hann.vegetablelivedetection.databinding.ActivityDetailResultBinding

class DetailResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailResultBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailResultBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val result = intent.getStringExtra(EXTRA_RESULT)

        if (result != null){
            binding.tvResult.text = result
        }

    }

    companion object{
        const val EXTRA_RESULT = "RESULT"
    }
}