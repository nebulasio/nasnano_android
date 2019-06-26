package io.nebulas.wallet.android.module.swap.introduction

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.module.swap.SwapHelper
import io.nebulas.wallet.android.module.swap.step.SwapStepActivity
import kotlinx.android.synthetic.nas_nano.activity_swap_introduction.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*

class SwapIntroductionActivity : BaseActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, SwapIntroductionActivity::class.java))
        }
    }

    private var additionalClauseDialog: AdditionalClauseDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swap_introduction)
    }

    override fun initView() {
        titleTV.text = getString(R.string.swap_title_introduction)
        showBackBtn(true, toolbar)

        tv_give_up_swap.setOnClickListener {
            firebaseAnalytics?.logEvent(Constants.Exchange_NoNeed_Click,Bundle())
            finish()
        }

        tv_start_to_swap.setOnClickListener {
            firebaseAnalytics?.logEvent(Constants.Exchange_Start_Click, Bundle())
            if (additionalClauseDialog == null) {
                additionalClauseDialog = AdditionalClauseDialog(this) { dialog ->
                    dialog.dismiss()
                    onAgree()
                }
            }
            val temp = additionalClauseDialog ?: return@setOnClickListener
            if (!temp.isShowing) {
                temp.show()
            }
        }
    }

    private fun onAgree() {
        SwapHelper.additionalClauseHasBeenAgreed(this)
        SwapStepActivity.launch(this)
        finish()
    }
}
