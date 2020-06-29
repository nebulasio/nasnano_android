package io.nebulas.wallet.android.util

import android.app.Activity
import android.app.ActivityManager
import android.app.LauncherActivity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.util.DisplayMetrics
import android.view.WindowManager
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.PreferenceConstants
import io.nebulas.wallet.android.common.URLConstants
import io.nebulas.wallet.android.dialog.WebViewBottomDialog
import io.nebulas.wallet.android.extensions.Preference
import io.nebulas.wallet.android.module.main.MainActivity
import io.nebulas.wallet.android.BuildConfig
import walletcore.Walletcore
import java.util.*
import java.util.regex.Pattern


/**
 * Created by Heinoc on 2018/3/2.
 */
object Util {


    /**
     * 当前设备屏幕的宽(像素)
     *
     * @param context
     * @return
     */
    fun screenWidth(context: Context): Int {
        val manager = context
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        manager.defaultDisplay.getMetrics(dm)
        return dm.widthPixels
    }

    /**
     * 当前设备屏幕的高(像素)
     *
     * @param context
     * @return
     */
    fun screenHeight(context: Context): Int {
        val manager = context
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        manager.defaultDisplay.getMetrics(dm)
        return dm.heightPixels
    }

    /**
     * 当前设备屏幕的密度
     *
     * @param context
     * @return
     */
    fun density(context: Context): Float {
        // return context.getResources().getDisplayMetrics().densityDpi;
        val metrics = DisplayMetrics()
        val windowManager = context
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(metrics)
        return metrics.density
    }


    /**
     * Gets the versionName of the application.
     *
     * @param context [Context]. Cannot be null.
     * @return The application's version
     */
    fun appVersion(context: Context): String {
//        val pm = context.packageManager
//
//        try {
//            /*
//                * If there is no versionName in the Android Manifest, the
//                * versionName will be null.
//                */
//            return pm.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES).versionName
//                    ?: return "unknown"
//        } catch (e: PackageManager.NameNotFoundException) {
//            /*
//			 * This should never occur--our own package must exist for this code
//			 * to be running
//			 */
//            throw RuntimeException(e)
//        }
        return BuildConfig.VERSION_NAME

    }

    fun appVersionCode(context: Context): Int {
//        val pm = context.packageManager
//
//        try {
//            /*
//                * If there is no versionName in the Android Manifest, the
//                * versionName will be null.
//                */
//            return pm.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES).versionCode
//        } catch (e: PackageManager.NameNotFoundException) {
//            /*
//			 * This should never occur--our own package must exist for this code
//			 * to be running
//			 */
//            throw RuntimeException(e)
//        }
        return BuildConfig.VERSION_CODE

    }

    /**
     * 校验地址格式是否正确
     */
    fun checkAddress(address: String, platform: String): Boolean {
        return when (platform) {
            Walletcore.NAS -> {
                val reg = "[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{35}"
                Pattern.compile(reg).matcher(address).matches() && address.startsWith("n")
            }
            Walletcore.ETH -> {
                val addr = if (address.startsWith("0x", true)) {
                    address.substring(2)
                } else {
                    address
                }
                val reg = "[0123456789ABCDEFabcdef]{40}"
                Pattern.compile(reg).matcher(addr).matches()
            }
            else -> {
                false
            }
        }
    }

    /**
     * 当改变系统语言时,重启App
     *
     * @param activity
     * @param homeActivityCls 主activity
     * @return
     */
    fun isLanguageChanged(activity: Activity, homeActivityCls: Class<*>): Boolean {
        val locale = Locale.getDefault() ?: return false

        var localeSystemStr: String by Preference<String>(activity, PreferenceConstants.PREFERENCE_SYSTEM_LANGUAGE, "")

        val curLocaleStr = getLocaleString(locale)

        if (localeSystemStr.isEmpty()) {
            localeSystemStr = curLocaleStr

            return false
        } else {
            if (localeSystemStr == curLocaleStr) {
                return false
            } else {
                localeSystemStr = curLocaleStr

                /**
                 * 同步应用语言配置
                 */
                when (curLocaleStr) {
                    "zh" -> {
                        if (getAppLanguage(activity) != "cn") {
//                            setAppLanguage(activity, "cn")

                            if (activity is LauncherActivity) {
                            } else
                                restartApp(activity, homeActivityCls)
                        }
                    }
                    "en" -> {
                        if (getAppLanguage(activity) != "en") {
//                            setAppLanguage(activity, "en")

                            if (activity is LauncherActivity) {
                            } else
                                restartApp(activity, homeActivityCls)
                        }
                    }
                    "kr" -> {
                        if (getAppLanguage(activity) != "kr") {
//                            setAppLanguage(activity, "en")

                            if (activity is LauncherActivity) {
                            } else
                                restartApp(activity, homeActivityCls)
                        }
                    }
                    //默认英语
                    else -> {
                        if (getAppLanguage(activity) != "en") {
//                            setAppLanguage(activity, "en")

                            if (activity is LauncherActivity) {
                            } else
                                restartApp(activity, homeActivityCls)
                        }
                    }
                }


                return true
            }
        }
    }


