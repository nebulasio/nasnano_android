package io.nebulas.wallet.android.util

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import java.io.*
import java.math.BigInteger
import java.security.MessageDigest
import java.text.DecimalFormat
import java.io.File

/**
 * 文件处理工具类
 *
 *
 * Created by heinoc on 14-6-17.
 */
class FileTools {
    private lateinit var context: Context

    constructor() {}

    constructor(context: Context) {
        this.context = context
    }

    /**
     * 创建文件目录
     */
    fun initFile() {
        val state = Environment.getExternalStorageState()

        // 检查存储设备是否存在
        if (Environment.MEDIA_MOUNTED != state && Environment.MEDIA_MOUNTED_READ_ONLY != state) {
            //SD卡不存在，修改文件存储目录，将文件目录结构创建在“/data/data/应用包名/files”目录下
        }

    }

    /**
     * 删除文件
     *
     * @param filePath
     */
    fun deleteFile(filePath: String) {
        val file = File(filePath)

        if (!file.exists()) {
            return
        }
        if (file.isDirectory) {
            return
        }

        file.delete()

    }

    /**
     * 删除整个文件夹（目录、文件、子目录）
     *
     * @param folderPath String 文件夹路径及名称 如c:/fqf
     * @return boolean
     */
    fun deleteDirectory(folderPath: String) {
        try {
            deleteAllFile(folderPath) // 删除完里面所有内容
            var filePath = folderPath
            filePath = filePath
            val myFilePath = File(filePath)
            myFilePath.delete() // 删除空文件夹

        } catch (e: Exception) {
            e.printStackTrace()

        }

    }

    /**
     * 删除文件夹里面的所有文件（包括目录）
     *
     * @param path String 文件夹路径
     */
    fun deleteAllFile(path: String) {
        val file = File(path)
        if (!file.exists()) {
            return
        }
        if (!file.isDirectory) {
            return
        }
        val tempList = file.list()
        var temp: File? = null
        for (i in tempList!!.indices) {
            if (path.endsWith(File.separator)) {
                temp = File(path + tempList[i])
            } else {
                temp = File(path + File.separator + tempList[i])
            }
            if (temp.isFile) {
                temp.delete()
            }
            if (temp.isDirectory) {
                deleteAllFile(path + "/" + tempList[i])// 先删除文件夹里面的文件
                deleteDirectory(path + "/" + tempList[i])// 再删除空文件夹
            }
        }
    }

    /**
     * 获取文件大小
     *
     * @param f
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun getFileSize(f: File): Long {
        var size: Long = 0
        val flist = f.listFiles()
        for (i in flist!!.indices) {
            if (flist[i].isDirectory) {
                size = size + getFileSize(flist[i])
            } else {
                size = size + flist[i].length()
            }
        }
        return size
    }

    /**
     * 转换文件大小为标准值
     *
     * @param fileS
     * @return
     */
    fun FormetFileSize(fileS: Long): String {//转换文件大小
        val df = DecimalFormat("#.00")
        var fileSizeString = ""
        if (fileS < 1024) {
            fileSizeString = df.format(fileS.toDouble()) + "B"
        } else if (fileS < 1048576) {
            fileSizeString = df.format(fileS.toDouble() / 1024) + "K"
        } else if (fileS < 1073741824) {
            fileSizeString = df.format(fileS.toDouble() / 1048576) + "M"
        } else {
            fileSizeString = df.format(fileS.toDouble() / 1073741824) + "G"
        }
        return fileSizeString
    }

