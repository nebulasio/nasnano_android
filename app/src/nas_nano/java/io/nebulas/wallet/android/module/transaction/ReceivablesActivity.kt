package io.nebulas.wallet.android.module.transaction

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.dialog.NasBottomListDialog
import io.nebulas.wallet.android.image.ImageUtil
import io.nebulas.wallet.android.module.balance.BalanceFragment
import io.nebulas.wallet.android.module.detail.BalanceDetailActivity
import io.nebulas.wallet.android.module.transaction.adapter.ReceivableWalletAdapter
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.token.viewmodel.SupportTokenViewModel
import io.nebulas.wallet.android.module.wallet.manage.MnemonicBackupCheckActivity
import io.nebulas.wallet.android.module.wallet.manage.WalletBackupActivity
import io.nebulas.wallet.android.util.*
import kotlinx.android.synthetic.nas_nano.activity_receivables.*
import kotlinx.android.synthetic.nas_nano.layout_receivables_share.*
import kotlinx.android.synthetic.nas_nano.app_bar_main_no_underline.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.uiThread
import org.jetbrains.annotations.NotNull
import java.io.File


class ReceivablesActivity : BaseActivity() {

    companion object {

        /**
         * 目标钱包
         */
        const val TARGET_WALLET = "targetWallet"

        const val REQUEST_FROM = "request_from"
        /**
         * 启动ReceivablesActivity
         */
        fun launch(@NotNull context: Context, requestCode: Int = 50001, targetWallet: Wallet? = null) {
            (context as AppCompatActivity).startActivityForResult<ReceivablesActivity>(requestCode, REQUEST_FROM to requestCode, TARGET_WALLET to targetWallet)
        }

    }


    var requestFrom = 0
    /**
     * 默认收款钱包id
     */
    var defaultWalletID = -1L
    /**
     * 包含当前token的钱包们
     */
    //lateinit var wallets: MutableList<Wallet>
    /**
     * 包含当前token的钱包们的token list
     */
    //lateinit var coins: MutableList<Coin>
    /**
     * 当前选中钱包的index
     */
    var curIndex: Int = 0

    private var selectWalletDialog: NasBottomListDialog<Wallet, WalletWrapper>? = null

    private var blockiesBitmap: Bitmap? = null
    private var qrCodeFilePath: String = ""


    var writeExternalStoragePermission: Array<String> = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    /**
     * 生成的待保存的分享图片
     */
    lateinit var sharePicBitmap: Bitmap
    var sharePicUri: Uri? = null
    /**
     * 分享图片生成之后的回调方法
     */
    lateinit var onSharePicGenerated: (picPath: String) -> Unit


    lateinit var walletViews: MutableList<View>

    lateinit var walletAdapter: ReceivableWalletAdapter

    /**
     * bitmap内存管理
     */
//    lateinit var bitmapManager: MutableList<Bitmap?>

    /**
     * 当前应用版本是否为testnet版本
     */
    var testnetVersion = false

    /**
     * 目标wallet
     */
    private var targetWallet: Wallet? = null


    private var selectedWalletWrapper: WalletWrapper? = null

