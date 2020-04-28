package com.example.insta.views.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.insta.R
import com.example.insta.utils.MiSharedPreferences

class MainActivity : AppCompatActivity(){


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MiSharedPreferences.init(this)
    }

    fun changeLayout(layout: Int){
        setContentView(layout)
    }

}

