package io.nebulas.wallet.android.module.me

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputFilter
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.DataCenter
import kotlinx.android.synthetic.nas_nano.activity_single_input.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.annotations.NotNull


class SingleInputActivity : BaseActivity() {

    companion object {
        /**
         * activity title
         */
        const val TITLE = "title"
        /**
         * 输入框标题
         */
        const val INPUT_TITLE = "inputTitle"
        /**
         * 输入框hint
         */
        const val INPUT_HINT = "inputHint"
        /**
         * 输入框内容
         */
        const val INPUT_TEXT = "inputText"

        /**
         * 启动SingleInputActivity
         *
         * @param context
         * @param requestCode
         * @param title
         * @param inputTitle
         * @param inputHint
         * @param inputText
         */
        fun launch(@NotNull context: Context, @NotNull requestCode: Int, @NotNull title: String, @NotNull inputTitle: String, @NotNull inputHint: String, @NotNull inputText: String) {
            (context as AppCompatActivity).startActivityForResult<SingleInputActivity>(requestCode, TITLE to title, INPUT_TITLE to inputTitle, INPUT_HINT to inputHint, INPUT_TEXT to inputText)
        }

    }

    var defaultValue: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_input)
    }

    override fun initView() {
        showBackBtn(true, toolbar)

        titleTV.text = intent.getStringExtra(TITLE)
//        inputTitleTv.text = intent.getStringExtra(INPUT_TITLE)
        inputET.hint = intent.getStringExtra(INPUT_HINT)

        defaultValue = intent.getStringExtra(INPUT_TEXT)
        inputET.setText(defaultValue)


        confirmBtn.setOnClickListener {
            if (titleTV.text.toString() == getString(R.string.modify_wallet_name)) {
                var name = inputET.text.toString()
                if (name.isNotEmpty()) {

                    DataCenter.wallets.forEach {
                        if (name != defaultValue && it.walletName == name) {
                            toastErrorMessage(R.string.tip_wallet_name_exists)
                            return@setOnClickListener
                        }
                    }

                    var intent = Intent()
                    intent.putExtra(INPUT_TEXT, inputET.text.toString())
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                } else {
                    toastErrorMessage(R.string.wallet_name_should_not_empty)
                }
            } else {
                var intent = Intent()
                intent.putExtra(INPUT_TEXT, inputET.text.toString())
                setResult(Activity.RESULT_OK, intent)
                finish()

            }
        }

        /**
         * 限定备注字符串最大长度
         */
        val maxLen = 20
        val filter = InputFilter { src, start, end, dest, dstart, dend ->

            var dindex = 0
            var count = 0

            while (count <= maxLen && dindex < dest.length) {
                val c = dest[dindex++]
                if (c.toInt() < 128) {
                    count += 1
                } else {
                    count += 2
                }
            }

            if (count > maxLen) {
                return@InputFilter dest.subSequence(0, dindex - 1)
            }

            var sindex = 0
            while (count <= maxLen && sindex < src.length) {
                val c = src[sindex++]
                if (c.toInt() < 128) {
                    count += 1
                } else {
                    count += 2
                }
            }

            if (count > maxLen) {
                sindex--
            }

            src.subSequence(0, sindex)
        }
//        inputET.filters = arrayOf(filter)

    }
}
