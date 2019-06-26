package io.nebulas.wallet.android.network.cache

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.common.Constants
import java.io.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.experimental.and

/**
 * Created by Heinoc on 2018/7/25.
 */
class CacheManager private constructor() {

    companion object {

        val TAG = "CacheManager"

        //max cache size 10mb
        private val DISK_CACHE_SIZE = (10 * 1024 * 1024).toLong()

        private val DISK_CACHE_INDEX = 0

        @Volatile
        private var mCacheManager: CacheManager? = null

        val instance: CacheManager
            get() {
                if (mCacheManager == null) {
                    synchronized(CacheManager::class.java) {
                        if (mCacheManager == null) {
                            mCacheManager = CacheManager()
                        }
                    }
                }
                return mCacheManager!!
            }

        /**
         * 对字符串进行MD5编码
         */
        fun encryptMD5(string: String): String {
            try {
                val hash = MessageDigest.getInstance("MD5").digest(
                        string.toByteArray(charset("UTF-8")))
                val hex = StringBuilder(hash.size * 2)
                for (b in hash) {
                    if ((b and 0xFF.toByte()) < 0x10) {
                        hex.append("0")
                    }
                    hex.append(Integer.toHexString((b and 0xFF.toByte()).toInt()))
                }
                return hex.toString()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            return string
        }
    }

    private var mDiskLruCache: DiskLruCache? = null

    init {
        val diskCacheDir = mkCacheDir()

        if (diskCacheDir.usableSpace > DISK_CACHE_SIZE) {
            try {
                mDiskLruCache = DiskLruCache.open(diskCacheDir,
                        getAppVersion(WalletApplication.INSTANCE), 1/*一个key对应多少个文件*/, DISK_CACHE_SIZE)
                Log.d(TAG, "mDiskLruCache created")
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    private fun mkCacheDir() : File{
        val diskCacheDir = getDiskCacheDir(WalletApplication.INSTANCE, Constants.NETWORK_CACHE_DIR)
        if (!diskCacheDir.exists()) {
            val b = diskCacheDir.mkdirs()
            Log.d(TAG, "!diskCacheDir.exists() --- diskCacheDir.mkdirs()=$b")
        }
        return diskCacheDir
    }
    /**
     * 同步设置缓存
     */
    fun putCache(key: String, value: String) {
        if (mDiskLruCache == null) return
        //设置中应用程序清除缓存，会导致缓存路径被清掉，在此创建
        mkCacheDir()
        var os: OutputStream? = null
        try {
            val editor = mDiskLruCache!!.edit(encryptMD5(key))
            os = editor.newOutputStream(DISK_CACHE_INDEX)
            os.write(value.toByteArray())
            os.flush()
            editor.commit()
            mDiskLruCache!!.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (os != null) {
                try {
                    os.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
    }

    /**
     * 异步设置缓存
     */
    fun setCache(key: String, value: String) {
        object : Thread() {
            override fun run() {
                putCache(key, value)
            }
        }.start()
    }

    /**
     * 同步获取缓存
     */
    fun getCache(key: String): String? {
        if (mDiskLruCache == null) {
            return null
        }
        var fis: FileInputStream? = null
        var bos: ByteArrayOutputStream? = null
        try {
            val snapshot = mDiskLruCache!!.get(encryptMD5(key))
            if (snapshot != null) {
                fis = snapshot.getInputStream(DISK_CACHE_INDEX) as FileInputStream
                bos = ByteArrayOutputStream()
                val buf = ByteArray(1024)
                var len = fis.read(buf)
                while (len != -1) {
                    bos.write(buf, 0, len)
                    len = fis.read(buf)
                }
                val data = bos.toByteArray()
                return String(data)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (fis != null) {
                try {
                    fis.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
            if (bos != null) {
                try {
                    bos.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
        return null
    }

    /**
     * 异步获取缓存
     */
    fun getCache(key: String, callback: CacheCallback) {
        object : Thread() {
            override fun run() {
                val cache = getCache(key)
                callback.onGetCache(cache ?: "")
            }
        }.start()
    }

    /**
     * 移除缓存
     */
    fun removeCache(key: String): Boolean {
        if (mDiskLruCache != null) {
            try {
                return mDiskLruCache!!.remove(encryptMD5(key))
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        return false
    }

    /**
     * 获取缓存目录
     */
    private fun getDiskCacheDir(context: Context, uniqueName: String): File {
        val cachePath = context.cacheDir.path
        return File(cachePath + File.separator + uniqueName)
    }

    /**
     * 获取APP版本号
     */
    private fun getAppVersion(context: Context): Int {
        val pm = context.packageManager
        try {
            val pi = pm.getPackageInfo(context.packageName, 0)
            return if (pi == null) 0 else pi.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        return 0
    }
}