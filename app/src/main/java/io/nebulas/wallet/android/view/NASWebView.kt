package io.nebulas.wallet.android.view

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.AbsoluteLayout
import android.widget.EditText
import android.widget.ProgressBar
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.util.NebJSInterface
import wendu.dsbridge.DWebView

/**
 * Created by Heinoc on 2018/5/24.
 */

class NASWebView : DWebView {
    internal lateinit var context: Context

    private var progressbar: ProgressBar? = null

    private var mUploadMessage: ValueCallback<Uri>? = null

    constructor(context: Context) : super(context) {
        this.context = context
        init(context)

    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        this.context = context
        init(context)

    }


    private fun init(contextTmp: Context) {
        this.context = contextTmp


        settings.userAgentString = if (contextTmp.applicationInfo.packageName.endsWith("testnet"))
            settings.userAgentString + " NASnanoApp.Testnet/android_" + getVersionName(context)
        else
            settings.userAgentString + " NASnanoApp/android_" + getVersionName(context)

        Log.d("useragent", settings.userAgentString)
        //        getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.javaScriptEnabled = true

        settings.defaultTextEncodingName = "UTF-8"
        settings.cacheMode = WebSettings.LOAD_NO_CACHE

        settings.setSupportZoom(true)//支持网页缩放
        settings.builtInZoomControls = true//支持手势缩放
        settings.displayZoomControls = false//隐藏Zoom缩放按钮

        /**
         * 添加NebJSInterface，name命名“NebJSBridge”
         */
        addJavascriptObject(NebJSInterface(), null)

        webViewClient = MyWebViewClient()
        webChromeClient = object : MyWebChromeClient(WebChromeClient()) {

            override fun openFileChooser(uploadFile: ValueCallback<Uri>) {
                if (mUploadMessage != null)
                    return
                mUploadMessage = uploadFile
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "*/*"
                (context as BaseActivity).startActivityForResult(Intent.createChooser(
                        i, context.getString(R.string.select_file_to_upload)), 4876)
                (context as BaseActivity).overridePendingTransition(R.anim.sliding_down_in, R.anim.hold_stop)
            }

            override fun openFileChooser(uploadFile: ValueCallback<Uri>, acceptType: String) {
                openFileChooser(uploadFile)
            }

            override fun openFileChooser(uploadFile: ValueCallback<Uri>, acceptType: String,
                                         capture: String) {
                openFileChooser(uploadFile)
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (newProgress == 100) {
                    progressbar!!.visibility = View.GONE
                } else {
                    if (progressbar!!.visibility == View.GONE)
                        progressbar!!.visibility = View.VISIBLE
                    progressbar!!.progress = newProgress
                }
                super.onProgressChanged(view, newProgress)
            }
        }

        progressbar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
        progressbar!!.layoutParams = AbsoluteLayout.LayoutParams(AbsoluteLayout.LayoutParams.MATCH_PARENT, 5, 0, 0)
        progressbar!!.progressDrawable = resources.getDrawable(R.drawable.web_progress)
        addView(progressbar)

        //        synCookies(context, "paipai.com", "paipai.com");
        //        synCookies(context, "paipai.com", ".paipai.com");
    }

    fun clearmUploadMessage() {
        mUploadMessage = null
    }

    fun getmUploadMessage(): ValueCallback<Uri>? {
        return mUploadMessage
    }


    override fun loadUrl(url: String) {

        //        synCookies(context, url);

        super.loadUrl(url)

    }

    override fun reload() {
        //        synCookies(context, "paipai.com", "paipai.com");
        super.reload()
    }

    /**
     * 同步一下cookie
     */
    fun synCookies(context: Context, url: String, domain: String) {

        //        String cookielist;
        //
        //        SharedPreferences preferences = context.getSharedPreferences(PreferencesConstant.FILE_LOGIN, Context.MODE_PRIVATE);
        //        cookielist = preferences.getString(PreferencesConstant.KEY_COOKIES, "");
        //
        //        CookieSyncManager.createInstance(context);
        //        CookieManager cookieManager = CookieManager.getInstance();
        //        cookieManager.setAcceptCookie(true);
        //        //cookieManager.removeSessionCookie();//移除
        //
        //        Log.d("TTT1", cookielist);
        //        if (TextUtils.isEmpty(cookielist)) {
        //            return;
        //        }
        //        Log.d("TTT2", cookielist);
        //        String[] cookies = cookielist.split(",");
        //        for (String cookie : cookies) {
        //            cookieManager.setCookie(url, cookie + "; domain=" + domain + "; path=/");
        //            cookieManager.setCookie("paipai.com", cookie + "; domain=" + domain + "; path=/");
        //        }
        //        CookieSyncManager.getInstance().sync();
    }

