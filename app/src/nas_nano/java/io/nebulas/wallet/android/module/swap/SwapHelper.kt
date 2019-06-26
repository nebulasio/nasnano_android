package io.nebulas.wallet.android.module.swap

import android.content.Context
import android.support.annotation.IntDef
import com.google.gson.Gson
import java.io.File
import java.io.Serializable

object SwapHelper {

    const val DEFAULT_MIN_GAS = "0.001"
    const val DEFAULT_MAX_GAS = "0.002"

    /**
     * 0：换币失败；
     * 1：换币成功；
     * 2：换币中；
     * 3：没有换币
     */
    const val SWAP_STATUS_FAILED = 0
    const val SWAP_STATUS_SUCCESS = 1
    const val SWAP_STATUS_IN_PROCESSING = 2
    const val SWAP_STATUS_NONE = 3

    private const val FILE_SWAP = "file_swap"

    private const val FILE_SWAP_WALLET = "file_swap_wallet"

    private const val FILE_SWAP_TRANSACTION = "file_swap_transaction"


    class SwapConfiguration {
        var clauseHasBeenAgreed: Boolean = false
        var isInReExchangeProcessing: Boolean = false
        var isReExchanging: Boolean = false
        var currentSwapStatus: Int = SWAP_STATUS_NONE
        var transactionHashToBeUpload: String = ""
        var swapMinGas: String = DEFAULT_MIN_GAS
        var swapMaxGas: String = DEFAULT_MAX_GAS
        var swapTransferConfig: Boolean = false
        var swapProcessingDescription: String = ""
    }

    private fun getConfiguration(context: Context): SwapConfiguration {
        val file = getFile(context, FILE_SWAP)
        val content = file.readText()
        val gSon = Gson()
        return if (content.isEmpty()) {
            SwapConfiguration()
        } else {
            gSon.fromJson(content, SwapConfiguration::class.java)
        }
    }

    fun additionalClauseHasBeenAgreed(context: Context) {
        val file = getFile(context, FILE_SWAP)
        val content = file.readText()
        val gSon = Gson()
        val configuration = if (content.isEmpty()) {
            SwapConfiguration()
        } else {
            gSon.fromJson(content, SwapConfiguration::class.java)
        }
        configuration.clauseHasBeenAgreed = true
        file.writeText(gSon.toJson(configuration))
    }

    fun isAdditionalClauseAgreed(context: Context): Boolean {
        val configuration = getConfiguration(context)
        return configuration.clauseHasBeenAgreed
    }

    fun isInReExchangeProcessing(context: Context): Boolean {
        val configuration = getConfiguration(context)
        return configuration.isInReExchangeProcessing
    }

    /**
     * 用于控制是否为再换一次（显示"再次备份"、"下一步"页面）
     */
    fun setInReExchangeProcessing(context: Context, value: Boolean) {
        val file = getFile(context, FILE_SWAP)
        val content = file.readText()
        val gSon = Gson()
        val configuration = if (content.isEmpty()) {
            SwapConfiguration()
        } else {
            gSon.fromJson(content, SwapConfiguration::class.java)
        }
        configuration.isInReExchangeProcessing = value
        file.writeText(gSon.toJson(configuration))
    }

    fun isReExchanging(context: Context): Boolean {
        val configuration = getConfiguration(context)
        return configuration.isReExchanging
    }

    /**
     * 用于控制是否进入到了再换一次的换币流程中（显示"二维码"及换币钱包余额的页面）
     */
    fun setReExchanging(context: Context, value: Boolean) {
        val file = getFile(context, FILE_SWAP)
        val content = file.readText()
        val gSon = Gson()
        val configuration = if (content.isEmpty()) {
            SwapConfiguration()
        } else {
            gSon.fromJson(content, SwapConfiguration::class.java)
        }
        configuration.isReExchanging = value
        file.writeText(gSon.toJson(configuration))
    }


