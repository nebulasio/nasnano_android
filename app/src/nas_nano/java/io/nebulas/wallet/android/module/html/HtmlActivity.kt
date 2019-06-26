package io.nebulas.wallet.android.module.html

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.util.BaseWebViewClient
import kotlinx.android.synthetic.nas_nano.activity_html.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import org.jetbrains.anko.startActivity
import org.jetbrains.annotations.NotNull

class HtmlActivity : BaseActivity() {

    companion object {
        const val URL = "url"
        const val H5_TITLE = "h5Title"

        /**
         * 启动HtmlActivity
         *
         * @param context
         * @param url
         * @param title
         */
        fun launch(@NotNull context: Context, @NotNull url: String, title: String = "") {
            context.startActivity<HtmlActivity>(URL to url, H5_TITLE to title)
        }

    }

    private var url: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_html)
    }

    override fun initView() {
//        showBackBtn(true, toolbar)
        titleTV.text = intent.getStringExtra(H5_TITLE)
        titleTV.isSelected = true

        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        // actionMenu
        actionMenuView.visibility = View.VISIBLE
        // 点击事件
        actionMenuBack.setOnClickListener {
            onBackPressed()
        }
        actionMenuClose.setOnClickListener {
            finish()
        }

        webView.webViewClient = HtmlWebViewClient(this)

        url = intent.getStringExtra(URL)
        webView.loadUrl(url = url!!)

    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            webView.destroy()
        } catch (e: Exception) {
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }

    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    inner class HtmlWebViewClient(context: Context) : BaseWebViewClient(context) {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)

            swipeRefreshLayout.isRefreshing = false

//            closeIV.visibility = if (view?.canGoBack() == true){
//                 View.VISIBLE
//            } else {
//                View.GONE
//            }

        }

    }

}
