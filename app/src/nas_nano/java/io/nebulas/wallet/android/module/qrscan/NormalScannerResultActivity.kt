package io.nebulas.wallet.android.module.qrscan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import kotlinx.android.synthetic.nas_nano.activity_normal_scanner_result.*
import kotlinx.android.synthetic.nas_nano.app_bar_main_no_underline.*

class NormalScannerResultActivity : BaseActivity() {

    companion object {
        private const val PARAM_CONTENT = "param_content"

        fun launch(context: Context, content:String){
            context.startActivity(Intent(context, NormalScannerResultActivity::class.java).apply {
                putExtra(PARAM_CONTENT, content)
            })
        }
    }

    private lateinit var content: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent()
        setContentView(R.layout.activity_normal_scanner_result)
    }

    private fun handleIntent(){
        content = intent.getStringExtra(PARAM_CONTENT)
    }

    override fun initView() {
        showBackBtn(true, toolbar)
        tvContent.text = content
    }
}
