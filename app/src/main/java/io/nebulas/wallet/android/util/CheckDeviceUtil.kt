package io.nebulas.wallet.android.util

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import java.io.File
import java.io.FileInputStream


class CheckDeviceUtil {
    companion object {
        val instance: CheckDeviceUtil by lazy { CheckDeviceUtil() }
    }

    private val known_pipes = arrayOf("/dev/socket/qemud", "/dev/qemu_pipe")
    private val known_qemu_drivers = arrayOf("goldfish")

    private val known_files = arrayOf("/system/lib/libc_malloc_debug_qemu.so", "/sys/qemu_trace")//"/system/bin/qemu-props"(这个文件在三星真机Galaxy C9 Pro - SM-C9000上被检测到了)

    private val known_numbers = arrayOf("15555215554", "15555215556", "15555215558", "15555215560", "15555215562", "15555215564", "15555215566", "15555215568", "15555215570", "15555215572", "15555215574", "15555215576", "15555215578", "15555215580", "15555215582", "15555215584")

    private val known_device_ids = arrayOf("000000000000000") // 默认ID

    private val known_imsi_ids = arrayOf("310260000000000") // 默认的 imsi id

    //检测“/dev/socket/qemud”，“/dev/qemu_pipe”这两个通道
    fun checkPipes(): Boolean {
        for (i in 0 until known_pipes.size) {
            val pipes = known_pipes[i]
            val qemu_socket = File(pipes)
            if (qemu_socket.exists()) {
                Log.v("Result:", "Find pipes!")
                return true
            }
        }
        Log.i("Result:", "Not Find pipes!")
        return false
    }

    // 检测驱动文件内容
    // 读取文件内容，然后检查已知QEmu的驱动程序的列表
    fun checkQEmuDriverFile(): Boolean {
        val driver_file = File("/proc/tty/drivers")
        if (driver_file.exists() && driver_file.canRead()) {
            val data = ByteArray(1024)  //(int)driver_file.length()
            try {
                val inStream = FileInputStream(driver_file)
                inStream.read(data)
                inStream.close()
            } catch (e: Exception) {
                // TODO: handle exception
                e.printStackTrace()
            }

            val driver_data = String(data)
            for (known_qemu_driver in known_qemu_drivers) {
                if (driver_data.indexOf(known_qemu_driver) != -1) {
                    Log.i("Result:", "Find know_qemu_drivers!")
                    return true
                }
            }
        }
        Log.i("Result:", "Not Find known_qemu_drivers!")
        return false
    }

    //检测模拟器上特有的几个文件
    fun checkEmulatorFiles(): Boolean {
        for (i in known_files.indices) {
            val file_name = known_files[i]
            val qemu_file = File(file_name)
            if (qemu_file.exists()) {
                Log.v("Result:", "Find Emulator Files!")
                return true
            }
        }
        Log.v("Result:", "Not Find Emulator Files!")
        return false
    }

    @SuppressLint("MissingPermission", "HardwareIds")
// 检测模拟器默认的电话号码
    fun checkPhoneNumber(context: Context): Boolean {
        try {
            val telephonyManager = context
                    .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val phonenumber = telephonyManager.line1Number

            for (number in known_numbers) {
                if (number.equals(phonenumber, ignoreCase = true)) {
                    Log.v("Result:", "Find PhoneNumber!")
                    return true
                }
            }
        }catch (e:Exception){
            return false
        }
        Log.v("Result:", "Not Find PhoneNumber!")
        return false
    }

    @SuppressLint("MissingPermission")
//检测设备IDS 是不是 “000000000000000”
    fun checkDeviceIDS(context: Context): Boolean {
        try {
            val telephonyManager = context
                    .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val device_ids = telephonyManager.deviceId

            for (know_deviceid in known_device_ids) {
                if (know_deviceid.equals(device_ids, ignoreCase = true)) {
                    Log.v("Result:", "Find ids: 000000000000000!")
                    return true
                }
            }
        }catch (e:Exception){
            return false
        }
        Log.v("Result:", "Not Find ids: 000000000000000!")
        return false
    }

    @SuppressLint("MissingPermission")
// 检测imsi id是不是“310260000000000”
    fun checkImsiIDS(context: Context): Boolean {
        try {

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val imsi_ids = telephonyManager.subscriberId

        for (know_imsi in known_imsi_ids) {
            if (know_imsi.equals(imsi_ids, ignoreCase = true)) {
                Log.v("Result:", "Find imsi ids: 310260000000000!")
                return true
            }
        }

        }catch (e:Exception){
            return false
        }
        Log.v("Result:", "Not Find imsi ids: 310260000000000!")
        return false
    }

    //检测手机上的一些硬件信息
    fun checkEmulatorBuild(context: Context): Boolean {
        val BOARD = android.os.Build.BOARD
        val BOOTLOADER = android.os.Build.BOOTLOADER
        val BRAND = android.os.Build.BRAND
        val DEVICE = android.os.Build.DEVICE
        val HARDWARE = android.os.Build.HARDWARE
        val MODEL = android.os.Build.MODEL
        val PRODUCT = android.os.Build.PRODUCT
        if (BOARD === "unknown" || BOOTLOADER === "unknown"
                || BRAND === "generic" || DEVICE === "generic"
                || MODEL === "sdk" || PRODUCT === "sdk"
                || HARDWARE === "goldfish") {
            Log.v("Result:", "Find Emulator by EmulatorBuild!")
            return true
        }
        Log.v("Result:", "Not Find Emulator by EmulatorBuild!")
        return false
    }

    //检测手机运营商家
    fun checkOperatorNameAndroid(context: Context): Boolean {
        val szOperatorName = (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).networkOperatorName

        if (szOperatorName.toLowerCase() == "android" == true) {
            Log.v("Result:", "Find Emulator by OperatorName!")
            return true
        }
        Log.v("Result:", "Not Find Emulator by OperatorName!")
        return false
    }
}
