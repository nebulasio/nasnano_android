package io.nebulas.wallet.android.module.wallet.manage

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.EditorInfo
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.extensions.removeKeyBoard
import io.nebulas.wallet.android.extensions.showKeyBoard
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.create.viewmodel.WalletViewModel
import io.nebulas.wallet.android.util.Formatter
import kotlinx.android.synthetic.nas_nano.activity_reset_pwd.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.annotations.NotNull

@Deprecated("Use #SetPassPhraseActivity instead")
class ResetPwdActivity : BaseActivity() {

    companion object {

        const val WALLET_INDEX = "walletIndex"

        /**
         * 启动ResetPwdActivity
         *
         * @param context
         * @param requestCode
         * @param walletIndex
         */
        fun launch(@NotNull context: Context, @NotNull requestCode: Int, @NotNull walletIndex: Int) {
            (context as AppCompatActivity).startActivityForResult<ResetPwdActivity>(requestCode, WALLET_INDEX to walletIndex)
        }

    }

    var verifiedPwd = false
    var firstNewPwd = true
    var oldPwd: String? = null
    var newPwd: String? = null

    lateinit var walletViewModel: WalletViewModel

    var walletIndex = 0
    lateinit var wallet: Wallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_pwd)
    }

    override fun initView() {
        showBackBtn(true, toolbar)
        titleTV.setText(R.string.change_pwd_title)


        walletIndex = intent.getIntExtra(WALLET_INDEX, 0)
        wallet = DataCenter.wallets[walletIndex]

        walletViewModel = ViewModelProviders.of(this).get(WalletViewModel::class.java)

        confirmBtn.setOnClickListener {
            var pwd = pwdET.text.toString()

            if (pwd.isNullOrEmpty()) {
                toastErrorMessage(R.string.passPhrase_not_null)
                return@setOnClickListener
            }

            if (pwd.length < 6) {
                toastErrorMessage(R.string.pwd_need_upper_six)
                return@setOnClickListener
            }

            handlePwd(pwd)

        }

        pwdET.setOnEditorActionListener { textView, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_NEXT, EditorInfo.IME_ACTION_DONE -> {
                    val pwd = pwdET.text.toString()

                    if (pwd.isNullOrEmpty()) {
                        toastErrorMessage(R.string.passPhrase_not_null)
                        return@setOnEditorActionListener true
                    }

                    if (pwd.length < 6) {
                        toastErrorMessage(R.string.pwd_need_upper_six)
                        return@setOnEditorActionListener true
                    }
                    handlePwd(pwd)
                    true
                }
                else -> false
            }
        }


        showKeyBoard(pwdET)


    }

    private fun handlePwd(pwd: String) {
        //校验原密码
        if (!verifiedPwd) {
            progressBar.visibility = View.VISIBLE
            walletViewModel.verifyWalletPassPhrase(wallet, pwd, {
                progressBar.visibility = View.GONE
                pwdET.setText("")

                /**
                 * 为了使imeOptions生效，需要先将键盘隐藏，修改imeOptions后在弹出键盘
                 */
                removeKeyBoard()
                pwdET.imeOptions = EditorInfo.IME_ACTION_DONE
                showKeyBoard(pwdET)

                pwdET.requestFocus()

                if (it) {
                    oldPwd = pwd
                    verifiedPwd = true
                    firstNewPwd = true
                    change_pwd_title_tv.setText(R.string.change_pwd_new_pwd_title)
                    change_pwd_subtitle_tv.setText(R.string.change_pwd_new_pwd_subtitle)
                    pwdET.setHint(R.string.change_pwd_new_pwd_hint)
                }
            }, {
                toastErrorMessage(Formatter.formatWalletErrorMsg(this, it))
                progressBar.visibility = View.GONE
                pwdET.setText("")
            })


        } else {
            //第一次输入新密码
            if (firstNewPwd) {
                firstNewPwd = false

                newPwd = pwd

                pwdET.setText("")

                /**
                 * 为了使imeOptions生效，需要先将键盘隐藏，修改imeOptions后在弹出键盘
                 */
                removeKeyBoard()
                pwdET.imeOptions = EditorInfo.IME_ACTION_NEXT
                showKeyBoard(pwdET)

                pwdET.requestFocus()

                change_pwd_title_tv.setText(R.string.change_pwd_new_pwd_again_title)
                change_pwd_subtitle_tv.setText(R.string.change_pwd_new_pwd_again_subtitle)
                pwdET.setHint(R.string.change_pwd_new_pwd_again_hint)
                confirmBtn.setText(R.string.change_pwd_confirm)


            } else {
                if (newPwd == pwd) {

                    removeKeyBoard()
                    pwdET.imeOptions = EditorInfo.IME_ACTION_DONE

                    progressBar.visibility = View.VISIBLE
                    walletViewModel.changePassPhrase(wallet, oldPwd!!, newPwd!!, {
                        progressBar.visibility = View.GONE
                        setResult(Activity.RESULT_OK)
                        finish()
                    }, {
                        toastErrorMessage(Formatter.formatWalletErrorMsg(this, it))
                        progressBar.visibility = View.GONE
                    })
                } else {
                    firstNewPwd = true
                    pwdET.setText("")


                    /**
                     * 为了使imeOptions生效，需要先将键盘隐藏，修改imeOptions后在弹出键盘
                     */
                    removeKeyBoard()
                    pwdET.imeOptions = EditorInfo.IME_ACTION_DONE
                    showKeyBoard(pwdET)

                    pwdET.requestFocus()

                    change_pwd_title_tv.setText(R.string.change_pwd_new_pwd_title)
                    change_pwd_subtitle_tv.setText(R.string.change_pwd_new_pwd_subtitle)
                    pwdET.setHint(R.string.change_pwd_new_pwd_hint)

                    toastErrorMessage(R.string.pwd_not_match)
                }
            }
        }
    }


}
