package io.nebulas.wallet.android.module.swap.step.step1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.extensions.logD
import kotlinx.android.synthetic.nas_nano.activity_swap_address_guide.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*

class SwapAddressGuideActivity : BaseActivity() {

    companion object {
        private const val KEY_NAS_ADDRESS = "key_nas_address"
        fun launch(context: Context, nasAddress:String){
            context.startActivity(Intent(context, SwapAddressGuideActivity::class.java).apply {
                putExtra(KEY_NAS_ADDRESS, nasAddress)
            })
        }
    }

    lateinit var nasAddress: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nasAddress = intent.getStringExtra(KEY_NAS_ADDRESS)
        setContentView(R.layout.activity_swap_address_guide)
    }

    override fun initView() {
        titleTV.text = getString(R.string.swap_title_step_setup_address)
        showBackBtn(true, toolbar)

        tv_setup.setOnClickListener {
            SwapAddressCreateActivity.launch(this, nasAddress)
            finish()
        }
    }
}