    fun setCurrentSwapStatus(context: Context, @SwapStatus status: Int) {
        val file = getFile(context, FILE_SWAP)
        val content = file.readText()
        val gSon = Gson()
        val configuration = if (content.isEmpty()) {
            SwapConfiguration()
        } else {
            gSon.fromJson(content, SwapConfiguration::class.java)
        }
        configuration.currentSwapStatus = status
        file.writeText(gSon.toJson(configuration))
    }

    fun getCurrentSwapStatus(context: Context): Int {
        val configuration = getConfiguration(context)
        return configuration.currentSwapStatus
    }

    fun setTransactionHashToBeUpload(context: Context, hash: String) {
        val file = getFile(context, FILE_SWAP)
        val content = file.readText()
        val gSon = Gson()
        val configuration = if (content.isEmpty()) {
            SwapConfiguration()
        } else {
            gSon.fromJson(content, SwapConfiguration::class.java)
        }
        configuration.transactionHashToBeUpload = hash
        file.writeText(gSon.toJson(configuration))
    }

    fun clearTransactionHashToBeUpload(context: Context) {
        val file = getFile(context, FILE_SWAP)
        val content = file.readText()
        val gSon = Gson()
        val configuration = if (content.isEmpty()) {
            SwapConfiguration()
        } else {
            gSon.fromJson(content, SwapConfiguration::class.java)
        }
        configuration.transactionHashToBeUpload = ""
        file.writeText(gSon.toJson(configuration))
    }

    fun getTransactionHashToBeUpload(context: Context): String {
        val configuration = getConfiguration(context)
        return configuration.transactionHashToBeUpload
    }

    fun setTransferConfig(context: Context, isSupportSwap: Boolean) {
        val file = getFile(context, FILE_SWAP)
        val content = file.readText()
        val gSon = Gson()
        val configuration = if (content.isEmpty()) {
            SwapConfiguration()
        } else {
            gSon.fromJson(content, SwapConfiguration::class.java)
        }
        configuration.swapTransferConfig = isSupportSwap
        file.writeText(gSon.toJson(configuration))
    }

    fun getTransferConfig(context: Context): Boolean {
        val configuration = getConfiguration(context)
        return configuration.swapTransferConfig
    }

    fun updateSwapGasAndProcessDescription(context: Context, minGas: String?, maxGas: String?, processingDesc: String?) {
        val file = getFile(context, FILE_SWAP)
        val content = file.readText()
        val gSon = Gson()
        val configuration = if (content.isEmpty()) {
            SwapConfiguration()
        } else {
            gSon.fromJson(content, SwapConfiguration::class.java)
        }
        configuration.swapMinGas = minGas ?: DEFAULT_MIN_GAS
        configuration.swapMaxGas = maxGas ?: DEFAULT_MAX_GAS
        configuration.swapProcessingDescription = processingDesc ?: ""
        file.writeText(gSon.toJson(configuration))
    }

    fun getMinSwapGas(context: Context): String {
        val configuration = getConfiguration(context)
        return configuration.swapMinGas
    }

    fun getMaxSwapGas(context: Context): String {
        val configuration = getConfiguration(context)
        return configuration.swapMaxGas
    }

    fun getProcessingDescription(context: Context): String {
        val configuration = getConfiguration(context)
        return configuration.swapProcessingDescription
    }

    data class SwapTransactionInfo(
            var transactionHash: String = "",
            var erc20Amount: String = "0",
            var gasFee: String = "0",
            var startTime: Long = 0L
    ) : Serializable

    data class SwapWalletInfo(
            var nasWalletAddress: String = "",
            var swapWalletWords: String = "",
            var swapWalletKeystore: String = "",
            var swapWalletAddress: String = "",
            var isComplexPassword: Boolean = true
    ) : Serializable

    private var cachedSwapWalletInfo: SwapWalletInfo? = null


    fun saveSwapWalletInfo(context: Context, swapWalletInfo: SwapWalletInfo) {
        val file = getFile(context, FILE_SWAP_WALLET)
        val gSon = Gson()
        val content = gSon.toJson(swapWalletInfo)
        file.writeText(content)
    }

