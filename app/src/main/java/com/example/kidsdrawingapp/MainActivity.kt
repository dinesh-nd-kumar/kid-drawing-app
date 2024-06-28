package com.example.kidsdrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint : ImageButton? = null

    private val openGalleryLancher : ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult(),){
            result ->
            if (result.resultCode == RESULT_OK && result.data != null){
                val imageBackground: ImageView =findViewById(R.id.iv_background_image)
                imageBackground.setImageURI(result.data?.data)
            }
        }

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if (isGranted){
                    Toast.makeText(this,"permission granted",Toast.LENGTH_LONG).show()
                    val pickIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLancher.launch(pickIntent)

                }
                else{
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this,"permission denied",Toast.LENGTH_LONG).show()

                    }
                }
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView!!.setSizeForBrush(20F)

        val mLinearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)
        mImageButtonCurrentPaint = mLinearLayoutPaintColors[3] as ImageButton
        mImageButtonCurrentPaint?.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallet_pressed))


        val ibBrush: ImageButton = findViewById(R.id.ib_brush)
        ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        val ibGallery : ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener {
            requestStoragePermission()
        }

        val ibUndo: ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener {
            drawingView?.onUndoClicked()
        }
        val ibRedo: ImageButton = findViewById(R.id.ib_redo)
        ibRedo.setOnClickListener {
            drawingView?.onRedoClicked()
        }

        val ibSave: ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener {
            if (isReadStorageAllowed()){
                lifecycleScope.launch {
                    val frameLayout : FrameLayout = findViewById(R.id.drawing_view_container)
                    saveBitmapFile(getBitmapfromView(frameLayout))




                }
            }
            
        }


    }

    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED

    }

    private fun requestStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationaleDialog("Kids Drwaing App", "need to access external storage")

        }
        else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ))
        }
    }

    fun onPaintClicked(v : View){
        if (v != mImageButtonCurrentPaint){
            val imagebutton = v as ImageButton
            val colorTag = imagebutton.tag.toString()
            drawingView!!.setColorForBrush(colorTag)

            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal))
            imagebutton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_pressed))
            mImageButtonCurrentPaint = imagebutton


        }

    }
    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
            brushDialog.setTitle("Brush Size")

        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10F)
            brushDialog.dismiss()
        }

        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20F)
            brushDialog.dismiss()
        }

        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(30F)
            brushDialog.dismiss()
        }

        brushDialog.show()
    }
    private fun showRationaleDialog(title:String , message :String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK"){dialog, _ ->
                dialog.dismiss()
                requestPermission.launch(arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ))
            }
        builder.create().show()


    }

    private fun getBitmapfromView(v : View): Bitmap{
        val rBitmap = Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(rBitmap)
        val bgDrawable = v.background
        if (bgDrawable != null)
            bgDrawable.draw(canvas)
        else
            canvas.drawColor(Color.WHITE)
        v.draw(canvas)

        return rBitmap
    }

    private suspend fun saveBitmapFile(bitmap: Bitmap): String{
        var result = ""
        withContext(Dispatchers.IO){
            if (bitmap != null){
                val byte = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG,98,byte)
                val pictureDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(pictureDir.absolutePath.toString()
                        + File.separator + "kids_drawing_app" + System.currentTimeMillis()/1000 +".jpeg")

                val fileOutputStream = FileOutputStream(file)
                fileOutputStream.write(byte.toByteArray())
                fileOutputStream.close()
                result = file.absolutePath

                runOnUiThread {
                    if (result.isNotEmpty()){
                        Toast.makeText(this@MainActivity, "saved at: $result", Toast.LENGTH_LONG).show()
                    }
                    else
                    Toast.makeText(this@MainActivity, "something wrong", Toast.LENGTH_SHORT).show()
                }


            }

        }
        return result
    }
}
