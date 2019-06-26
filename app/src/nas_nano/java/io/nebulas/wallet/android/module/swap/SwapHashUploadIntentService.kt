package io.nebulas.wallet.android.module.swap

import android.app.IntentService
import android.content.Context
import android.content.Intent
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.network.server.model.SwapTransactionInfoRequest
import io.reactivex.Flowable
import java.math.BigInteger
import java.util.concurrent.TimeUnit

private const val ACTION_START = "io.nebulas.wallet.android.module.swap.action.FOO"

/**
 * 用于上传换币交易Hash
 */
class SwapHashUploadIntentService : IntentService("SwapHashUploadIntentService") {

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_START -> {
                handleAction()
            }
        }
    }

    private fun handleAction() {
        val hashForUpload = SwapHelper.getTransactionHashToBeUpload(this)
        if (hashForUpload.isBlank()) {
            return
        }
        val swapWalletInfo = SwapHelper.getSwapWalletInfo(this)
        if (swapWalletInfo==null || swapWalletInfo.swapWalletAddress.isBlank()) {
            return
        }
        val swapTransactionInfo = SwapHelper.getLastSwapTransactionInfo(this)
        if (swapTransactionInfo==null || swapTransactionInfo.erc20Amount.isBlank()) {
            return
        }
        val api = HttpManager.getServerApi()
        val requestBody = SwapTransactionInfoRequest(swapWalletInfo.swapWalletAddress, hashForUpload, swapTransactionInfo.erc20Amount)
        api.uploadSwapHash(HttpManager.getHeaderMap(), requestBody)
                .retryWhen {
                    it.flatMap {
                        Flowable.timer(3L, TimeUnit.SECONDS) //3秒一次，无限重试
                    }
                }
                .subscribe({
                    if (it.code!=0) {
                        throw BizException()
                    }
                    //上传成功
                    SwapHelper.clearTransactionHashToBeUpload(this)
                },{
                    //do nothing?
                })
    }

    inner class BizException:Exception()

    companion object {
        @JvmStatic
        fun startAction(context: Context) {
            val intent = Intent(context, SwapHashUploadIntentService::class.java).apply {
                action = ACTION_START
            }
            context.startService(intent)
        }
    }
}
