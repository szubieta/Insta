package com.example.insta.views.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.insta.R
import com.example.insta.utils.MiSharedPreferences

class MainActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        MiSharedPreferences.init(this)
        /*if(MiSharedPreferences.getPreferencesString("token") != "") {
            val intent = Intent(applicationContext, Activity2::class.java);
            startActivity(intent)
            finish()
        }*/
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }


}