    private fun getVersionName(context: Context): String {
        try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            return pi.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return "0.0.0"
        }

    }

    internal abstract inner class MyWebChromeClient protected constructor(private val mWrappedClient: WebChromeClient) : WebChromeClient() {

        /**
         * {@inheritDoc}
         */
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            mWrappedClient.onProgressChanged(view, newProgress)
        }

        /**
         * {@inheritDoc}
         */
        override fun onReceivedTitle(view: WebView, title: String) {
            mWrappedClient.onReceivedTitle(view, title)
        }

        /**
         * {@inheritDoc}
         */
        override fun onReceivedIcon(view: WebView, icon: Bitmap) {
            mWrappedClient.onReceivedIcon(view, icon)
        }

        /**
         * {@inheritDoc}
         */
        override fun onReceivedTouchIconUrl(view: WebView, url: String, precomposed: Boolean) {
            mWrappedClient.onReceivedTouchIconUrl(view, url, precomposed)
        }

        /**
         * {@inheritDoc}
         */
        override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
            mWrappedClient.onShowCustomView(view, callback)
        }

        /**
         * {@inheritDoc}
         */
        override fun onHideCustomView() {
            mWrappedClient.onHideCustomView()
        }

        /**
         * {@inheritDoc}
         */
        override fun onCreateWindow(view: WebView, dialog: Boolean, userGesture: Boolean,
                                    resultMsg: Message): Boolean {
            return mWrappedClient.onCreateWindow(view, dialog, userGesture, resultMsg)
        }

        /**
         * {@inheritDoc}
         */
        override fun onRequestFocus(view: WebView) {
            mWrappedClient.onRequestFocus(view)
        }

        /**
         * {@inheritDoc}
         */
        override fun onCloseWindow(window: WebView) {
            mWrappedClient.onCloseWindow(window)
        }

        /**
         * {@inheritDoc}
         */
        override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
            //return mWrappedClient.onJsAlert(view, url, message, result);

            val builder = AlertDialog.Builder(view.context)

            builder.setMessage(message)
                    .setPositiveButton("确定") { dialog, which -> result.confirm() }

            // 不需要绑定按键事件
            // 屏蔽keycode等于84之类的按键
            builder.setOnKeyListener { dialog, keyCode, event ->
                Log.v("onJsAlert", "keyCode==" + keyCode + "event=" + event)
                true
            }
            // 禁止响应按back键的事件
            builder.setCancelable(false)
            val dialog = builder.create()
            val ctx = view.context
            if (ctx!=null){
                if(ctx is Activity){
                    if (!ctx.isFinishing){
                        dialog.show()
                    }
                }
            }
            //result.confirm();// 因为没有绑定事件，需要强行confirm,否则页面会变黑显示不了内容。
            return true
        }

        /**
         * {@inheritDoc}
         */
        override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
            val builder = AlertDialog.Builder(view.context)
            builder.setMessage(message)
                    .setPositiveButton("确定") { dialog, which -> result.confirm() }
                    .setNeutralButton("取消") { dialog, which -> result.cancel() }
            builder.setOnCancelListener { result.cancel() }

            // 屏蔽keycode等于84之类的按键，避免按键后导致对话框消息而页面无法再弹出对话框的问题
            builder.setOnKeyListener { dialog, keyCode, event ->
                Log.v("onJsConfirm", "keyCode==" + keyCode + "event=" + event)
                true
            }
            // 禁止响应按back键的事件
            // builder.setCancelable(false);
            val dialog = builder.create()
            dialog.show()
            return true
            // return super.onJsConfirm(view, url, message, result);
        }

        /**
         * {@inheritDoc}
         */
        override fun onJsPrompt(view: WebView, url: String, message: String,
                                defaultValue: String, result: JsPromptResult): Boolean {
            val builder = AlertDialog.Builder(view.context)

            builder.setMessage(message)

            val et = EditText(view.context)
            et.setSingleLine()
            et.setText(defaultValue)
            builder.setView(et)
                    .setPositiveButton("确定") { dialog, which -> result.confirm(et.text.toString()) }
                    .setNeutralButton("取消") { dialog, which -> result.cancel() }

            // 屏蔽keycode等于84之类的按键，避免按键后导致对话框消息而页面无法再弹出对话框的问题
            builder.setOnKeyListener { dialog, keyCode, event ->
                Log.v("onJsPrompt", "keyCode==" + keyCode + "event=" + event)
                true
            }

            // 禁止响应按back键的事件
            // builder.setCancelable(false);
            val dialog = builder.create()
            dialog.show()
            return true
            //return mWrappedClient.onJsPrompt(view, url, message, defaultValue, result);
        }

        /**
         * {@inheritDoc}
         */
        override fun onJsBeforeUnload(view: WebView, url: String, message: String,
                                      result: JsResult): Boolean {
            return mWrappedClient.onJsBeforeUnload(view, url, message, result)
        }

        /**
         * {@inheritDoc}
         */
        override fun onExceededDatabaseQuota(url: String, databaseIdentifier: String,
                                             currentQuota: Long, estimatedSize: Long, totalUsedQuota: Long,
                                             quotaUpdater: WebStorage.QuotaUpdater) {
            mWrappedClient.onExceededDatabaseQuota(url, databaseIdentifier, currentQuota,
                    estimatedSize, totalUsedQuota, quotaUpdater)
        }

        /**
         * {@inheritDoc}
         */
        override fun onReachedMaxAppCacheSize(spaceNeeded: Long, totalUsedQuota: Long,
                                              quotaUpdater: WebStorage.QuotaUpdater) {
            mWrappedClient
                    .onReachedMaxAppCacheSize(spaceNeeded, totalUsedQuota, quotaUpdater)
        }

        /**
         * {@inheritDoc}
         */
        override fun onGeolocationPermissionsShowPrompt(origin: String,
                                                        callback: GeolocationPermissions.Callback) {
            mWrappedClient.onGeolocationPermissionsShowPrompt(origin, callback)
        }

        /**
         * {@inheritDoc}
         */
        override fun onGeolocationPermissionsHidePrompt() {
            mWrappedClient.onGeolocationPermissionsHidePrompt()
        }

        /**
         * {@inheritDoc}
         */
        override fun onJsTimeout(): Boolean {
            return mWrappedClient.onJsTimeout()
        }

        /**
         * {@inheritDoc}
         */
        @Deprecated("")
        override fun onConsoleMessage(message: String, lineNumber: Int, sourceID: String) {
            mWrappedClient.onConsoleMessage(message, lineNumber, sourceID)
        }

        /**
         * {@inheritDoc}
         */
        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            return mWrappedClient.onConsoleMessage(consoleMessage)
        }

        /**
         * {@inheritDoc}
         */
