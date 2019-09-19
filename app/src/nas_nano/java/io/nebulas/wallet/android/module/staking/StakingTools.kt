package io.nebulas.wallet.android.module.staking

import android.text.TextUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

object StakingTools {

    fun naxFormat(str: String?): String {
        if (TextUtils.isEmpty(str)) {
            return "0"
        }
        return try {
            val decimal = BigDecimal(str).divide(BigDecimal.TEN.pow(9), 2, RoundingMode.FLOOR)
            val df = DecimalFormat("###,##0.00")
            df.format(decimal)
        } catch (e: Exception) {
            "0"
        }
    }

    fun calculatePledgeRate(pledged: String?, total: String?): String {
        if (TextUtils.isEmpty(pledged) || TextUtils.isEmpty(total)) {
            return "0.00%"
        }
        return try {
            val pledgedNas = BigDecimal(pledged)
            val totalNas = BigDecimal(total)
            val result = pledgedNas.multiply(BigDecimal("100")).divide(totalNas, 2, RoundingMode.FLOOR)
            "${result.toPlainString()}%"
        } catch (e: java.lang.Exception) {
            "0.00%"
        }
    }

    fun nasFormat(wei: String?, scale: Int = 4): BigDecimal {
        if (wei.isNullOrEmpty()) {
            return BigDecimal.ZERO
        }
        return try {
            val decimalWEI = BigDecimal(wei)
            decimalWEI.divide(BigDecimal.TEN.pow(18), scale, RoundingMode.FLOOR)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    private const val K = 1000
    private const val M = 1000000

    /**
     * NAS格式化
     * NAS<1000 => ###.####
     * NAS>1000 && NAS<1000000 => ###K
     * NAS>1000000 => ###M
     * @param wei NAS数量（单位：WEI）
     * @return 格式化后的字符串
     */
    fun nasFormat2KM(wei: String?): String {
        if (wei.isNullOrEmpty()) {
            return "0"
        }
        try {
            val decimalWEI = BigDecimal(wei)
            if (decimalWEI.toDouble() < 0) {
                return "0"
            }
            val decimalNAS = decimalWEI.divide(BigDecimal.TEN.pow(18), 4, RoundingMode.FLOOR)
            return when {
                decimalNAS.toDouble() < K -> decimalNAS.toPlainString()
                decimalNAS.toDouble() < M -> {
                    val k = decimalNAS.divide(BigDecimal.TEN.pow(3), 3, RoundingMode.FLOOR)
                    val df = DecimalFormat("###,##0")
                    "${df.format(k)}K"
                }
                else -> {
                    val m = decimalNAS.divide(BigDecimal.TEN.pow(6), 3, RoundingMode.FLOOR)
                    val df = DecimalFormat("###,##0")
                    "${df.format(m)}M"
                }
            }
        } catch (e: Exception) {
            return "0"
        }
    }
}