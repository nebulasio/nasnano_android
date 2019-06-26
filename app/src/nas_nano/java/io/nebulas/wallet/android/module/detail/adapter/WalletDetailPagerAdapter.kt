package io.nebulas.wallet.android.module.detail.adapter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter

/**
 * Created by alina on 2018/6/26.
 */
class WalletDetailPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {
    var fragmentList: MutableList<Fragment>? = null
    var titleList: MutableList<String>? = null

    fun addFragment(fragment: Fragment, title: String) {
        if (fragmentList === null)
            fragmentList = mutableListOf(fragment)
        else
            fragmentList!!.add(fragment)

        if (titleList === null)
            titleList = mutableListOf(title)
        else
            titleList!!.add(title)

    }


    override fun getItem(position: Int): Fragment {
        return fragmentList!![position]
    }

    override fun getCount(): Int {
        return fragmentList!!.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return titleList!![position]
    }
}