    private fun getLocaleString(locale: Locale?): String {
        return if (locale == null) {
            ""
        } else {
            locale!!.getLanguage()
        }
    }

    fun restartApp(activity: Activity, homeClass: Class<*>) {
//        doAsync {
//            Thread.sleep(200)
//            uiThread {
//                val intent = Intent(activity, homeClass)
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                activity.startActivity(intent)
//                // 杀掉进程
//                android.os.Process.killProcess(android.os.Process.myPid())
//                System.exit(0)
//            }
//        }
        MainActivity.launch(activity)
    }

    fun getAppCurrencySymbol(context: Context): String {
        var currencySymbolStr: String by Preference<String>(context, PreferenceConstants.PREFERENCE_CURRENCY_SYMBOL, "")
        if (currencySymbolStr.isEmpty())
            currencySymbolStr = "usd"

        when (currencySymbolStr) {
            context.getString(R.string.currency_symbol_name_cny).toLowerCase() -> {
                Constants.CURRENCY_SYMBOL = context.getString(R.string.currency_symbol_cny)
            }
            context.getString(R.string.currency_symbol_name_usd).toLowerCase() -> {
                Constants.CURRENCY_SYMBOL = context.getString(R.string.currency_symbol_usd)
            }
        }
        return currencySymbolStr

    }

    fun setAppCurrencySymbol(context: Context, currencySymbol: String) {
        Constants.CURRENCY_SYMBOL_NAME = currencySymbol
        when (currencySymbol) {
            context.getString(R.string.currency_symbol_name_cny).toLowerCase() -> {
                Constants.CURRENCY_SYMBOL = context.getString(R.string.currency_symbol_cny)
            }
            context.getString(R.string.currency_symbol_name_usd).toLowerCase() -> {
                Constants.CURRENCY_SYMBOL = context.getString(R.string.currency_symbol_usd)
            }
        }
        var currencySymbolStr: String by Preference<String>(context, PreferenceConstants.PREFERENCE_CURRENCY_SYMBOL, "")
        currencySymbolStr = currencySymbol

    }

    fun getAppLanguage(context: Context): String {
        var appLanguageStr: String by Preference<String>(context, PreferenceConstants.PREFERENCE_APP_LANGUAGE, "")

        if (appLanguageStr.isEmpty()) {

//            val defaultLocale = Locale.getDefault()
//            appLanguageStr = if (defaultLocale.country == "CN") {
//                "cn"
//            } else {
//                "en"
//            }
            appLanguageStr = "auto"
        }

        Constants.LANGUAGE = appLanguageStr

        return appLanguageStr

    }

    fun setAppLanguage(context: Context, language: String) {
        Constants.LANGUAGE = language

        var appLanguageStr: String by Preference<String>(context, PreferenceConstants.PREFERENCE_APP_LANGUAGE, "")

        /**
         * ugly miui!!!,when change the app's language,the function "Locale.getDefault()" will return the app's language,not the system'slanguage.
         * so,when change the app's language,we also need to async the "Locale.getDefault()" language to shared preference
         */
        val locale = Locale.getDefault()
        val curLocaleStr = getLocaleString(locale)
        var localeSystemStr: String by Preference<String>(context, PreferenceConstants.PREFERENCE_SYSTEM_LANGUAGE, "")
        localeSystemStr = curLocaleStr

        appLanguageStr = language

    }

    fun getCurLanguage(): String {
        return if (Constants.LANGUAGE == "auto") {
            val defaultLocale = Locale.getDefault()
            val country = defaultLocale.country
            when (country) {
                "CN" -> "cn"
                "KR" -> "kr" // 韩国-KR，朝鲜-KP
                else -> "en"
            }
        } else {
            Constants.LANGUAGE
        }
    }

    fun getDefaultReceivingWallet(context: Context): Long {
        var receivingWallet: Long by Preference<Long>(context, PreferenceConstants.PREFERENCE_DEFAULT_RECEIVING_WALLET, -1)
        return receivingWallet
    }

