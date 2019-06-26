package io.nebulas.wallet.android.module.swap.step.step2

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProviders
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import io.nebulas.wallet.android.BuildConfig
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.PASSWORD_TYPE_COMPLEX
import io.nebulas.wallet.android.common.PASSWORD_TYPE_SIMPLE
import io.nebulas.wallet.android.common.mainHandler
import io.nebulas.wallet.android.dialog.NasBottomDialog
import io.nebulas.wallet.android.dialog.VerifyPasswordDialog
import io.nebulas.wallet.android.extensions.doOnEnd
import io.nebulas.wallet.android.extensions.doOnRepeat
import io.nebulas.wallet.android.extensions.withValueAnimator
import io.nebulas.wallet.android.image.ImageUtil
import io.nebulas.wallet.android.module.swap.SwapHashUploadIntentService
import io.nebulas.wallet.android.module.swap.SwapHelper
import io.nebulas.wallet.android.module.swap.detail.ExchangeDetailActivity
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.module.transaction.viewmodel.TransferViewModel
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.network.callback.OnResultCallBack
import io.nebulas.wallet.android.network.eth.ETHHttpManager
import io.nebulas.wallet.android.network.subscriber.HttpSubscriber
import io.nebulas.wallet.android.util.*
import io.nebulas.wallet.android.view.research.CurtainResearch
import kotlinx.android.synthetic.nas_nano.fragment_swap_transfer.*
import kotlinx.android.synthetic.nas_nano.fragment_swap_transfer.view.*
import kotlinx.android.synthetic.nas_nano.layout_receivables_share.*
import org.jetbrains.anko.dip
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import walletcore.Payload
import walletcore.Walletcore
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode


class SwapTransferFragment : Fragment() {

