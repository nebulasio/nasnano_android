package io.nebulas.wallet.android.module.transaction.view

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

class WalletSelectViewPager(context: Context, attrs: AttributeSet?) : ViewPager(context, attrs) {

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (adapter?.count ?: 0 > 1) {
            return super.dispatchTouchEvent(ev)
        }
        return false
    }

}