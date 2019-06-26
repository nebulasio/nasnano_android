package io.nebulas.wallet.android.module.wallet.create.view

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import io.nebulas.wallet.android.R
import kotlinx.android.synthetic.nas_nano.dialog_mnemonic_type.*

/**
 * Created by Heinoc on 2018/3/20.
 */
class MnemonicTypeDialog(mContext: Context) : Dialog(mContext, R.style.DialogStyle) {
    lateinit var mContext: Context
    var listener: OnMnemonicTypeSelectedListener? = null

    constructor(mContext: Context, listener: OnMnemonicTypeSelectedListener) : this(mContext) {
        this.mContext = mContext
        this.listener = listener
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setGravity(Gravity.BOTTOM)

        setContentView(R.layout.dialog_mnemonic_type)


        initView()

    }

    private fun initView() {

        closeIV.setOnClickListener {
            dismiss()

        }

        m4460TypeLayout.setOnClickListener {
            listener?.onTypeSelected(mContext.getString(R.string.default_mnemonic_type_name), mContext.getString(R.string.default_mnemonic_type))
            dismiss()
        }

    }

    override fun show() {
        super.show()

        window.setGravity(Gravity.BOTTOM)

        var lp = window.attributes
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        window.attributes = lp

    }

    override fun dismiss() {
        if (isShowing) {
            super.dismiss()
        }

    }


    interface OnMnemonicTypeSelectedListener {
        fun onTypeSelected(typeName: String, type: String)
    }

}