    companion object {
        const val REQUEST_CODE_SETTING_PAGE = 2018

        private const val KEY_SWAP_WALLET_INFO = "key_swap_wallet_info"

        @JvmStatic
        fun newInstance(swapWalletInfo: SwapHelper.SwapWalletInfo) =
                SwapTransferFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable(KEY_SWAP_WALLET_INFO, swapWalletInfo)
                    }
                }
    }

    private lateinit var swapWalletInfo: SwapHelper.SwapWalletInfo

    private lateinit var transferViewModel: TransferViewModel

    private val writeExternalStoragePermission: Array<String> = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private val permissionRequestCode = 1001
    private var bitmapForSave: Bitmap? = null

    private var ethBalance: String? = null
    private var erc20Balance: String? = null

    private var minGas = BigDecimal(SwapHelper.DEFAULT_MIN_GAS).toDouble()
    private var maxGas = BigDecimal(SwapHelper.DEFAULT_MAX_GAS).toDouble()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            swapWalletInfo = it.getSerializable(KEY_SWAP_WALLET_INFO) as SwapHelper.SwapWalletInfo
        }
        transferViewModel = ViewModelProviders.of(this).get(TransferViewModel::class.java)
        val minGasString = SwapHelper.getMinSwapGas(requireContext())
        minGas = BigDecimal(minGasString).toDouble()
        val maxGasString = SwapHelper.getMaxSwapGas(requireContext())
        maxGas = BigDecimal(maxGasString).toDouble()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_swap_transfer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        mainHandler.postDelayed({
            refreshWalletBalance()
        }, 300)
    }

    @SuppressLint("SetTextI18n")
    private fun setupViews() {
        val view = view ?: return
        val minGasString = SwapHelper.getMinSwapGas(requireContext())
        val maxGasString = SwapHelper.getMaxSwapGas(requireContext())
        view.tv_range_of_gas_fee.text = "($minGasString~$maxGasString)"
        view.tv_swap_address.text = swapWalletInfo.swapWalletAddress
        view.tv_copy_address.setOnClickListener {
            (activity as BaseActivity).firebaseAnalytics?.logEvent(Constants.Exchange_CopyAddress_Click, Bundle())
            copyAddress()
        }
        view.tv_save_pic.setOnClickListener {
            (activity as BaseActivity).firebaseAnalytics?.logEvent(Constants.Exchange_QRcode_Click, Bundle())
            savePic()
        }
        view.layout_refresh.setOnClickListener {
            refreshWalletBalance()
        }
        view.tv_confirm_swap.setOnClickListener {
            (activity as BaseActivity).firebaseAnalytics?.logEvent(Constants.Exchange_Confirm_Click, Bundle())
            verifyPassword()
        }
        val qrCodeFilePath = FileTools.getDiskCacheDir(requireContext(), Constants.QRCODE_CACHE_DIR).absolutePath + File.separator + "qr_" + swapWalletInfo.swapWalletAddress + ".jpg"
        doAsync {
            val width = requireContext().dip(96)
            val resultFlag = QRCodeUtil.createQRImage(swapWalletInfo.swapWalletAddress, width, width, null, qrCodeFilePath, true)
            if (resultFlag) {
                uiThread {
                    ImageUtil.load(requireContext(), view.iv_swap_address_qr_code, qrCodeFilePath)
                    iv_eth_icon.visibility = View.VISIBLE
                    /**
                     * share图片二维码赋值，TODO 放在这里赋值是因为当点击“分享”实时赋值是会出现二维码图片无法正常加载，暂且将赋值时间前置，有空再解决无法加载的问题吧
                     */
                    ImageUtil.load(requireContext(), shareQRCodeIV, qrCodeFilePath)
                }
            }
        }
    }

    private fun copyAddress() {
        (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = ClipData.newPlainText("account", swapWalletInfo.swapWalletAddress)
        CurtainResearch.create(requireActivity())
                .withLevel(CurtainResearch.CurtainLevel.SUCCESS)
                .withContentRes(R.string.text_copy_successful)
                .show()
    }

    private fun savePic() {
        val addressIcon = Blockies.createIcon(swapWalletInfo.swapWalletAddress, circleImage = true)
        shareAddressIconIV.setImageBitmap(addressIcon)
        share_container_layout.background = getWalletColorDrawable(requireContext(), 0)
        shareTipsTV.text = getString(R.string.swap_text_please_import_in)
        shareAddressTV.text = swapWalletInfo.swapWalletAddress
        shareTokenLogoIV.setImageResource(R.drawable.ic_eth)
        shareViewLayout.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {

                shareViewLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)

                doAsync {
                    if (bitmapForSave != null && !bitmapForSave!!.isRecycled) {
                        bitmapForSave?.recycle()
                        bitmapForSave = null
                    }
                    val width = shareViewLayout.measuredWidth
                    val height = shareViewLayout.measuredHeight
                    bitmapForSave = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val c = Canvas(bitmapForSave)
                    shareViewLayout.layout(shareViewLayout.left, shareViewLayout.top, shareViewLayout.right, shareViewLayout.bottom)
                    shareViewLayout.draw(c)

                    uiThread {
                        //图片存储到相册
                        //动态权限判断
                        requestPermissions(writeExternalStoragePermission, permissionRequestCode)
                    }

                }

            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //权限被同意
                createShareFile()
            } else {
                //权限被拒绝
                permissionDenied()
            }
        }
    }

    private fun createShareFile() {
        if (bitmapForSave == null || bitmapForSave?.isRecycled == true) {
            return
        }
        //将生成的Bitmap插入到手机的图片库当中，获取到图片路径
        MediaStore.Images.Media.insertImage(requireContext().contentResolver, bitmapForSave, null, null)
        //及时回收Bitmap对象，防止OOM
        if (bitmapForSave != null && bitmapForSave?.isRecycled == false) {
            bitmapForSave?.recycle()
            bitmapForSave = null
        }
        CurtainResearch.create(requireActivity())
                .withLevel(CurtainResearch.CurtainLevel.SUCCESS)
                .withContentRes(R.string.succ_save_pic_to_album)
                .show()
    }

    private var refreshingAnim: ValueAnimator? = null
    private var animRepeatCount = 0 //记录刷新动画当前的重复次数，用于处理刷新完成后动画的平滑过渡（执行完最后一次，不会突兀的将rotate归0）
    private fun refreshWalletBalance() {
        val view = view ?: return
        if (refreshingAnim != null) {
            return
        }
        if (!NetWorkUtil.instance.isNetWorkConnected()) {
            CurtainResearch.create(requireActivity())
                    .withLevel(CurtainResearch.CurtainLevel.ERROR)
                    .withContentRes(R.string.network_connect_exception)
                    .show()
            return
        }
        view.tv_refresh.text = getString(R.string.swap_text_refreshing)
        view.tv_refresh.setTextColor(resources.getColor(R.color.color_8F8F8F))
        refreshingAnim = view.iv_refresh.withValueAnimator(360f, 0f, 800L) {
            rotation = it
        }.apply {
            repeatCount = ValueAnimator.INFINITE
        }.doOnEnd {
            animRepeatCount = 0
            refreshingAnim = null
        }.doOnRepeat {
            animRepeatCount++
        }

        loadBalance()
    }

    private fun loadBalance() {
        refreshingAnim?.start()
        ETHHttpManager.getBalance(swapWalletInfo.swapWalletAddress, HttpSubscriber(object : OnResultCallBack<String> {
            //        ETHHttpManager.getBalance("0x3e53ce428a5c3f3fb08c1f9c31a5fed06ffdb794", HttpSubscriber(object : OnResultCallBack<String> {    //测试地址
            override fun onSuccess(t: String) {
                ethBalance = Formatter.ethHexToBalance(t)
                loadErc20Balance()
            }

            override fun onError(code: Int, errorMsg: String) {
                super.onError(code, errorMsg)
                loadErc20Balance()
            }
        }, lifecycle))
    }

    private fun loadErc20Balance() {
        ETHHttpManager.getERC20Balance(
                address = swapWalletInfo.swapWalletAddress,
//                        address = "0x3e53ce428a5c3f3fb08c1f9c31a5fed06ffdb794",    //测试地址
                subscriber = HttpSubscriber(object : OnResultCallBack<String> {
                    override fun onSuccess(t: String) {
                        erc20Balance = Formatter.ethHexToBalance(t)
                        updateBalanceView()
                        stopAnim()
                    }

                    override fun onError(code: Int, errorMsg: String) {
                        super.onError(code, errorMsg)
                        stopAnim()
                    }
                }, lifecycle))
    }

    private fun stopAnim() {
        val view = view ?: return
        refreshingAnim?.repeatCount = animRepeatCount
        view.tv_refresh.text = getString(R.string.swap_action_refresh)
        view.tv_refresh.setTextColor(resources.getColor(R.color.color_038AFB))
    }

    private fun updateBalanceView() {
        val view = view ?: return
        if (ethBalance == null) {
            ethBalance = "0.00"
        }
        if (erc20Balance == null) {
            erc20Balance = "0.00"
        }
        val ethBalanceDecimal = BigDecimal(ethBalance)
        val erc20BalanceDecimal = BigDecimal(erc20Balance)
        view.tv_balance_eth.text = Formatter.tokenFormat(ethBalanceDecimal, Constants.TOKEN_FORMAT_ETH)
        view.tv_balance_erc_20.text = Formatter.tokenFormat(erc20BalanceDecimal, Constants.TOKEN_FORMAT_ETH)
        view.tv_confirm_swap.isEnabled = ethBalanceDecimal.toDouble() >= minGas && erc20BalanceDecimal.toDouble() > 0
    }

    var verifyPWDDialog: VerifyPasswordDialog? = null
    private fun verifyPassword() {
        if (null == verifyPWDDialog) {
            verifyPWDDialog = VerifyPasswordDialog(activity = requireActivity() as BaseActivity,
                    title = getString(R.string.payment_password_text),
                    passwordType = if (swapWalletInfo.isComplexPassword) PASSWORD_TYPE_COMPLEX else PASSWORD_TYPE_SIMPLE,
                    onNext = { passPhrase ->
                        getGas(passPhrase)
                    })
        }
        verifyPWDDialog?.show()
    }

    private fun getGas(password: String) {
        val transaction = Transaction().apply {
            platform = Walletcore.ETH
            account = swapWalletInfo.swapWalletAddress
            sender = swapWalletInfo.swapWalletAddress
            amount = BigDecimal(erc20Balance).multiply(BigDecimal.TEN.pow(Constants.TOKEN_SCALE)).setScale(0, BigDecimal.ROUND_DOWN).toPlainString()
            payload = Payload().apply {
                ethContract = ETHHttpManager.getETHContractPayload(BuildConfig.OFFICIAL_SWAP_ERC20_ADDRESS, amount
                        ?: "0")
            }
        }
        transferViewModel.getErc20Gas(
                address = swapWalletInfo.swapWalletAddress,
                data = transaction.payload!!.ethContract,
                lifecycle = lifecycle,
                onComplete = { gas ->
                    transaction.gasLimit = gas
                    val gasDecimal = BigDecimal(gas)
                    val totalGasPrice = Math.min(BigDecimal(ethBalance ?: "0").toDouble(), maxGas)
                    val gasPrice = BigDecimal(totalGasPrice)
                            .divide(gasDecimal, Constants.TOKEN_SCALE, RoundingMode.DOWN)
                            .multiply(BigDecimal(10).pow(18))
                            .setScale(0, BigDecimal.ROUND_DOWN)
                    transaction.gasPrice = gasPrice.toPlainString()
                    getEthNonce(password, transaction)
                },
                onFailed = { errorMsg ->
                    onError(errorMsg)
                })
    }

    private fun getEthNonce(password: String, transaction: Transaction) {
        transferViewModel.getEthNonce(
                address = swapWalletInfo.swapWalletAddress,
                lifecycle = lifecycle,
                onComplete = { nonce ->
                    transaction.nonce = nonce
                    doSwap(password, transaction)
                },
                onFailed = { errorMsg ->
                    onError(errorMsg)
                })
    }

    private fun doSwap(password: String, transaction: Transaction) {
        val address = Address(swapWalletInfo.swapWalletAddress, swapWalletInfo.swapWalletKeystore, Walletcore.ETH)

        transferViewModel.sendRawTransaction(
                address = address,
                passPhrase = password,
                transaction = transaction,
                lifecycle = this.lifecycle,
                onComplete = {
                    SwapHelper.setCurrentSwapStatus(requireContext(), SwapHelper.SWAP_STATUS_IN_PROCESSING)
                    val now = System.currentTimeMillis()
                    val gasFee = BigDecimal(transaction.gasPrice).multiply(BigDecimal(transaction.gasLimit)).toPlainString()
                    val savedInfo = saveTransactionInfo(transaction.hash!!, transaction.amount
                            ?: "0", gasFee, now)
                    SwapHelper.setTransactionHashToBeUpload(requireContext(), transaction.hash!!)   //保存交易Hash
                    SwapHashUploadIntentService.startAction(requireContext())   //开启换币交易Hash上传服务
                    goToSwapDetail(savedInfo)
                },
                onFailed = { errorMsg ->
                    onError(errorMsg)
                })
    }

    private fun onError(errorMsg: String) {
        if (verifyPWDDialog != null && verifyPWDDialog?.isShowing == true) {
            verifyPWDDialog?.toastErrorMessage(Formatter.formatWalletErrorMsg(requireContext(), errorMsg))
            verifyPWDDialog?.reset()
        } else {
            (requireActivity() as BaseActivity).toastErrorMessage(Formatter.formatWalletErrorMsg(requireContext(), errorMsg))
        }
    }

    private fun saveTransactionInfo(hash: String, erc20Balance: String, gasFee: String, startTime: Long): SwapHelper.SwapTransactionInfo {
        return SwapHelper.saveLastSwapTransactionInfo(requireContext(), hash, erc20Balance, gasFee, startTime)
    }

    private fun goToSwapDetail(swapTransactionInfo: SwapHelper.SwapTransactionInfo) {
        if (verifyPWDDialog != null && verifyPWDDialog?.isShowing == true) {
            verifyPWDDialog?.dismiss()
        }
        SwapHelper.setReExchanging(requireContext(), false)
        ExchangeDetailActivity.launch(
                context = requireContext(),
                requestCode = 0,
                swapTransactionInfo = swapTransactionInfo)
        activity?.finish()
    }

    private fun permissionDenied() {
        NasBottomDialog.Builder(requireContext())
                .withTitle(getString(R.string.alert_tips))
                .withContent(getString(R.string.need_permissions))
                .withCancelButton(buttonText = getString(R.string.alert_negative_title), block = { _, dialog ->
                    dialog.dismiss()
                })
                .withConfirmButton(getString(R.string.alert_positive_title)) { _, dialog ->
                    dialog.dismiss()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:" + requireContext().packageName)
                    startActivityForResult(intent, REQUEST_CODE_SETTING_PAGE)
                }
                .build()
                .show()
    }

}
