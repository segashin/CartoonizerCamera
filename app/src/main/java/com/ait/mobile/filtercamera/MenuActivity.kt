package com.ait.mobile.filtercamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.*
import kotlinx.android.synthetic.main.activity_menu.*
import java.lang.StringBuilder
import androidx.core.content.ContextCompat.checkSelfPermission as checkSelfPermission1

class MenuActivity : AppCompatActivity() {

    companion object{
        const val GALLERY_MODE = 0
        const val CAMERA_MODE = 1

        const val PERMISSION_REQUEST_CODE = 1001

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        btnGallery.setOnClickListener{
            startActivity(Intent(this, MainActivity::class.java).apply{
                putExtra("MODE", GALLERY_MODE)
            })
        }

        btnCamera.setOnClickListener{
            startActivity(Intent(this, MainActivity::class.java).apply{
                putExtra("MODE", CAMERA_MODE)
            })
        }

        requestNeededPermission()
    }


    private fun requestNeededPermission(){
        var permArray = mutableListOf<String>()
        if(checkSelfPermission1(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            permArray.add(Manifest.permission.CAMERA)
        }else{
            //permission already granted
        }
        if(checkSelfPermission1(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permArray.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }else{
            //permission already granted
        }
        if(checkSelfPermission1(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permArray.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }else{
            //permission already granted
        }

        if(!permArray.isEmpty()){
            ActivityCompat.requestPermissions(this, permArray.toTypedArray(), PERMISSION_REQUEST_CODE)
        }

    }
}