    companion object {

        /**
         * 获取指定文件夹的缓存目录
         * @param context
         * @param uniqueName
         * @return
         */
        fun getDiskCacheDir(context: Context, uniqueName: String): File {
            val cachePath: String
            if ((Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) && context.externalCacheDir != null) {
                cachePath = context.externalCacheDir.path
            } else {
                cachePath = context.cacheDir.path
            }
            val file = File(cachePath + File.separator + uniqueName)
            if (!file.exists())
                file.mkdirs()
            return file
        }

        //判断sd卡是否存在
        //获取跟目录
        val sdPath: String?
            get() {
                var sdDir: File? = null
                val sdCardExist = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
                if (sdCardExist) {
                    sdDir = Environment.getExternalStorageDirectory()
                }
                if (sdDir == null) {
                    return null
                }
                return if (TextUtils.isEmpty(sdDir.toString())) null else sdDir.toString()

            }

        fun writeFileToSD(pathName: String, fileName: String, content: String) {
            var pathName = pathName

            val sdStatus = Environment.getExternalStorageState()
            if (sdStatus != Environment.MEDIA_MOUNTED) {
//                Log.d("TestFile", "SD card is not avaiable/writeable right now.")
                return
            }

            var sdcard = sdPath
            if (!sdcard!!.endsWith("/")) {
                sdcard = sdcard + "/"
            }

            pathName = sdcard + pathName

            try {
                if (!pathName.endsWith("/")) {
                    pathName = pathName + "/"
                }
                val path = File(pathName)
                val file = File(pathName + fileName)
                if (!path.exists()) {
//                    Log.d("TestFile", "Create the path:" + pathName)
                    path.mkdir()
                }
                if (!file.exists()) {
//                    Log.d("TestFile", "Create the file:" + fileName)
                    file.createNewFile()
                }
                //            String s = readFile(pathName+fileName);
                //            s = s+"\n"+content;
                val stream = FileOutputStream(file, true)
                val buf = content.toByteArray()
                stream.write(buf)
                stream.close()

            } catch (e: Exception) {
//                Log.e("TestFile", "Error on writeFilToSD.")
                e.printStackTrace()
            } catch (throwable: Throwable) {
//                Log.e("TestFile", "Error on read file.")
                throwable.printStackTrace()
            }

        }

        @Throws(Throwable::class)
        fun readFile(filename: String): String {
            val fis = FileInputStream(filename)
            val buf = ByteArray(1024)
            var len = 0
            val baos = ByteArrayOutputStream()
            //读取数据
            len = fis.read(buf)
            while (len != -1) {
                baos.write(buf, 0, len)

                len = fis.read(buf)
            }
            val data = baos.toByteArray()
            //关闭流
            baos.close()
            fis.close()
            return String(data)
        }

        /**
         * Indicates if this file represents a file on the underlying file system.
         *
         * @param filePath
         * @return
         */
        fun isFileExist(filePath: String): Boolean {
            if (StringUtils.isBlank(filePath)) {
                return false
            }

            val file = File(filePath)
            return file.exists() && file.isFile
        }

        /**
         * read file
         *
         * @param filePath
         * @param charsetName The name of a supported [&lt;/code&gt;charset&lt;code&gt;][java.nio.charset.Charset]
         * @return if file not exist, return null, else return content of file
         * @throws RuntimeException if an error occurs while operator BufferedReader
         */
        fun readFile(filePath: String, charsetName: String): StringBuilder? {
            val pathName: String
            var sdcard = sdPath
            if (TextUtils.isEmpty(sdcard)) {
                return null
            }
            if (!sdcard!!.endsWith("/")) {
                sdcard = sdcard + "/"
            }
            pathName = sdcard + filePath

            val file = File(pathName)
            val fileContent = StringBuilder("")
            if (file == null || !file.isFile) {
                return null
            }

            var reader: BufferedReader? = null
            try {
                val `is` = InputStreamReader(FileInputStream(file), charsetName)
                reader = BufferedReader(`is`)
                var line: String? = reader.readLine()
                while (line != null) {
                    if (fileContent.toString() != "") {
                        fileContent.append("\r\n")
                    }
                    fileContent.append(line)

                    line = reader.readLine()
                }
                reader.close()
                return fileContent
            } catch (e: IOException) {
                throw RuntimeException("IOException occurred. ", e)
            } finally {
                if (reader != null) {
                    try {
                        reader.close()
                    } catch (e: IOException) {
                        throw RuntimeException("IOException occurred. ", e)
                    }

                }
            }
        }

        /**
         * write file
         *
         * @param filePath
         * @param content
         * @param append   is append, if true, write to the end of file, else clear content of file and write into it
         * @return return false if content is empty, true otherwise
         * @throws RuntimeException if an error occurs while operator FileWriter
         */
        fun writeFile(filePath: String, content: String, append: Boolean): Boolean {
            if (StringUtils.isEmpty(content)) {
                return false
            }

            var fileWriter: FileWriter? = null
            try {
                makeDirs(filePath)
                fileWriter = FileWriter(filePath, append)
                fileWriter.write(content)
                fileWriter.close()
                return true
            } catch (e: IOException) {
                // throw new RuntimeException("IOException occurred. ", e);
                e.printStackTrace()
            } finally {
                if (fileWriter != null) {
                    try {
                        fileWriter.close()
                    } catch (e: IOException) {
                        // throw new RuntimeException("IOException occurred. ", e);
                        e.printStackTrace()
                    }

                }
            }
            return false
        }

        fun makeDirs(filePath: String): Boolean {
            val folderName = getFolderName(filePath)
            if (StringUtils.isEmpty(folderName)) {
                return false
            }

            val folder = File(folderName)
            return if (folder.exists() && folder.isDirectory) true else folder.mkdirs()
        }

        fun getFolderName(filePath: String): String {

            if (StringUtils.isEmpty(filePath)) {
                return filePath
            }

            val filePosi = filePath.lastIndexOf(File.separator)
            return if (filePosi == -1) "" else filePath.substring(0, filePosi)
        }

        /**
         * 返回文件列表
         *
         * @param filePath
         * @return
         */
        fun readFileToList(filePath: String): Array<String>? {
            var filePath = filePath
            var sdcard = sdPath
            if (TextUtils.isEmpty(sdcard)) {
                return null
            }
            if (!sdcard!!.endsWith("/")) {
                sdcard = sdcard + "/"
            }
            filePath = sdcard + filePath

            val file = File(filePath)
            return if (file == null || !file.isDirectory) {
                null
            } else file.list()
        }

        /**
         * 删除文件
         *
         * @param filePath
         * @return
         */
        fun delFile(filePath: String): Boolean {
            var filePath = filePath
            var sdcard = sdPath
            if (TextUtils.isEmpty(sdcard)) {
                return false
            }
            if (!sdcard!!.endsWith("/")) {
                sdcard = sdcard + "/"
            }
            filePath = sdcard + filePath

            val file = File(filePath)
            if (file == null || !file.isFile) {
                return false
            }
            file.delete()
            return true
        }

        /**
         *
         * 将文件转成base64 字符串
         *
         * @param path 文件路径
         * @return
         * @throws Exception
         */
        @Throws(Exception::class)
        fun encodeBase64File(path: String): String {
            val file = File(path)
            val inputFile = FileInputStream(file)
            val buffer = ByteArray(file.length().toInt())
            inputFile.read(buffer)
            inputFile.close()
            return Base64.encodeToString(buffer, Base64.DEFAULT)
        }

        /**
         * 专为Android4.4设计的从Uri获取文件绝对路径，以前的方法已不好使
         */
        fun getPath(context: Context, uri: Uri): String? {
            try {
                val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                // DocumentProvider
                if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                    // ExternalStorageProvider
                    if (isExternalStorageDocument(uri)) {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val type = split[0]

                        if ("25DA-14E4".equals(type, ignoreCase = true)) {
                            return (Environment.getExternalStorageDirectory().toString() + "/"
                                    + split[1])
                        } else if ("primary".equals(type, ignoreCase = true)) {
                            return (Environment.getExternalStorageDirectory().toString() + "/"
                                    + split[1])
                        }

                    } else if (isDownloadsDocument(uri)) {

                        val id = DocumentsContract.getDocumentId(uri)
                        val contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"),
                                java.lang.Long.valueOf(id))

                        return getDataColumn(context, contentUri, null, null)
                    } else if (isMediaDocument(uri)) {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val type = split[0]

                        var contentUri: Uri? = null
                        if ("image" == type) {
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        } else if ("video" == type) {
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        } else if ("audio" == type) {
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        }

                        val selection = "_id=?"
                        val selectionArgs = arrayOf(split[1])

                        return getDataColumn(context, contentUri, selection, selectionArgs)
                    }// MediaProvider
                    // DownloadsProvider
                } else if ("content".equals(uri.scheme, ignoreCase = true)) {
                    return getDataColumn(context, uri, null, null)
                } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                    return uri.path
                }// File
                // MediaStore (and general)

            }catch (e : Exception){
                return getFilePathFromURI(context, uri)
            }
            return null
        }

        fun getFileMD5(filePath: String): String? {
            return getFileMD5(File(filePath))
        }

        fun getFileMD5(file: File): String? {
            if (!file.isFile)
                return null
            val digest = MessageDigest.getInstance("MD5")
            val inputStream = file.inputStream()
            val buffer = ByteArray(1024)
            var length = 0
            try {
                while (length != -1) {
                    length = inputStream.read(buffer)
                    digest.update(buffer, 0, length)
                }
            } catch (e: Exception) {
                return null
            } finally {
                inputStream.close()
            }
            return BigInteger(1, digest.digest()).toString(16)
        }

        /**
         * Get the value of the data column for this Uri. This is useful for
         * MediaStore Uris, and other file-based ContentProviders.
         *
         * @param context       The context.
         * @param uri           The Uri to query.
         * @param selection     (Optional) Filter used in the query.
         * @param selectionArgs (Optional) Selection arguments used in the query.
         * @return The value of the _data column, which is typically a file path.
         */
        private fun getDataColumn(context: Context, uri: Uri?,
                                  selection: String?, selectionArgs: Array<String>?): String? {

            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(column)

            try {
                cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val column_index = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(column_index)
                }
            } finally {
                if (cursor != null)
                    cursor.close()
            }
            return null
        }

        private fun getFilePathFromURI(context: Context, contentUri: Uri?): String? {
            val fileName = getFileName(uri = contentUri)
            if (!TextUtils.isEmpty(fileName)) {
                val rootDataDir = context.filesDir.absolutePath
                var copyFile = File(rootDataDir.plus(File.separator) .plus(fileName))
                copy(context, contentUri!!, copyFile)
                return copyFile.absolutePath
            }
            return null
        }

        private fun getFileName(uri: Uri?): String? {
            if (uri == null) return null
            var fileName: String? = null
            val path = uri.path
            val cut = path.lastIndexOf('/')
            if (cut != -1) {
                fileName = path.substring(cut + 1)
            }
            return fileName
        }

        fun copy(context: Context, srcUri: Uri, dstFile: File) {
            try {
                val inputStream = context.contentResolver.openInputStream(srcUri) ?: return
                val outputStream = FileOutputStream(dstFile)
                IOUtils.copy(inputStream, outputStream)
                inputStream.close()
                outputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is ExternalStorageProvider.
         */
        private fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri
                    .authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is MediaProvider.
         */
        private fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri
                    .authority
        }

        private fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri
                    .authority
        }
    }


}
