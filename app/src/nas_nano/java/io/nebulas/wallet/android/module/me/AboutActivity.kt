package io.nebulas.wallet.android.module.me

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.URLConstants
import io.nebulas.wallet.android.module.html.HtmlActivity
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.nas_nano.activity_about.*
import kotlinx.android.synthetic.nas_nano.app_bar_about.*
import org.jetbrains.anko.startActivity
import org.jetbrains.annotations.NotNull


class AboutActivity : BaseActivity() {

    companion object {
        /**
         * 启动AboutActivity
         *
         * @param context
         */
        fun launch(@NotNull context: Context) {
            context.startActivity<AboutActivity>()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
    }

    override fun initView() {
        showBackBtn(false, toolbar)
        titleTV.setText(R.string.me_about_us)
        versionTV.text = Util.appVersion(this)

        email.setOnClickListener {
            try {
                val data = Intent(Intent.ACTION_SENDTO)
                data.data = Uri.parse("mailto:${getString(R.string.email_content)}")
                startActivity(data)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        terms.setOnClickListener {
            HtmlActivity.launch(context = this,
                    url = URLConstants.TERMS_URL_EN,
                    title = getString(R.string.terms))
        }

        userProtocol.setOnClickListener{
            HtmlActivity.launch(context = this,url = URLConstants.PRIVACY_URL,title = getString(R.string.user_protocol))
        }
    }
}
