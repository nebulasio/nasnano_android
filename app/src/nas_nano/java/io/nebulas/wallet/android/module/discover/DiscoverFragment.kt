package io.nebulas.wallet.android.module.discover

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import io.nebulas.wallet.android.BuildConfig
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseFragment
import io.nebulas.wallet.android.util.BaseWebViewClient
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.nas_nano.app_bar_discover.*
import kotlinx.android.synthetic.nas_nano.fragment_discover.*

/**
 * Created by Heinoc on 2018/5/24.
 */
class DiscoverFragment: BaseFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_discover, container, false)
    }


    override fun initView() {
        isHasNetWork()
        setHasOptionsMenu(true)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar!!.title = ""
        titleTV.text = getString(R.string.title_discover)

        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        webView.webViewClient = DiscoverWebViewClient(this.context!!)

        val url = when (Util.getCurLanguage()) {
            "cn" -> {
                BuildConfig.DAPP_STORE_URL_CN
            }
            "en" -> {
                BuildConfig.DAPP_STORE_URL_EN
            }
            else -> {
                BuildConfig.DAPP_STORE_URL_EN
            }
        }
        webView.loadUrl(url)

    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                if (webView.canGoBack()){
                    webView.goBack()
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onBackPressed(): Boolean{
        if (webView.canGoBack()){
            webView.goBack()
            return true
        }
        return false
    }

    inner class DiscoverWebViewClient(context: Context): BaseWebViewClient(context){

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            val layout = getView()?.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
            layout?.isRefreshing = false
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)

            if (null != toolbar) {
                if (view?.canGoBack()!!) {
                    toolbar.setNavigationIcon(R.drawable.backarrow_black)
                } else {
                    toolbar.setNavigationIcon(R.color.transparent)
                }
            }

        }
    }


}