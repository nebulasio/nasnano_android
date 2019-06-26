package io.nebulas.wallet.android.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import io.nebulas.wallet.android.dialog.ChoosePicWayDialog
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class PhotoChooseUtil {

    companion object {
        val instance: PhotoChooseUtil by lazy {
            PhotoChooseUtil()
        }
    }

    var choosePicWayDialog: ChoosePicWayDialog? = null

    fun compressPicture(imgPath: String): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imgPath, options)

        options.inSampleSize = calculateInSampleSize(options)

        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565
        val afterCompressBm = BitmapFactory.decodeFile(imgPath, options)

        return afterCompressBm
    }


    fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date());
        val imageFileName: String = "JPEG_" + timeStamp + "_";
        val appDir = File(Environment.getExternalStorageDirectory(), "topNews")
        if (!appDir.exists()) {
            appDir.mkdir()
        }
        var imageFile: File? = null
        try {
            imageFile = File.createTempFile(imageFileName, ".jpg", appDir)

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return imageFile!!

    }

    private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
        val height = options.outHeight
        val width = options.outWidth

        var inSampleSize = 1

        while ((height / inSampleSize) * (width / inSampleSize) * 2 > 300 * 1024) {
            inSampleSize *= 2
        }

        return inSampleSize
    }

    fun showDilag(context: Context, takePhoto: () -> Unit, choosePic: () -> Unit) {
        if (choosePicWayDialog == null) {
            choosePicWayDialog = ChoosePicWayDialog(context, chooser1 = { _, dialog ->
                takePhoto()
                dialog.dismiss()
            }, chooser2 = { _, dialog ->
                choosePic()
                dialog.dismiss()
            }, cancelBlock = { _, dialog ->
                dialog.dismiss()
            })
        }
        choosePicWayDialog?.show()

    }
}