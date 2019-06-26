package io.nebulas.wallet.android.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.support.v7.widget.AppCompatEditText
import android.util.AttributeSet
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.dialog.NasBottomDialog

/**
 * Created by alina on 2018/7/20.
 */

class ListenActionEditText : AppCompatEditText {
    private var mContext: Context? = null

    constructor(context: Context) : super(context) {
        mContext = context
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        mContext = context
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        if (id == android.R.id.paste) showDialog()
        return super.onTextContextMenuItem(id)
    }

    private fun showDialog(){
        val clipboard = (mContext!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        try {
            if (clipboard.primaryClip.itemCount > 0 && clipboard.primaryClip.getItemAt(0) != null && (clipboard.primaryClip.getItemAt(0).coerceToText(mContext).isNotEmpty())) {
                NasBottomDialog.Builder(mContext!!)
                        .withTitle(mContext!!.getString(R.string.tips_title))
                        .withContent(this.tag as String)
                        .withCancelButton(mContext?.getString(R.string.not_now), block = { _, dialog ->
                            dialog.dismiss()
                        })
                        .withConfirmButton(mContext?.getString(R.string.clear_now)) { _, dialog ->
                            clipboard.primaryClip = ClipData.newPlainText("empty", " ")
                            clipboard.primaryClip = ClipData.newPlainText("empty",  null)

                            dialog.dismiss()
                            WalletApplication.INSTANCE.activity?.toastSuccessMessage(R.string.tip_clear_success)
                        }
                        .build()
                        .show()
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

}
