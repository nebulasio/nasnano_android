package io.nebulas.wallet.android.view.swip

import android.view.View

/**
 * Created by Heinoc on 2018/7/4.
 */

interface OnPullListener {
    fun onPulling(headview: View)
    fun onCanRefreshing(headview: View)
    fun onRefreshing(headview: View)
}