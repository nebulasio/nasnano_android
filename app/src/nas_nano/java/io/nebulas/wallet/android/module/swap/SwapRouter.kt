package io.nebulas.wallet.android.module.swap

import android.app.Activity
import io.nebulas.wallet.android.module.swap.SwapRouter.route
import io.nebulas.wallet.android.module.swap.detail.ExchangeDetailActivity
import io.nebulas.wallet.android.module.swap.introduction.SwapIntroductionActivity
import io.nebulas.wallet.android.module.swap.step.SwapStepActivity

/**
 * 换币模块路由（入口） -- 负责页面转发，外部模块只需要调用 SwapRouter.route方法
 * @see route
 */
object SwapRouter {

    /**
     * 路由方法，负责页面分发
     * @param activity   上下文
     * @param status   0：换币失败；1：换币成功；2：换币中；3：没有换币
     */
    fun route(activity: Activity, status: Int = 3) {
        if (status == 3) {
            val swapTransactionInfo = SwapHelper.getLastSwapTransactionInfo(activity)
            if (swapTransactionInfo != null && !swapTransactionInfo.transactionHash.isBlank()) {
                ExchangeDetailActivity.launch(activity, 0, swapTransactionInfo)
                return
            }
            //换币首页
            if (SwapHelper.isAdditionalClauseAgreed(activity)) {
                //直接进入首页
                SwapStepActivity.launch(activity)
            } else {
                //进入说明页面
                SwapIntroductionActivity.launch(activity)
            }
        } else {
            //换币详情页面
            if (status == 1 || status == 0) {  //换币成功或者换币失败
                val isInReExchangeProcessing = SwapHelper.isInReExchangeProcessing(activity)
                val isReExchanging = SwapHelper.isReExchanging(activity)
                if (isInReExchangeProcessing || isReExchanging) { //点过再换一次之后的逻辑处理
                    SwapStepActivity.launch(activity)
                    return
                }
            }
            val lastTransactionInfo = SwapHelper.getLastSwapTransactionInfo(activity) ?: return
            ExchangeDetailActivity.launch(activity, 0, lastTransactionInfo, status)
        }
    }

    /**
     * 再换一次/重试
     */
    fun reExchange(activity: Activity) {
        SwapStepActivity.reExchange(activity)
    }


}