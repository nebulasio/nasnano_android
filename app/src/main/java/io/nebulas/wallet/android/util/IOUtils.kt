package io.nebulas.wallet.android.util


import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object IOUtils {
    private val BUFFER_SIZE = 1024 * 2

    @Throws(Exception::class, IOException::class)
    fun copy(input: InputStream, output: OutputStream): Int {
        val buffer = ByteArray(BUFFER_SIZE)

        val input = BufferedInputStream(input, BUFFER_SIZE)
        val out = BufferedOutputStream(output, BUFFER_SIZE)
        var count = 0
        var n = 0
        try {
            while (n != -1) {
                n = input.read(buffer, 0, BUFFER_SIZE)
                if (n != -1) {
                    out.write(buffer, 0, n)
                    count += n
                }
            }
            out.flush()
        } finally {
            try {
                out.close()
            } catch (e: IOException) {
            }

            try {
                input.close()
            } catch (e: IOException) {
            }

        }
        return count
    }

}
