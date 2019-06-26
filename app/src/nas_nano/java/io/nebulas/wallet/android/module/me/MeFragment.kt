package io.nebulas.wallet.android.module.me

import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.base.BaseFragment
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.main.MainActivity
import io.nebulas.wallet.android.module.me.adapter.MePagerAdapter
import io.nebulas.wallet.android.module.me.model.MeListModel
import io.nebulas.wallet.android.module.setting.SettingActivity
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.nas_nano.app_bar_me.*
import kotlinx.android.synthetic.nas_nano.fragment_me.*
import org.jetbrains.anko.dip

/**
 * Created by Heinoc on 2018/1/31.
 */

class MeFragment : BaseFragment() {

    companion object {
        const val REQUEST_CODE_CHANGE_LANGUAGE = 20001
        const val REQUEST_CODE_CHANGE_CURRENCY = 20002
    }


    lateinit var items: MutableList<MeListModel>

    lateinit var tokenFragment: TokenFragment
    lateinit var walletFragment: WalletFragment
    var curFragment: Fragment? = null

    lateinit var mePagerAdapter: MePagerAdapter

    //lateinit var balanceViewModel: BalanceViewModel

    var showBalanceNumber: Boolean = true
    var balanceValue: String? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_me, container, false)

    }

    override fun initView() {
//        setHasOptionsMenu(true)
//        (activity as AppCompatActivity).setSupportActionBar(toolbar)
//
//        toolbar.setTitle("")
//        titleTV.setText(R.string.me_title)

        showBalanceNumber = !Util.getBalanceHidden(context!!)

        tokenFragment = TokenFragment()
        walletFragment = WalletFragment()

        mePagerAdapter = MePagerAdapter(fragmentManager)
        mePagerAdapter.addFragment(tokenFragment, context!!.getString(R.string.token_with_first_upper_case))
        mePagerAdapter.addFragment(walletFragment, context!!.getString(R.string.wallet_with_first_upper_case))
        meViewPager.adapter = mePagerAdapter

        tabLayout.setupWithViewPager(meViewPager)

        meViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {

            }

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
                if (p2 != 0)
                    tabIndicatorView.translationX = p2.toFloat() / Util.screenWidth(this@MeFragment.context!!) * tabIndicatorView.width

            }

            override fun onPageSelected(p0: Int) {
                curFragment = mePagerAdapter.getItem(p0)
            }
        })


        /**
         * toolbar动画
         */
//        var translationX = balanceValueDesTV.translationX
        //上划的极限高度
        val offSetHeight = context!!.dip(72).toFloat()
        //按钮的极限Y
        val offSetY = context!!.dip(52).toFloat()

        (toolbar_layout as AppBarLayout).addOnOffsetChangedListener { appBarLayout, verticalOffset ->

            //            if (verticalOffset == 0)
//                return@addOnOffsetChangedListener

            val alphaScale = 1.0f + verticalOffset / offSetHeight
            val modifyY = verticalOffset / offSetHeight * offSetY

            balanceValueDesTV.alpha = alphaScale
            maskBtn.alpha = alphaScale
            balanceValueTV.alpha = alphaScale
            settingBtn.alpha = alphaScale
            approximateTV.alpha = alphaScale


            tabLayoutContainer.translationY = modifyY

        }

        balanceValueDesTV.text = getString(R.string.total_balance_des)

        maskBtn.setOnClickListener {
            showBalanceNumber = !showBalanceNumber
            Util.setBalanceHidden(context!!, !showBalanceNumber)

            updateHiddenStatus()
        }


        (context as MainActivity).balanceViewModel().getTotalBalance().observe(this, Observer {
            balanceValue = it

            updateBalanceNumberUI()

//            curFragment?.onResume()

        })

        settingBtn.setOnClickListener {
            SettingActivity.launch(this@MeFragment.context!!)
        }

        updateHiddenStatus()
    }

    private fun updateBalanceNumberUI() {

        balanceValueDesTV.text = getString(R.string.total_balance_des)

        val it = if (showBalanceNumber) {
            approximateTV.visibility = View.VISIBLE
            approximateTV.text = "≈${Constants.CURRENCY_SYMBOL}"

            if (balanceValue != null)
                "$balanceValue"
            else
                "0.00"
        } else {
            approximateTV.visibility = View.GONE
            "****"
        }

        balanceValueTV.text = it

//        adapter.items.forEach { item -> item.showBalanceDetail = showBalanceNumber }
//        adapter.notifyDataSetChanged()

    }

    private fun updateHiddenStatus() {
        if (showBalanceNumber)
            maskBtn.setImageResource(R.drawable.eyeopen)
        else
            maskBtn.setImageResource(R.drawable.eyeclose)

        updateBalanceNumberUI()
        tokenFragment.refreshBalanceHidden(!showBalanceNumber)
        walletFragment.refreshBalanceHidden(!showBalanceNumber)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            showBalanceNumber = !Util.getBalanceHidden(context!!)
            updateHiddenStatus()
        }
    }

    override fun onStart() {
        super.onStart()
        tabIndicatorView.translationX = meViewPager.currentItem * tabIndicatorView.width.toFloat()
    }

    override fun onResume() {
        super.onResume()

        //GA埋点
        (activity as BaseActivity).firebaseAnalytics?.setCurrentScreen(activity!!, "MeFragment", null)

        //判断是否有钱包
        if (DataCenter.wallets.isEmpty()) {
            balanceValue = "0.00"

        }
        updateBalanceNumberUI()

    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        curFragment?.onActivityResult(requestCode, resultCode, data)

    }

}