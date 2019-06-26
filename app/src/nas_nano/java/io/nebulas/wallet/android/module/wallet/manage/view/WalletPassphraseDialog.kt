package io.nebulas.wallet.android.module.wallet.manage.view

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import kotlinx.android.synthetic.nas_nano.dialog_wallet_passphrase.*
import io.nebulas.wallet.android.extensions.centerToast

/**
 * Created by Heinoc on 2018/3/27.
 */
class WalletPassphraseDialog(context: Context, var title: String, var btnTitle: String, var onConfirm:(passPhrase: String) -> Unit):Dialog(context, R.style.DialogStyle){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setGravity(Gravity.BOTTOM)

        setContentView(R.layout.dialog_wallet_passphrase)

        initView()
    }

    private fun initView(){
        title_tv.text = title
        confirmBtn.text = btnTitle

        confirmBtn.setOnClickListener {
            if (walletPassPhraseET.text.toString().isNullOrEmpty()){
                (context as BaseActivity).toastErrorMessage(R.string.passPhrase_not_null)
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            onConfirm(walletPassPhraseET.text.toString())

        }

        closeIV.setOnClickListener {
            dismiss()

        }

    }

    override fun show() {
        super.show()

        var lp = window.attributes
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        window.attributes = lp

        walletPassPhraseET.setText("")

    }

    override fun dismiss() {
        progressBar.visibility = View.GONE
        super.dismiss()
    }

    fun hideProgressBar() {
        progressBar.visibility = View.GONE
    }


}