//        override fun getDefaultVideoPoster(): Bitmap {
//            return mWrappedClient.defaultVideoPoster
//        }

        /**
         * {@inheritDoc}
         */
        override fun getVideoLoadingProgressView(): View {
            return mWrappedClient.videoLoadingProgressView
        }

        /**
         * {@inheritDoc}
         */
        override fun getVisitedHistory(callback: ValueCallback<Array<String>>) {
            mWrappedClient.getVisitedHistory(callback)
        }

        /**
         * {@inheritDoc}
         */

        open fun openFileChooser(uploadFile: ValueCallback<Uri>) {
            (mWrappedClient as MyWebChromeClient).openFileChooser(uploadFile)
        }

        /**
         * {@inheritDoc}
         */

        open fun openFileChooser(uploadFile: ValueCallback<Uri>, acceptType: String) {
            (mWrappedClient as MyWebChromeClient).openFileChooser(uploadFile, acceptType)
        }

        /**
         * {@inheritDoc}
         */

        open fun openFileChooser(uploadFile: ValueCallback<Uri>, acceptType: String,
                                 capture: String) {
            (mWrappedClient as MyWebChromeClient).openFileChooser(uploadFile, acceptType,
                    capture)
        }
    }

    private inner class MyWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            view.loadUrl(url)
            return true
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        val lp = progressbar!!.layoutParams as AbsoluteLayout.LayoutParams
        lp.x = l
        lp.y = t
        progressbar!!.layoutParams = lp
        super.onScrollChanged(l, t, oldl, oldt)
    }

}