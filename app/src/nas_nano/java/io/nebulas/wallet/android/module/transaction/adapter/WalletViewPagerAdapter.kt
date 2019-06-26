package io.nebulas.wallet.android.module.transaction.adapter

import android.support.v4.view.PagerAdapter
import android.util.Log
import android.view.View
import android.view.ViewGroup

/**
 * Created by Heinoc on 2018/6/22.
 */
class WalletViewPagerAdapter(var dataSource: ArrayList<View>) : PagerAdapter() {
    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`

    }

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): View {
        container.addView(dataSource[position], 0)
        return dataSource[position]
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        try {
            container.removeView(dataSource[position])
        } catch (e: Exception) {
            Log.d("Error", e.toString())
        }

    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun getPageWidth(position: Int): Float {
        return 0.7f
    }

}