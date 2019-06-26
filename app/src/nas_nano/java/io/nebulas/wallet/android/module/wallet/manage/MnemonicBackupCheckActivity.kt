package io.nebulas.wallet.android.module.wallet.manage

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.LinearLayout
import android.widget.TextView
import io.nebulas.wallet.android.BuildConfig
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.mainHandler
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.extensions.errorToast
import io.nebulas.wallet.android.extensions.successToast
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import kotlinx.android.synthetic.nas_nano.activity_mnemonic_backup_check.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.uiThread
import org.jetbrains.annotations.NotNull

class MnemonicBackupCheckActivity : BaseActivity() {

    companion object {

        /**
         * 当前需要备份的钱包
         */
        const val WALLET = "wallet"
        /**
         * 备份成功的点击入口
         */
        const val CLICK_BACKUP_FROM = "click_back_from"

        /**
         * 启动MnemonicBackupCheckActivity
         *
         * @param context
         * @param requestCode
         * @param wallet
         */
        fun launch(@NotNull context: Context, @NotNull requestCode: Int, @NotNull wallet: Wallet) {
            (context as AppCompatActivity).startActivityForResult<MnemonicBackupCheckActivity>(requestCode, WALLET to wallet)
        }

    }

    lateinit var wallet: Wallet

    private var curSelectedWords = 0
    private var selectedMnemonicWordViews = mutableListOf<TextView>()
    private var mnemonicWordViews = mutableListOf<TextView>()
    private var backupResultStatus = Activity.RESULT_CANCELED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mnemonic_backup_check)
    }

    override fun screenshotEnabled(): Boolean {
        return false
    }

    override fun onBackPressed() {
        setResult(backupResultStatus)
        super.onBackPressed()
    }

    override fun initView() {

        showBackBtn(true, toolbar)
        titleTV.setText(R.string.wallet_backup_mnemonic_title)

        wallet = intent.getSerializableExtra(WALLET) as Wallet

        confirmBtn.setOnClickListener {
            doAsync {
                val backupResult = StringBuffer()

                selectedMnemonicWordViews.forEach {
                    backupResult.append(" ")
                    backupResult.append(it.text.toString())
                }

                if (backupResult.isNotEmpty()) {
                    backupResult.deleteCharAt(0)
                }
                val walletMnemonic = wallet.getPlainMnemonic()  //此方法平均耗时250ms左右
                if (BuildConfig.DEBUG){
                    backupResult.delete(0, backupResult.length)
                    backupResult.append(walletMnemonic)
                }
                if (walletMnemonic == backupResult.toString()) {//备份成功
                    wallet.setMnemonic("")
                    if (wallet.id >= 0) {
                        DBUtil.appDB.walletDao().insertWallet(wallet)

                        run breakPoint@{
                            DataCenter.wallets.forEach {
                                if (it.id == wallet.id) {
                                    it.setMnemonic("")
                                    return@breakPoint
                                }
                            }
                        }
                    }
                    val backupFrom = DataCenter.getData(CLICK_BACKUP_FROM, true) as String
                    val key: String = if (backupFrom == "") Constants.Backup_NewUser_Success else backupFrom
                    firebaseAnalytics?.logEvent(key, Bundle())
                    uiThread {
                        backupResultStatus = Activity.RESULT_OK
                        showTipsDialog(
                                title = getString(R.string.wallet_backup_mnemonic_success),
                                message = getString(R.string.wallet_backup_mnemonic_del_tips),
                                negativeTitle = "",
                                onCancel = {},
                                positiveTitle = getString(R.string.wallet_backup_mnemonic_del_btn),
                                onConfirm = {
                                    mainHandler.postDelayed({
                                        setResult(backupResultStatus)
                                        finish()
                                    }, 200L)
                                },
                                onCustomBackPressed = {
                                    setResult(backupResultStatus)
                                    finish()
                                })
                    }
                    firebaseAnalytics?.logEvent(Constants.kABackupMnemonicSuccess, Bundle())
                } else {//备份失败
                    uiThread {
                        curSelectedWords = 0
                        errorToast(R.string.wallet_backup_mnemonic_failed)
                        confirmBtn.isClickable = false
                        confirmBtn.isEnabled = false
                        selectedMnemonicWordViews.forEach {
                            val index = it.tag as Int?
                            index?:return@forEach
                            if (index < 0) {
                                return@forEach
                            }
                            mnemonicWordViews[index].visibility = View.VISIBLE

                            it.text = ""
                            it.tag = -1
                            it.visibility = View.INVISIBLE
                        }
                    }
                }
            }
        }

        /**
         * 加载助记词Views
         */
        loadMnemonicWordsViews()
    }

    private fun loadMnemonicWordsViews() {

        val mnemonicWords = wallet.getPlainMnemonic().split(" ").shuffled()

        /**
         * 待选助记词组view
         */
        for (lines in 0 until mnemonicWords.size step 3) {
            val linearLayout = LinearLayout(this)
            linearLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1F)
            linearLayout.orientation = LinearLayout.HORIZONTAL
            linearLayout.dividerDrawable = resources.getDrawable(R.drawable.divider_9dp_for_linear_layout)
            linearLayout.showDividers = LinearLayout.SHOW_DIVIDER_BEGINNING or LinearLayout.SHOW_DIVIDER_MIDDLE or LinearLayout.SHOW_DIVIDER_END
            mnemonicWordsContainer.addView(linearLayout)

            for (index in lines until lines + 3) {
                val textView = TextView(this)
                textView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1F)
                textView.gravity = Gravity.CENTER
                textView.textSize = 15F
                textView.setTextColor(ContextCompat.getColor(this, R.color.color_202020))
                textView.setBackgroundResource(R.drawable.shape_round_corner_6dp_ffffff)

                textView.text = mnemonicWords[index]

                textView.setOnClickListener {

                    if (curSelectedWords == 0) {
                        tipsTV.visibility = View.GONE
                    }

                    it.visibility = View.INVISIBLE

                    selectedMnemonicWordViews[curSelectedWords].text = (it as TextView).text
                    selectedMnemonicWordViews[curSelectedWords].tag = index
                    selectedMnemonicWordViews[curSelectedWords].visibility = View.VISIBLE

                    curSelectedWords++

                    if (curSelectedWords == mnemonicWords.size) {
                        confirmBtn.isEnabled = true
                        confirmBtn.isClickable = true
                    }

                }

                linearLayout.addView(textView)

                mnemonicWordViews.add(textView)

            }

        }

        /**
         * 已选助记词组View
         */
        for (lines in 0 until mnemonicWords.size step 3) {
            val linearLayout = LinearLayout(this)
            linearLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1F)
            linearLayout.orientation = LinearLayout.HORIZONTAL
            linearLayout.dividerDrawable = resources.getDrawable(R.drawable.divider_9dp_for_linear_layout)
            linearLayout.showDividers = LinearLayout.SHOW_DIVIDER_BEGINNING or LinearLayout.SHOW_DIVIDER_MIDDLE or LinearLayout.SHOW_DIVIDER_END
            selectedMnemonicWordsContainer.addView(linearLayout)

            for (index in lines until lines + 3) {
                val textView = TextView(this)
                textView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1F)
                textView.gravity = Gravity.CENTER
                textView.textSize = 15F
                textView.setTextColor(ContextCompat.getColor(this, R.color.color_202020))
                textView.setBackgroundResource(R.drawable.shape_round_corner_6dp_ffffff)
                textView.visibility = View.INVISIBLE

                textView.setOnClickListener {
                    val viewIndex = try {
                        it.tag as Int
                    } catch (e: Exception) {
                        -1
                    }
                    if (viewIndex < 0) {
                        return@setOnClickListener
                    }

                    mnemonicWordViews[viewIndex].visibility = View.VISIBLE

                    if (selectedMnemonicWordViews.indexOf(it) < curSelectedWords) {
                        for (i in selectedMnemonicWordViews.indexOf(it) until curSelectedWords - 1) {
                            selectedMnemonicWordViews[i].text = selectedMnemonicWordViews[i + 1].text
                            selectedMnemonicWordViews[i].tag = selectedMnemonicWordViews[i + 1].tag
                            selectedMnemonicWordViews[i].visibility = selectedMnemonicWordViews[i + 1].visibility
                        }
                        selectedMnemonicWordViews[curSelectedWords - 1].text = ""
                        selectedMnemonicWordViews[curSelectedWords - 1].tag = -1
                        selectedMnemonicWordViews[curSelectedWords - 1].visibility = View.INVISIBLE
                    } else {
                        (it as TextView).text = ""
                        it.tag = -1
                        it.visibility = View.INVISIBLE
                    }

                    curSelectedWords--

                    if (curSelectedWords == 0) {
                        tipsTV.visibility = View.VISIBLE
                    }

                    /**
                     * 选中的助记词数量一定少于全部助记词数量，因此取消按钮可点击状态
                     */
                    confirmBtn.isEnabled = false
                    confirmBtn.isClickable = false
                }

                linearLayout.addView(textView)

                selectedMnemonicWordViews.add(textView)
            }
        }
    }


    /**
     * 获得view距activity根布局的x坐标
     */
    private fun getX(v: View?): Float {
        return if (v != null) {
            v.left + getParentX(v.parent)
        } else 0f
    }

    private fun getParentX(parent: ViewParent?): Float {
        return if (parent != null && parent is ViewGroup) {
            parent.left + getParentX(parent.parent)
        } else 0f
    }

    /**
     * 获得view距activity根布局的y坐标
     */
    private fun getY(v: View?): Float {
        return if (v != null) {
            v.top + getParentY(v.parent) - getSystemBarHeight()
        } else 0f
    }

    private fun getParentY(parent: ViewParent?): Float {
        return if (parent != null && parent is ViewGroup) {
            parent.top + getParentY(parent.parent)
        } else 0f
    }

    private fun getSystemBarHeight(): Float {
        val rectangle = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rectangle)
        return rectangle.top.toFloat()
    }


}
