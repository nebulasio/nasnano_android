package io.nebulas.wallet.android.module.transaction.adapter

import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup

/**
 * Created by Heinoc on 2018/3/14.
 */
class ReceivableWalletAdapter(val dataSource: List<View>): PagerAdapter() {
    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        container.addView(dataSource[position], 0)
        return dataSource[position]
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(dataSource[position])
    }

}