    fun getDefaultPaymentWallet(context: Context): Long {
        var paymentWalletId: Long by Preference<Long>(context, PreferenceConstants.PREFERENCE_DEFAULT_PAMENT_WALLET, -1)
        return paymentWalletId
    }

    /**
     * 默认支付钱包
     * @param id  钱包id
     */
    fun setDefaultPaymentWallet(context: Context, id: Long) {
        var paymentWalletId: Long by Preference<Long>(context, PreferenceConstants.PREFERENCE_DEFAULT_PAMENT_WALLET, -1)
        paymentWalletId = id
    }

    /**
     * 默认收币钱包
     * @param id  钱包id
     */
    fun setDefaultReceivingWallet(context: Context, id: Long) {
        var receivingWalletId: Long by Preference<Long>(context, PreferenceConstants.PREFERENCE_DEFAULT_RECEIVING_WALLET, -1)
        receivingWalletId = id
    }

    /**
     * 是否隐藏金额
     */
    fun setBalanceHidden(context: Context, isHidden: Boolean) {
        var isHiddenPreference: Boolean by Preference<Boolean>(context, PreferenceConstants.PREFERENCE_BALANCE_HIDDEN, false)
        isHiddenPreference = isHidden
    }

    /**
     * 是否隐藏金额
     */
    fun getBalanceHidden(context: Context): Boolean {
        var isHiddenPreference: Boolean by Preference<Boolean>(context, PreferenceConstants.PREFERENCE_BALANCE_HIDDEN, false)
        return isHiddenPreference
    }


    /**
     * 获取UUID
     */
    fun getUUID(context: Context): String {
        var uuidStr: String by Preference<String>(context, PreferenceConstants.PREFERENCE_UUID, "")

        if (uuidStr.isEmpty())
            uuidStr = UUID.randomUUID().toString()

        return uuidStr

    }

    /**
     * 协议版本
     */
    private fun setPRIVACY_TERMS_VERSION(context: Context, privacyTermsVersion: String) {
        var privacyTermsVersionSP: String by Preference<String>(context, PreferenceConstants.PRIVACY_TERMS_VERSION, "")
        privacyTermsVersionSP = privacyTermsVersion
    }

    private fun getPRIVACY_TERMS_VERSION(context: Context): String {
        val privacyTermsVersionSP: String by Preference<String>(context, PreferenceConstants.PRIVACY_TERMS_VERSION, "")
        return privacyTermsVersionSP
    }

    /**
     * 展示隐私协议
     */
    private fun showProtocol(context: Context, onCancle: () -> Unit, onConfirm: () -> Unit) {
        WebViewBottomDialog(context, title = context.getString(R.string.user_pri_service), content = URLConstants.PRIVACY_TERMS_URL, cancelButtonText = context.getString(R.string.exit), cancelBlock = { _, dialog ->
            onCancle()
            dialog.dismiss()
        }, confirmButtonText = context.getString(R.string.next), confirmBlock = { _, dialog ->
            onConfirm()
            dialog.dismiss()
        }, canceledOnTouchOutsideEnable = false).show()
    }

    /**
     * 检查是否是展示隐私协议和服务协议
     */
    fun checkProtocol(context: Context) {
        if (Util.getPRIVACY_TERMS_VERSION(context) != Constants.PRIVACY_TERMS_VERSION) {
            Util.showProtocol(context, {
                Process.killProcess(Process.myPid())
                System.exit(0)
            }, {
                Util.setPRIVACY_TERMS_VERSION(context, Constants.PRIVACY_TERMS_VERSION)
            })
        }
    }

    /**
     * 记录需要展示的tokens
     */
    public fun setNeddShowTokens(context: Context, needShowTokens: String) {
        var needShowTokensSP: String by Preference<String>(context, PreferenceConstants.PREFERENCE_NEED_SHOW_TOKENS, "")
        needShowTokensSP = needShowTokens
    }

    public fun getNeedShowTokens(context: Context): String {
        val needShowTokensSP: String by Preference<String>(context, PreferenceConstants.PREFERENCE_NEED_SHOW_TOKENS, "")
        return needShowTokensSP
    }

    fun isRunForeground(activity: Activity): Boolean {
        if (activity == null)
            return false

        val am: ActivityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcessInfoList = am.runningAppProcesses
        if (runningAppProcessInfoList == null || runningAppProcessInfoList.isEmpty()) return false
        runningAppProcessInfoList.forEach {
            if (it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && it.processName == activity.applicationInfo.processName)
                return true
        }
        return false
    }
}