    private var address: String = ""
    lateinit var supportTokenViewModel: SupportTokenViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receivables)
    }

    override fun initView() {
        curIndex = 0
        showBackBtn(true, toolbar)
        titleTV.setText(R.string.receivables_btn_text)

        if (intent.hasExtra(TARGET_WALLET) && null != intent.getSerializableExtra(TARGET_WALLET)) {
            targetWallet = intent.getSerializableExtra(TARGET_WALLET) as Wallet
        }

        requestFrom = intent.getIntExtra(REQUEST_FROM, 0)
        testnetVersion = application.packageName.endsWith("testnet")

        testNetTipsTV.visibility = if (testnetVersion) View.VISIBLE else View.GONE
        shareTestNetTipsTV.visibility = if (testnetVersion) View.VISIBLE else View.GONE
        supportTokenViewModel = ViewModelProviders.of(this).get(SupportTokenViewModel::class.java)

        addressTV.setOnClickListener {
            copyAddress()
        }

        copyTV.setOnClickListener {
            copyAddress()
        }

        savePicTV.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            getSharePic {
                progressBar.visibility = View.GONE
                toastSuccessMessage(R.string.succ_save_pic_to_album)
            }
            firebaseAnalytics?.logEvent(Constants.kAReceiveSaveClick, Bundle())
        }

        walletLayout.setOnClickListener {
            if (null == selectWalletDialog) {
                val wrappers = mutableListOf<WalletWrapper>()
                val walletCount = DataCenter.wallets.size
                (0 until walletCount).forEach {
                    val wrapper = WalletWrapper(DataCenter.wallets[it], it)
                    wrappers.add(wrapper)
                    if (it == curIndex) {
                        selectedWalletWrapper = wrapper
                    }
                }
                selectWalletDialog = NasBottomListDialog(
                        context = this,
                        title = getString(R.string.select_wallet_title),
                        dataSource = wrappers,
                        initSelectedItem = selectedWalletWrapper)
                        .also {
                            it.onItemSelectedListener = object : NasBottomListDialog.OnItemSelectedListener<Wallet, WalletWrapper> {
                                override fun onItemSelected(itemWrapper: WalletWrapper) {
                                    selectedWalletWrapper = itemWrapper
                                    curIndex = itemWrapper.index
                                    targetWallet = DataCenter.wallets[curIndex]
                                    setReceivablesInfo(curIndex)
                                }
                            }
                        }
            }
            selectWalletDialog!!.showWithSelectedItem(selectedWalletWrapper)
        }

        ibShare.setOnClickListener {
            share()
            firebaseAnalytics?.logEvent(Constants.kAReceiveShareClick, Bundle())
        }

        supportTokenViewModel.getSupportTokens().observe(this, Observer {
            if (supportTokenViewModel.isNeedPopShowTokens()) supportTokenViewModel.showSupportTokens(this)

        })
    }


    override fun onStart() {
        super.onStart()

        if (testnetVersion) {
            showTipsDialog(getString(R.string.tips_title),
                    getString(R.string.test_net_tips),
                    getString(R.string.know_it)
            ) { }
        }
    }

    private fun copyAddress() {
        firebaseAnalytics?.logEvent(Constants.kAReceiveCopyClick, Bundle())
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = ClipData.newPlainText("account", address)
        toastSuccessMessage(R.string.text_copy_successful)
    }

    private fun initWalletsAndCoins() {

        if (targetWallet == null) {
            /**
             * 默认收款钱包
             */
            defaultWalletID = Util.getDefaultReceivingWallet(this)
            if (DataCenter.wallets.any { it.id == defaultWalletID }) {
                run breakPoint@{
                    DataCenter.wallets.forEach {
                        if (it.id == defaultWalletID) {
                            curIndex = DataCenter.wallets.indexOf(it)
                            targetWallet = it
                            return@breakPoint
                        }
                    }
                }

            }
            if (targetWallet == null) {
                targetWallet = DataCenter.wallets[0]
            }
        } else {
            /**
             * 目标钱包
             */
            targetWallet?.run breakPoint@{
                DataCenter.wallets.forEach {
                    if (it.id == this.id) {
                        curIndex = DataCenter.wallets.indexOf(it)
                        return@breakPoint
                    }
                }
            }
        }
        if (targetWallet!!.isNeedBackup()) {
            showTipsDialogWithIcon(title = getString(R.string.swap_title_important_tip),
                    icon = R.drawable.icon_notice,
                    message = getString(R.string.backup_for_rec),
                    negativeTitle = getString(R.string.backup_tip_cancel),
                    onCancel = {
                        finish()
                    },
                    positiveTitle = getString(R.string.backup_tip_confirm),
                    onConfirm = {
                        if (BalanceFragment.REQUST_CODE_FROM_HOME_PAGE == requestFrom) {
                            firebaseAnalytics?.logEvent(Constants.Backup_HomeReceive_Click, Bundle())
                            DataCenter.setData(MnemonicBackupCheckActivity.CLICK_BACKUP_FROM, Constants.Backup_HomeReceive_Success)
                        } else if(BalanceDetailActivity.REQUEST_CODE_RECEIVABLES == requestFrom){
                            firebaseAnalytics?.logEvent(Constants.Backup_TokenDetail_Click, Bundle())
                            DataCenter.setData(MnemonicBackupCheckActivity.CLICK_BACKUP_FROM, Constants.Backup_TokenDetail_Success)
                        }
                        WalletBackupActivity.launch(this,
                                WalletBackupActivity.REQUEST_CODE_BACKUP_DETAIL,
                                getString(R.string.wallet_backup_mnemonic),
                                targetWallet!!)
                    },
                    onCustomBackPressed = {}
            )
            firebaseAnalytics?.logEvent(Constants.Backup_HomeReceive_Show, Bundle())
        }
    }

    override fun onResume() {
        super.onResume()
        //初始化当前token的钱包和钱包里的token
        initWalletsAndCoins()

        setReceivablesInfo(curIndex)
        if (!targetWallet!!.isNeedBackup()) {
            if (supportTokenViewModel.isNeedPopShowTokens()) supportTokenViewModel.showSupportTokens(this)
        }
    }

    private fun setReceivablesInfo(index: Int) {
        curIndex = index
        address = DataCenter.coins.first {
            it.walletId == targetWallet?.id
        }.address ?: ""

        blockiesBitmap = Blockies.createIcon(address, circleImage = true)
        addressIconIV.setImageBitmap(blockiesBitmap)
        addressTV.text = address

        walletNameTV.text = targetWallet!!.walletName
        tvWalletDefault.visibility = if (targetWallet?.id == defaultWalletID) View.VISIBLE else View.GONE
        background_layout.background = getWalletColorDrawable(this, targetWallet!!.id)

        DataCenter.wallets.forEach {
            it.selected = index == DataCenter.wallets.indexOf(it)
        }

        qrCodeFilePath = FileTools.getDiskCacheDir(this, Constants.QRCODE_CACHE_DIR).absolutePath + File.separator + "qr_" + DataCenter.wallets[index].walletName + "_" + DataCenter.wallets[index].id + ".jpg"
        doAsync {
            val resultFlag = QRCodeUtil.createQRImage(address, 800, 800, null, qrCodeFilePath)
            if (resultFlag) {
                uiThread {
                    ImageUtil.load(it, qrCodeIV, qrCodeFilePath)
                    /**
                     * share图片二维码赋值，TODO 放在这里赋值是因为当点击“分享”实时赋值是会出现二维码图片无法正常加载，暂且将赋值时间前置，有空再解决无法加载的问题吧
                     */
                    ImageUtil.load(it, shareQRCodeIV, qrCodeFilePath)

                }
            }
        }
    }

    private fun getSharePic(onPicGenerated: (picPath: String) -> Unit) {
        share_container_layout.background = getWalletColorDrawable(this, targetWallet!!.id)
        shareAddressIconIV.setImageBitmap(blockiesBitmap)
        //shareCoinSymbolTV.text = coins[curIndex].symbol
        shareAddressTV.text = address
        //ImageUtil.load(this, shareTokenLogoIV, coins[curIndex].logo)

        val vto = shareViewLayout.viewTreeObserver
        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {

                shareViewLayout.viewTreeObserver.removeGlobalOnLayoutListener(this)

                doAsync {

                    val width = shareViewLayout.measuredWidth
                    val height = shareViewLayout.measuredHeight
                    val bb = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val c = Canvas(bb)
                    shareViewLayout.layout(shareViewLayout.left, shareViewLayout.top, shareViewLayout.right, shareViewLayout.bottom)
                    shareViewLayout.draw(c)


                    uiThread {
                        //图片存储到相册

                        //动态权限判断
                        if (!checkPermissions(writeExternalStoragePermission)) {
                            sharePicBitmap = bb
                            onSharePicGenerated = onPicGenerated
                            requestPermission(writeExternalStoragePermission)
                        } else {
                            onPicGenerated(createShareFile(bb))
                        }

                    }

                }

            }
        })

    }

    /**
     * 创建分享的图片文件
     */
    private fun createShareFile(bitmap: Bitmap): String {

        //将生成的Bitmap插入到手机的图片库当中，获取到图片路径
        val filePath = MediaStore.Images.Media.insertImage(this.contentResolver, bitmap, null, null)
        //及时回收Bitmap对象，防止OOM
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
        //转uri之前必须判空，防止保存图片失败
        return if (filePath.isNullOrEmpty()) {
            ""
        } else getRealPathFromURI(this, Uri.parse(filePath))

    }

    private fun getRealPathFromURI(context: Context, contentUri: Uri): String {
        sharePicUri = contentUri
        var cursor: Cursor? = null
        try {
            val projection = arrayOf<String>(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri, projection, null, null, null)
            if (cursor == null) {
                return ""
            }
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(columnIndex)
        } finally {
            if (cursor != null) {
                cursor.close()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (verifyPermissions(grantResults)) {
                //权限开启成功
                onSharePicGenerated(createShareFile(sharePicBitmap))
                //回收bitmap
                sharePicBitmap.recycle()
            } else {
                //权限开启失败
                showPermissionTips()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_SETTING_PAGE -> {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!checkPermissions(writeExternalStoragePermission)) {
                        showPermissionTips()
                    } else {
                        onSharePicGenerated(createShareFile(sharePicBitmap))
                        //回收bitmap
                        sharePicBitmap.recycle()
                    }
                }

            }
        }
    }

    private fun share() {
        progressBar.visibility = View.VISIBLE
        getSharePic({
            progressBar.visibility = View.GONE

            val imageIntent = Intent(Intent.ACTION_SEND)
            imageIntent.type = "image/*"
            imageIntent.putExtra(Intent.EXTRA_STREAM, sharePicUri)
            startActivity(Intent.createChooser(imageIntent, getString(R.string.receivables_share)))
        })
    }

    override fun onDestroy() {
        super.onDestroy()

        if (null != blockiesBitmap)
            blockiesBitmap?.recycle()
        blockiesBitmap = null

        System.gc()

    }

    private class WalletWrapper(val wallet: Wallet, val index: Int) : NasBottomListDialog.ItemWrapper<Wallet> {
        override fun getDisplayText(): String = wallet.walletName
        override fun getOriginObject(): Wallet = wallet
        override fun isShow(): Boolean = wallet.isNeedBackup()

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item == null) {
            return false
        }
        when (item?.itemId) {
            R.id.supportToken -> {
                supportTokenViewModel.showSupportTokens(this)
            }

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.clear()
        menuInflater.inflate(R.menu.receivables_menu, menu)
        return true
    }
}
