package com.example.ejs

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.OutputStream
import kotlinx.android.synthetic.main.fragment_qr.view.*
import java.io.File
import java.io.FileOutputStream

class QRFragment : DialogFragment() {
    lateinit var v: View
    var kd = ""
    lateinit var urlClass: UrlClass

    private val PERMISSION_REQUEST_CODE = 1
    private val FILENAME = "qrcode.png"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        v = inflater.inflate(R.layout.fragment_qr, container, false)

        urlClass = UrlClass()

        val data = arguments
        kd = data?.get("kode").toString()
        Toast.makeText(v.context, kd, Toast.LENGTH_SHORT).show()

        val url = urlClass.local+"detail_arsip.php?id="+kd
        val bitmap = generateBarcode(url)
        v.imageQr.setImageBitmap(bitmap)

        val filename = "qrcode_"+kd+".png"

        v.imageQr.setOnLongClickListener {
            var contextMenu = PopupMenu(it.context, it)
            contextMenu.menuInflater.inflate(R.menu.unduh_qr, contextMenu.menu)
            contextMenu.setOnMenuItemClickListener { item ->
                when(item.itemId){
                    R.id.unduhQr -> {
                        generateQRCodeAndDownload()
                    }
                }
                false
            }
            contextMenu.show()
            true
        }

        return v
    }

    private fun generateQRCodeAndDownload() {
        val url = urlClass.local+"detail_arsip.php?id="+kd
        val bitmap = generateBarcode(url)

        if (isWritePermissionGranted()) {
            saveBitmap(bitmap, FILENAME)
        } else {
            requestWritePermission()
        }
    }

    private fun generateBarcode(content: String): Bitmap {
        val width = 400
        val height = 400
        val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height)
        val barcodeEncoder = BarcodeEncoder()
        return barcodeEncoder.createBitmap(bitMatrix)
    }

    private fun saveBitmap(bitmap: Bitmap, filename: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveBitmapToMediaStore(bitmap, filename)
        } else {
            saveBitmapToExternalStorage(bitmap, filename)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveBitmapToMediaStore(bitmap: Bitmap, filename: String) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QRCode")
        }

        activity?.contentResolver?.let { contentResolver ->
            val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            imageUri?.let {
                val outputStream: OutputStream? = contentResolver.openOutputStream(it)
                outputStream?.let {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.flush()
                    outputStream.close()
                    showToast("QR Code saved successfully")
                }
            }
        }
    }

    private fun saveBitmapToExternalStorage(bitmap: Bitmap, filename: String) {
        val directory = File(Environment.getExternalStorageDirectory().toString() + "/QRCode")
        directory.mkdirs()

        val file = File(directory, filename)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        showToast("QR Code saved successfully")
    }

    private fun isWritePermissionGranted(): Boolean {
        val writePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        val permissionResult = ContextCompat.checkSelfPermission(this.requireContext(), writePermission)
        return permissionResult == PackageManager.PERMISSION_GRANTED
    }

    private fun requestWritePermission() {
        ActivityCompat.requestPermissions(
            this.requireActivity(),
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generateQRCodeAndDownload()
            } else {
                showToast("Permission denied")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this.context, message, Toast.LENGTH_SHORT).show()
    }
}