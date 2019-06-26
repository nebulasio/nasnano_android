package io.nebulas.wallet.android.util

import android.content.Context
import android.text.format.DateFormat
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.common.Constants
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Heinoc on 2018/2/9.
 */

object Formatter {
    fun isEmpty(str: String?): Boolean {
        return str == null || str.isEmpty()
    }

    fun sp2px(context: Context, spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }

    /**
     * 将dip或dp值转换为px值，保证尺寸大小不变
     *
     * @param dipValue
     * @return
     */
    fun dip2px(context: Context, dipValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dipValue * scale + 0.5f).toInt()
    }

    /**
     * 将px值转换为dp值，保证尺寸大小不变
     *
     * @param dipValue
     * @return
     */
    fun px2dip(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return ((pxValue - 0.5) / scale).toInt()
    }

    fun formatImageUrl(url: String?): String {

        if (url == null) return ""
        var str = url + "?imageView2/0/w/500"
        return url
    }

    /**
     * 16进制数字string转10进制string
     */
    fun hexToString(hexStr: String): String {
        var hexString = hexStr.trim().replace("0x", "")
        hexString = hexString.replace("0X", "")
        var bigInt = BigInteger(hexString, 16)

        return bigInt.toString(10)
    }

    /**
     * 10进制数字string转16进制string
     */
    fun stringToHex(str: String): String {
        var preHexString = str.trim().replace("0x", "")
        preHexString = preHexString.replace("0X", "")
        var bigInt = BigInteger(preHexString, 10)

        return bigInt.toString(16)
    }

    fun ethHexToBalance(hexStr: String, tokenDecimal: Int = 18): String {
        var hexString = hexStr.trim().replace("0x", "")
        hexString = hexString.replace("0X", "")
        if (hexString.isBlank()) {
            hexString = "0"
        }
        var bigInt = BigInteger(hexString, 16)

        val result = BigDecimal(bigInt).divide(BigDecimal(10).pow(tokenDecimal), Constants.TOKEN_SCALE, RoundingMode.DOWN)

        return result.stripTrailingZeros().toPlainString()

    }

    fun nasBalanceFormat(balance: String, tokenDecimal: Int = 18): String {
        val result = BigDecimal(balance.trim()).divide(BigDecimal(10).pow(tokenDecimal), Constants.TOKEN_SCALE, RoundingMode.DOWN)
        return result.stripTrailingZeros().toPlainString()
    }

    fun amountFormat(amount: String, tokenDecimal: Int): String {
        if (amount.isEmpty() || "null" == amount)
            return "0"
        val result = BigDecimal(amount.trim()).divide(BigDecimal(10).pow(tokenDecimal), Constants.TOKEN_SCALE, RoundingMode.DOWN)
        return result.stripTrailingZeros().toPlainString()
    }

    /**
     * 根据gasPrice和gasUse获取gas手续费
     */
    fun getGas(gasPrice: String, gasUse: String, tokenDecimal: Int): String {
        var gasPriceBD = BigDecimal(gasPrice.trim())
        var gasUseBD = BigDecimal(gasUse.trim())

        return gasPriceBD.multiply(gasUseBD).divide(BigDecimal(10).pow(tokenDecimal)).stripTrailingZeros().toPlainString()

    }

    /**
     * format the errorMsg from the wallet-core SDK
     */
    fun formatWalletErrorMsg(context: Context, errMsg: String): String {
        return if (errMsg.contains("invalid address")) {
            return context.getString(R.string.wrong_address_content)
        } else if (errMsg.contains("unexpected end of json input")) {
            return context.getString(R.string.wrong_keyjson)
        } else if (errMsg.contains("could not decrypt key with given passphrase")) {
            return context.getString(R.string.wrong_pwd)
        } else if (errMsg.contains("privateKey convert to big.int failed")) {
            return context.getString(R.string.cannot_get_privatekey)
        } else if (errMsg.contains("invalid character") || errMsg.contains("unexpected end of JSON input") || errMsg.contains("version not supported")) {
            return context.getString(R.string.wrong_keystore_content)
        } else if (errMsg.contains("Invalid mnemonic") || errMsg.contains("Invalid byte at position") || errMsg.contains("Entropy length must be") || errMsg.contains("Wrong entropy + checksum size")) {
            return context.getString(R.string.wrong_mnemonic_content)
        } else errMsg
    }

    fun tokenFormat(data: BigDecimal, formatter: String = Constants.TOKEN_FORMAT): String {
        val format = DecimalFormat(formatter)
        val dfs = DecimalFormatSymbols()
        dfs.decimalSeparator = '.'
        dfs.monetaryDecimalSeparator = '.'
        dfs.groupingSeparator = ','
        format.decimalFormatSymbols = dfs
        return format.format(data)
    }

    fun priceFormat(data: BigDecimal): String {
        val priceFormat = DecimalFormat(Constants.PRICE_FORMAT)
        val dfs = DecimalFormatSymbols()
        dfs.decimalSeparator = '.'
        dfs.monetaryDecimalSeparator = '.'
        dfs.groupingSeparator = ','
        priceFormat.decimalFormatSymbols = dfs
        return priceFormat.format(data.setScale(2, RoundingMode.DOWN))
    }

    fun gasPriceFormat(data: BigDecimal): String {
        val priceFormat = DecimalFormat(Constants.GAS_PRICE_FORMAT)
        val dfs = DecimalFormatSymbols()
        dfs.decimalSeparator = '.'
        dfs.monetaryDecimalSeparator = '.'
        dfs.groupingSeparator = ','
        priceFormat.decimalFormatSymbols = dfs
        return priceFormat.format(data)
    }

    /**
     * 获取timeFormat后的字符串
     *
     * @param skeleton 默认需要的时间字段, 例：yyyyMMMddkkmmss
     * @param date 时间
     * @param showGMT 是否显示GMT时区
     * @return
     */
    fun timeFormat(skeleton: String, date: Date, showGMT: Boolean = false): String {
        return timeFormat(skeleton, date.time, showGMT)
    }

    /**
     * 获取timeFormat后的字符串
     *
     * @param skeleton 默认需要的时间字段, 例：yyyyMMMddkkmmss
     * @param timestamp 时间戳
     * @param showGMT 是否显示GMT时区
     * @return
     */
    fun timeFormat(skeleton: String, timestamp: Long, showGMT: Boolean = false): String {
        val pattern: String
        val sdf: SimpleDateFormat
        if ("cn" == Util.getCurLanguage()) {
            pattern = DateFormat.getBestDateTimePattern(Locale.CHINA, skeleton)
            sdf = SimpleDateFormat(pattern, Locale.CHINA)
        } else {
            pattern = DateFormat.getBestDateTimePattern(Locale.ENGLISH, skeleton)
            sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
        }

        return if (showGMT) {
            var timeZone = BigDecimal(TimeZone.getDefault().rawOffset)
            val symbol = if (timeZone >= BigDecimal(0)) {
                "+"
            } else {
                timeZone = BigDecimal(Math.abs(TimeZone.getDefault().rawOffset) - 3600000)
                "-"
            }
            "${sdf.format(timestamp)} GMT$symbol${timeZone.divide(BigDecimal(3600000)).stripTrailingZeros().toPlainString()}"
        } else {
            sdf.format(timestamp)
        }


    }

}