    fun swapWalletBackupSuccess(context: Context) {
        cachedSwapWalletInfo = null
        val file = getFile(context, FILE_SWAP_WALLET)
        val content = file.readText()
        val gSon = Gson()
        val swapWalletInfo = gSon.fromJson(content, SwapWalletInfo::class.java)
        swapWalletInfo.swapWalletWords = ""
        val newContent = gSon.toJson(swapWalletInfo)
        file.writeText(newContent)
    }

    fun getSwapWalletInfo(context: Context): SwapWalletInfo? {
        if (cachedSwapWalletInfo != null) {
            return cachedSwapWalletInfo
        }
        val file = getFile(context, FILE_SWAP_WALLET)
        val content = file.readText()
        if (content.isEmpty()) {
            return null
        }
        val gSon = Gson()
        val swapWalletInfo = gSon.fromJson(content, SwapWalletInfo::class.java)
        if (swapWalletInfo.swapWalletKeystore.isBlank() ||
                swapWalletInfo.swapWalletAddress.isBlank() ||
                swapWalletInfo.nasWalletAddress.isBlank()) {
            return null
        }
        cachedSwapWalletInfo = swapWalletInfo
        return cachedSwapWalletInfo
    }

    fun saveLastSwapTransactionInfo(context: Context,
                                    transactionHash: String,
                                    erc20Amount: String,
                                    gasFee: String,
                                    startTime: Long): SwapTransactionInfo {
        val transactionInfo = SwapTransactionInfo(transactionHash, erc20Amount, gasFee, startTime)
        val file = getFile(context, FILE_SWAP_TRANSACTION)
        val gSon = Gson()
        val content = gSon.toJson(transactionInfo)
        file.writeText(content)
        return transactionInfo
    }

    fun updateLastSwapTransactionInfo(context: Context,
                                      transactionHash: String? = null,
                                      erc20Amount: String? = null,
                                      gasFee: String? = null,
                                      startTime: Long = -1L) {
        if (transactionHash.isNullOrEmpty()
                && erc20Amount.isNullOrEmpty()
                && gasFee.isNullOrEmpty()
                && startTime <= 0L) {
            return
        }

        val file = getFile(context, FILE_SWAP_TRANSACTION)
        val gSon = Gson()
        val content = file.readText()
        val transactionInfo = if (content.isEmpty()) {
            SwapTransactionInfo()
        } else {
            gSon.fromJson(content, SwapTransactionInfo::class.java)
        }

        transactionHash?.apply {
            transactionInfo.transactionHash = this
        }
        erc20Amount?.apply {
            transactionInfo.erc20Amount = this
        }
        gasFee?.apply {
            transactionInfo.gasFee = this
        }
        if (startTime > 0L) {
            transactionInfo.startTime = startTime
        }

        val newContent = gSon.toJson(transactionInfo)
        file.writeText(newContent)
    }

    fun clearLastSwapTransactionInfo(context: Context) {
        val file = getFile(context, FILE_SWAP_TRANSACTION)
        try {
            file.delete()
        } catch (e: Exception) {

        }
    }

    fun getLastSwapTransactionInfo(context: Context): SwapTransactionInfo? {
        val file = getFile(context, FILE_SWAP_TRANSACTION)
        val gSon = Gson()
        val content = file.readText()
        return if (content.isEmpty()) {
            null
        } else {
            gSon.fromJson(content, SwapTransactionInfo::class.java)
        }
    }

    @IntDef(SWAP_STATUS_FAILED,
            SWAP_STATUS_SUCCESS,
            SWAP_STATUS_IN_PROCESSING,
            SWAP_STATUS_NONE)
    annotation class SwapStatus

    private fun getFile(context: Context, fileName: String): File {
        val file = File("${context.filesDir.absolutePath}/$fileName")
        if (!file.exists()) {
            file.createNewFile()
        }
        return file
    }

}