package com.example.kidsdrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint : ImageButton? = null

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if (isGranted){
                    Toast.makeText(this,"permission granted",Toast.LENGTH_LONG).show()
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
        ibGallery.setOnClickListener { requestStoragePermission() }


    }

    private fun requestStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationaleDialog("Kids Drwaing App", "need to access external storage")

        }
        else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
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
}
