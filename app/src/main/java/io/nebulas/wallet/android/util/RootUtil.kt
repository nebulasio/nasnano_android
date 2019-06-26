package io.nebulas.wallet.android.util

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class RootUtil {
    companion object {
        val instance : RootUtil by lazy { RootUtil() }
    }

    fun isDeviceRooted(): Boolean {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
    }

    private fun checkRootMethod1(): Boolean {
        val buildTags = android.os.Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootMethod2(): Boolean {
        val paths = arrayListOf("/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su")
        paths.forEach {
            if (File(it).exists()) return true
        }

        return false
    }

    private fun checkRootMethod3(): Boolean {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            var input = BufferedReader(InputStreamReader(process?.getInputStream()))
            if (input.readLine() != null) return true
            return false
        } catch (t: Throwable) {
            return false
        } finally {
            if (process != null) process.destroy()
        }
    }

}