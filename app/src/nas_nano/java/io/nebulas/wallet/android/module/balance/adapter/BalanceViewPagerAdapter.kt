package io.nebulas.wallet.android.module.balance.adapter

import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup

/**
 * Created by Heinoc on 2018/1/31.
 */

class BalanceViewPagerAdapter() : PagerAdapter() {

    lateinit var mViewList: List<View>

    constructor(mViewList: List<View>) : this() {
        this.mViewList = mViewList

    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        container.addView(mViewList.get(position))
        return mViewList.get(position)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`

    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(mViewList.get(position))

    }

    override fun getCount(): Int {
        return mViewList.size
    }
}