package io.nebulas.wallet.android.module.qrscan

import android.app.Activity
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONException
import com.alibaba.fastjson.JSONObject
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.Constants.voteContracts
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.extensions.logI
import io.nebulas.wallet.android.module.transaction.transfer.TransferActivity
import io.nebulas.wallet.android.module.vote.VoteActivity
import io.nebulas.wallet.android.util.Util
import walletcore.Walletcore
import java.lang.Exception
import java.math.BigDecimal

class ScannerDispatcher {

    data class VoteRequest(val contractAddress:String, val amountNAT:String, val function:String, val args:String, val gasPrice:String, val gasLimit:String)

    companion object {

        fun dispatch(activity: Activity, content: String, autoFinish: Boolean = false) {
            try {
                //{"pageParams":{"pay":{"currency":"NAS","value":0,"to":"n1pADU7jnrvpPzcWusGkaizZoWgUywMRGMY","payload":{"function":"vote","args":"[\"n1x94zq4f2F1sQXVVHGm7KX2nPxfoD8sSUi\",\"meetup_country\",\"uk\",3000000000000000000]","type":"call"}}},"des":"confirmTransfer","category":"jump"}
                val json = JSON.parseObject(content)
                if (json.containsKey("pageParams")) {
                    val pageParams = json.getJSONObject("pageParams")
                    if (!pageParams.containsKey("innerPay")) {
                        // 增加App内部支付字段
                        pageParams["innerPay"] = true
                    }
                    if (pageParams.containsKey("pay")) {
                        val pay = pageParams.getJSONObject("pay")
                        if (isVoteRequest(pay)) {
                            val voteRequest = boxVoteInfo(pay)
                            VoteActivity.launch(
                                    context = activity,
                                    contractAddress = voteRequest.contractAddress,
                                    amountNAT = voteRequest.amountNAT,
                                    function = voteRequest.function,
                                    args = voteRequest.args,
                                    gasPrice = voteRequest.gasPrice,
                                    gasLimit = voteRequest.gasLimit
                            )
                        } else {
                            DataCenter.setData(Constants.KEY_DAPP_TRANSFER_JSON, JSON.toJSONString(pageParams))
                            TransferActivity.launch(activity,
                                    10000,
                                    true,
                                    QRScanActivity::class.java.name,
                                    "",
                                    true)
                        }
                    }

                }
            } catch (e: Exception) {
                val isNebAddress = Util.checkAddress(content, Walletcore.NAS)
                if (isNebAddress) {
                    TransferActivity.launch(activity,
                            10000,
                            true,
                            QRScanActivity::class.java.name,
                            content,
                            false,
                            coin = DataCenter.coins.find {
                                it.type == 1 && it.platform == Walletcore.NAS
                            })
                } else {
                    //无法识别的数据格式
                    NormalScannerResultActivity.launch(activity, content)
                }
            }
            if (autoFinish) {
                activity.finish()
            }
        }

        fun isVoteRequest(payInfo: JSONObject): Boolean {
            if (!payInfo.containsKey("to")) {
                return false
            }
            val to = payInfo.getString("to")
            if (voteContracts.contains(to)) {
                if (payInfo.containsKey("payload")) {
                    val payloadInfo = payInfo.getJSONObject("payload")
                    val type = payloadInfo.getString("type") ?: ""
                    val function = payloadInfo.getString("function") ?: ""
                    return type == "call" && function == "vote"
                }
            }
            return false
        }

        fun boxVoteInfo(payInfo: JSONObject): VoteRequest{
            val to = payInfo.getString("to")
            val tokenSymbol = Constants.voteContractsMap[to]
            val token = DataCenter.coins.find { it.tokenId==tokenSymbol }
            val payloadInfo = payInfo.getJSONObject("payload")
            val function = payloadInfo.getString("function") ?: ""
            val args = payloadInfo.getString("args")
            val argArray = JSON.parseArray(args)
            val amountWEI = argArray.last().toString()
            val wei = BigDecimal(amountWEI)
            val decimal = token?.tokenDecimals?.toInt()?:18
            val amountNAT = wei.divide(BigDecimal.TEN.pow(decimal)).toString()
            val gasPrice = payInfo.getString("gasPrice")?: "20000000000"
            val gasLimit = payInfo.getString("gasLimit")?: "800000"
            return VoteRequest(to, amountNAT, function, args, gasPrice, gasLimit)
        }
    }

}