package com.ait.mobile.filtercamera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.reflect.typeOf

class MainActivity : AppCompatActivity() {

    companion object{
        private const val CAMERA_REQUEST_CODE = 1002
        private val GALLERY_REQUEST_CODE = 1003
    }

    var bmp :Bitmap? = null
    var currentPhotoPath: String? = null

    //cartoonize config
    var shadow: Int = 0
    var abstraction: Double = 0.0
    var edge: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if(!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "error_openCV")
        }

        setMode()

        btnRotate.setOnClickListener{
            ivPhoto.rotation += 90
        }
        btnCartoonize.setOnClickListener{
            //cartoonize
            var img = Mat()
            Utils.bitmapToMat(bmp, img)
            Thread{
                var mat = cartoonize(img, shadow, abstraction, edge) //pass mat returns mat
                //draw the output
                var output = mat
                runOnUiThread{
                    ivPhoto.setImageBitmap(output)
                }
            }.start()
        }

        setSbShadow()
        setSbAbstraction()
        setSbEdge()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_save -> {
            var photo = getBitmapFromImageView(ivPhoto)
            saveBitmapAsJpg(photo)
            true
        }
        else -> {
            // nothing
            super.onOptionsItemSelected(item)
        }
    }

    private fun saveBitmapAsJpg(photo:Bitmap){
        try{
            var extStageDir : File = Environment.getExternalStorageDirectory()
            var subdir: String = extStageDir.absolutePath + "/" + Environment.DIRECTORY_PICTURES + "/" + getString(R.string.app_name)
            var subdirf = File(subdir)
            if(!subdirf.exists()){
                subdirf.mkdir()
            }
            var filename = getFileName()
            var file = File(subdir, filename)
            var outStream = FileOutputStream(file)
            photo.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            outStream.close()
            galleryAddPic(file)
            Toast.makeText(this, getString(R.string.image_saved) + " " + filename, Toast.LENGTH_SHORT).show()
        }catch(e: FileNotFoundException){
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.image_save_failed), Toast.LENGTH_SHORT).show()
        }catch (e: IOException){
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.image_save_failed), Toast.LENGTH_SHORT).show()
        }catch (e: Exception){
            Toast.makeText(this, getString(R.string.image_save_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(): String{
        var c : Calendar = Calendar.getInstance()
        var s : String = c.get(Calendar.YEAR).toString() + "_" +
                (c.get(Calendar.MONTH)+1).toString() +
                "_" + c.get(Calendar.DAY_OF_MONTH).toString() +
                "_" + c.get(Calendar.HOUR_OF_DAY).toString() +
                "_" + c.get(Calendar.MINUTE).toString() +
                "_" + c.get(Calendar.SECOND).toString() +
                "_" + c.get(Calendar.MILLISECOND).toString() +
                ".png"
        return s
    }

    private fun setMode() {
        val mode = intent.extras["MODE"]
        if (mode == MenuActivity.GALLERY_MODE) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.setType("*/*")
            startActivityForResult(intent, GALLERY_REQUEST_CODE)

            btnGallery.text = getString(R.string.gallery)
            btnGallery.setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.setType("*/*")
                startActivityForResult(intent, GALLERY_REQUEST_CODE)
            }
        } else if (mode == MenuActivity.CAMERA_MODE) {
            //startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), CAMERA_REQUEST_CODE)
            dispatchTakePictureIntent()
            btnGallery.text = getString(R.string.camera)
            btnGallery.setOnClickListener {
                dispatchTakePictureIntent()
            }
        }
    }

    private fun setSbEdge() {
        sbEdge.min = -5
        sbEdge.max = 5
        sbEdge.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    tvEdge.text = getString(R.string.edge_level) + progress.toString()
                    edge = progress.toDouble()
                }
            }
        )
    }

    private fun setSbAbstraction() {
        sbAbstraction.min = -5
        sbAbstraction.max = 5
        sbAbstraction.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    tvAbstraction.text = getString(R.string.abstraction_level) + progress.toString()
                    abstraction = progress.toDouble()
                }
            }
        )
    }

    private fun setSbShadow() {
        sbShadow.min = -100
        sbShadow.max = 100
        sbShadow.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    tvShadow.text = getString(R.string.shadow_leve) + progress.toString()
                    shadow = progress
                }
            }
        )
    }


    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Toast.makeText(this, getString(R.string.could_not_save_the_photo), Toast.LENGTH_SHORT).show()
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.ait.mobile.filtercamera",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
                }
            }
        }
    }

    private fun cartoonize(imgx: Mat, shadow_level: Int, abstraction_level: Double, edge_level:Double) : Bitmap{

        var mat1 = Mat() // Gray base
        var mat4 = Mat() // colored base

        var emat1 = Mat() // dense edges
        var emat2 = Mat() // sparse edges
        val minWH = 800.0
        var rW = minWH/imgx.width()
        var rH = minWH/imgx.height()
        var downsizeRatio = 1.0
        if(rW < 1 || rH < 1){
            if(rW > rH){
                downsizeRatio = rH.toDouble()
            }else{
                downsizeRatio = rW.toDouble()
            }
        }
        var img = Mat()
        Imgproc.resize(imgx, img, Size(imgx.width()*downsizeRatio, imgx.height()*downsizeRatio))
        Imgproc.cvtColor(img, mat1, Imgproc.COLOR_RGB2GRAY)
        Imgproc.GaussianBlur(mat1, mat1, Size(11-edge_level*2, 11-edge_level*2), 0.0, 0.0)
        Imgproc.Canny(mat1, emat1, 40.0, 45.0) //edge 1 many edges
        Imgproc.Canny(mat1, emat2, 50.0, 50.0) //edge 2 less edges
        Imgproc.GaussianBlur(img, mat4, Size(11+abstraction_level*2, 11+abstraction_level*2), 0.0, 0.0) //colored base
        var result = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888) //result mat

        var darkshadow = 40
        var darkshadow_thresh = (60 + shadow_level).coerceAtLeast(0)
        var lightshadow = 25
        var lightshadow_thresh = (100 + shadow_level).coerceAtLeast(0)

        for(y in 0..result.height-1){
            for(x in 0..result.width-1){
                //result[x,y].set(0, rgb[0])
                var mat1_gray = mat1.get(y,x)
                var mat4_rgba = mat4.get(y,x)

                //edge 1
                var eval1 = emat1.get(y,x)

                //edge 2
                var eval2 = emat2.get(y,x)

                //result.set(x, y, Color.argb(rgba[3].toInt(), (rgba[0]/2).toInt(), (rgba[1]/2).toInt(), (rgba[2]/2).toInt()))
                //result.set(x, y, Color.argb(mat4_rgba[3].toInt(), mat4_rgba[0].toInt(), mat4_rgba[1].toInt(), mat4_rgba[2].toInt()))

                //draw edges 1 dense
                if(eval1[0] <= 100){
                    // same
                    //result.set(x, y, Color.argb(mat4_rgba[3].toInt(), mat4_rgba[0].toInt(), mat4_rgba[1].toInt(), mat4_rgba[2].toInt()))
                    result.set(x, y, Color.argb(255, mat4_rgba[0].toInt(), mat4_rgba[1].toInt(), mat4_rgba[2].toInt()))
                }else{
                    //draw edges
                    //result.set(x, y, Color.argb(mat4_rgba[3].toInt(), (mat4_rgba[0]*0.7).toInt(), (mat4_rgba[1]*0.7).toInt(), (mat4_rgba[2]*0.7).toInt()))
                    result.set(x, y, Color.argb(255, (mat4_rgba[0]*0.6).toInt(), (mat4_rgba[1]*0.6).toInt(), (mat4_rgba[2]*0.6).toInt()))
                }
                //result.set(x, y, Color.argb(mat4_rgba[3].toInt(), mat4_rgba[0].toInt(), mat4_rgba[1].toInt(), mat4_rgba[2].toInt()))

                //draw shadow
                if(mat1_gray[0]<=lightshadow_thresh){
                    //result.set(x, y, Color.argb(mat4_rgba[3].toInt(), (mat4_rgba[0]*0.9).toInt(), (mat4_rgba[1]*0.9).toInt(), (mat4_rgba[2]*0.9).toInt()))
                    //result.set(x, y, Color.argb(255, (mat4_rgba[0]*lightshadow).toInt(), (mat4_rgba[1]*lightshadow).toInt(), (mat4_rgba[2]*lightshadow).toInt()))
                    result.set(x, y, Color.argb(255, (mat4_rgba[0]-lightshadow).toInt().coerceAtLeast(0), (mat4_rgba[1]-lightshadow).toInt().coerceAtLeast(0), (mat4_rgba[2]-lightshadow).toInt().coerceAtLeast(0)))
                }else if(mat4_rgba[0] <= darkshadow_thresh){
                    //result.set(x, y, Color.argb(mat4_rgba[3].toInt(), (mat4_rgba[0]*0.8).toInt(), (mat4_rgba[1]*0.8).toInt(), (mat4_rgba[2]*0.8).toInt()))
                    //result.set(x, y, Color.argb(255, (mat4_rgba[0]*darkshadow).toInt(), (mat4_rgba[1]*darkshadow).toInt(), (mat4_rgba[2]*darkshadow).toInt()))
                    result.set(x, y, Color.argb(255, (mat4_rgba[0]-darkshadow).toInt().coerceAtLeast(0), (mat4_rgba[1]-darkshadow).toInt().coerceAtLeast(0), (mat4_rgba[2]-darkshadow).toInt().coerceAtLeast(0)))
                }

                //draw edges 2 sparse
                if(eval2[0] >= 100){
                    //result.set(x, y, Color.argb(mat4_rgba[3].toInt(), (mat4_rgba[0]*0.4).toInt(), (mat4_rgba[1]*0.4).toInt(), (mat4_rgba[2]*0.4).toInt()))
                    result.set(x, y, Color.argb(255, 0, 0, 0))
                }
            }
        }
        return result
    }


    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun galleryAddPic(f: File) {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            mediaScanIntent.data = Uri.fromFile(f)
            sendBroadcast(mediaScanIntent)
        }
    }

    private fun setPic() {
        // Get the dimensions of the View
        val targetW: Int = ivPhoto.width
        val targetH: Int = ivPhoto.height
        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true
            val photoW: Int = outWidth
            val photoH: Int = outHeight
            // Determine how much to scale down the image
            val scaleFactor: Int = Math.min(photoW / targetW, photoH / targetH)
            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inPurgeable = true
        }
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions)?.also { bitmap ->
            bmp = bitmap
            ivPhoto.setImageBitmap(bitmap)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultdata: Intent?) {
        if((requestCode == GALLERY_REQUEST_CODE)
            && resultCode == Activity.RESULT_OK) {
            if(resultdata?.data != null) {
                try {
                    val uri: Uri = resultdata.data
                    val parcelFileDesc: ParcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r")
                    val fDesc: FileDescriptor = parcelFileDesc.fileDescriptor
                    bmp = BitmapFactory.decodeFileDescriptor(fDesc)
                    parcelFileDesc.close()
                    ivPhoto.setImageBitmap(bmp)
                    ivPhoto.visibility = View.VISIBLE
                } catch(e: IOException) {
                    e.printStackTrace()
                }
            }
        }else if(requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            setPic()
        }
    }

    private fun getBitmapFromImageView(view: ImageView): Bitmap {
        view.getDrawingCache(true)
        return (view.drawable as BitmapDrawable)?.let { it.bitmap }
    }

}
