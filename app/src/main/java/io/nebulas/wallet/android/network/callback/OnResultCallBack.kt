package io.nebulas.wallet.android.network.callback

import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.extensions.centerToast

/**
 * Created by Heinoc on 2018/2/6.
 */

interface OnResultCallBack<in T> {

    fun onSuccess(t: T)

    fun onError(code: Int, errorMsg: String){
        //WalletApplication.INSTANCE.activity?.toastErrorMessage(errorMsg)
    }

}