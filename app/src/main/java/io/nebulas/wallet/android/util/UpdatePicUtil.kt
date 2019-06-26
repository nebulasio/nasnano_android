package io.nebulas.wallet.android.util

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

class UpdatePicUtil {
    val instances: UpdatePicUtil by lazy { UpdatePicUtil() }

    /**
     * 将Bitmap转换成Base64字符串
     * @param bit
     * @return
     */
    fun Bitmap2StrByBase64(bit: Bitmap): String {
        val bos: ByteArrayOutputStream = ByteArrayOutputStream()
        bit.compress(Bitmap.CompressFormat.JPEG, 100, bos)
        val bytes = bos.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }


}