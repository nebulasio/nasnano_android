package io.nebulas.wallet.android.util

import android.graphics.Bitmap
import android.graphics.Canvas
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream




/**
 * 二维码生成工具类
 *
 * Created by Heinoc on 2018/3/14.
 */
object QRCodeUtil {
    /**
     * 生成二维码Bitmap
     *
     * @param content   内容
     * @param widthPix  图片宽度
     * @param heightPix 图片高度
     * @param logoBm    二维码中心的Logo图标（可以为null）
     * @param filePath  用于存储二维码图片的文件路径
     * @return 生成二维码及保存文件是否成功
     */
    fun createQRImage(content: String?, widthPix: Int, heightPix: Int, logoBm: Bitmap?, filePath: String, deleteWhite:Boolean = false): Boolean {
        try {
            if (content == null || "" == content) {
                return false
            }

            val file = File(filePath)
            if (file.exists() && file.length() > 0) { //文件已经存在，不进行重复创建
                return true
            }

            //配置参数
            val hints = HashMap<EncodeHintType, Any>()
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8")
            //容错级别
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            //设置空白边距的宽度
            hints.put(EncodeHintType.MARGIN, 1) //default is 4

            // 图像数据转换，使用了矩阵转换
            var bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, widthPix, heightPix, hints)
            if (deleteWhite) {
                bitMatrix = deleteWhite(bitMatrix)
            }
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            // 下面这里按照二维码的算法，逐个生成二维码的图片，
            // 两个for循环是图片横列扫描的结果
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * width + x] = -0x1000000
                    } else {
                        pixels[y * width + x] = -0x1
                    }
                }
            }

            // 生成二维码图片的格式，使用ARGB_8888
            var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap!!.setPixels(pixels, 0, width, 0, 0, width, height)

            if (logoBm != null) {
                bitmap = addLogo(bitmap, logoBm)
            }

            //必须使用compress方法将bitmap保存到文件中再进行读取。直接返回的bitmap是没有任何压缩的，内存消耗巨大！
            return bitmap != null && bitmap.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream(filePath))
        } catch (e: Exception) {
            e.printStackTrace()

            FileTools().deleteFile(filePath)

        }

        return false
    }

    /**
     * 在二维码中间添加Logo图案
     */
    private fun addLogo(src: Bitmap?, logo: Bitmap?): Bitmap? {
        if (src == null) {
            return null
        }

        if (logo == null) {
            return src
        }

        //获取图片的宽高
        val srcWidth = src.width
        val srcHeight = src.width
        val logoWidth = logo.width
        val logoHeight = logo.width

        if (srcWidth == 0 || srcHeight == 0) {
            return null
        }

        if (logoWidth == 0 || logoHeight == 0) {
            return src
        }

        //logo大小为二维码整体大小的1/5
        val scaleFactor = srcWidth * 1.0f / 5f / logoWidth.toFloat()
        var bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawBitmap(src, 0f, 0f, null)
            canvas.scale(scaleFactor, scaleFactor, srcWidth / 2f, srcHeight / 2f)
            canvas.drawBitmap(logo, (srcWidth - logoWidth) / 2f, (srcHeight - logoHeight) / 2f, null)

            canvas.save(Canvas.ALL_SAVE_FLAG)
            canvas.restore()
        } catch (e: Exception) {
            bitmap = null
            e.stackTrace
        }

        return bitmap
    }


    private fun deleteWhite(matrix: BitMatrix): BitMatrix {
        val rec = matrix.enclosingRectangle
        val resWidth = rec[2] + 1
        val resHeight = rec[3] + 1

        val resMatrix = BitMatrix(resWidth, resHeight)
        resMatrix.clear()
        for (i in 0 until resWidth) {
            for (j in 0 until resHeight) {
                if (matrix.get(i + rec[0], j + rec[1]))
                    resMatrix.set(i, j)
            }
        }
        return resMatrix
    }

}