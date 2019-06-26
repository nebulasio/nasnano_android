package io.nebulas.wallet.android.module.wallet.create.view

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import io.nebulas.wallet.android.R
import kotlinx.android.synthetic.nas_nano.dialog_generate_wallet_success.*

/**
 * Created by Heinoc on 2018/4/2.
 */
class GenerateWalletSuccessDialog(context: Context, var title: String, var onOkBtnClick:()->Unit): Dialog(context, R.style.DialogStyle){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setGravity(Gravity.CENTER)
        setCanceledOnTouchOutside(false)

        setContentView(R.layout.dialog_generate_wallet_success)

        initView()



    }

    private fun initView() {

        titleTV.text = title

        okTV.setOnClickListener {
            onOkBtnClick()
            dismiss()

        }


    }
}