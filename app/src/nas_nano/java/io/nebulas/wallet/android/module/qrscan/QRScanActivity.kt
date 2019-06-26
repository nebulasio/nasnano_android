package io.nebulas.wallet.android.module.qrscan

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.young.scanner.CameraStatusCallback
import com.young.scanner.DecodeListener
import com.young.scanner.ScannerComponent
import com.young.scanner.zxing.DecodeFactoryZxing
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.detail.BalanceDetailActivity
import io.nebulas.wallet.android.module.transaction.transfer.TransferActivity
import io.nebulas.wallet.android.util.FileTools
import kotlinx.android.synthetic.nas_nano.activity_qrscan.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.uiThread
import org.jetbrains.annotations.NotNull
import org.json.JSONObject


/**
 * Created by Heinoc on 2018/2/2.
 */

class QRScanActivity : BaseActivity(), DecodeListener, CameraStatusCallback {

    companion object {
        /**
         * 扫描成功后需要跳转到转账页面？还会返回上一页面
         */
        const val NEED_GOTO_TRANSFER = "needGotoTransfer"

        private const val GALLERY_ENABLED = "galleryEnabled"
        /**
         * QRScan qrCode result text key
         */
        const val SCAN_QRCODE_RESULT_TEXT = "scanQRcodeResultText"

        /**
         * 申请相机权限的requestCode
         */
        const val REQUEST_CODE_CAMERA_PERMISSION = 0x00098
        /**
         * 申请存储权限的requestCode
         */
        const val REQUEST_CODE_STORAGE_PERMISSION = 0x00099
        /**
         * 打开相册的requestCode
         */
        const val REQUEST_CODE_ALBUM = 10009

        /**
         * 启动QRScanActivity
         *
         * @param context
         */
        fun launch(@NotNull context: Context) {
            context.startActivity<QRScanActivity>()
        }

        /**
         * 启动QRScanActivity
         *
         * @param context
         * @param requestCode
         */
        fun launch(@NotNull context: Context,
                   @NotNull requestCode: Int,
                   galleryEnabled: Boolean = true) {
            (context as AppCompatActivity).startActivityForResult<QRScanActivity>(requestCode,
                    GALLERY_ENABLED to galleryEnabled)
        }

    }

    var galleryEnabled: Boolean = true

    var cameraPermission: Array<String> = arrayOf(android.Manifest.permission.CAMERA)
    var writeExternalStoragePermission: Array<String> = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    lateinit var picBitmap: Bitmap

    var scannerComponent: ScannerComponent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSwipeBackEnable(false)
        setContentView(R.layout.activity_qrscan)
    }

    override fun initView() {
        showBackBtn(false, toolbar)
        titleTV.setText(R.string.action_scan_qr)
        galleryEnabled = intent.getBooleanExtra(GALLERY_ENABLED, true)
        if (!checkPermissions(cameraPermission)) {
            requestPermission(cameraPermission, REQUEST_CODE_CAMERA_PERMISSION)
        }

        fromAlbumLayout.visibility = if (galleryEnabled) {
            fromAlbumLayout.setOnClickListener {
                if (!checkPermissions(writeExternalStoragePermission))
                    requestPermission(writeExternalStoragePermission, REQUEST_CODE_STORAGE_PERMISSION)
                else
                    getPic()
            }
            View.VISIBLE
        } else {
            View.GONE
        }

    }

    override fun onStart() {
        super.onStart()
        if (checkPermissions(cameraPermission) && scannerComponent == null) {
            scannerComponent = ScannerComponent(this, surfaceView, this, this, scannerView, true, DecodeFactoryZxing())
        }
    }

    private fun getPic() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_ALBUM)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (verifyPermissions(grantResults)) {
            //权限开启成功
            when (requestCode) {
                REQUEST_CODE_CAMERA_PERMISSION -> {
//                    onResume()
                    scannerComponent = ScannerComponent(this, surfaceView, this, this, scannerView, true, DecodeFactoryZxing())
                }

                REQUEST_CODE_STORAGE_PERMISSION -> {
                    getPic()
                }
            }
        } else {
            //权限开启失败
            showPermissionTips()
        }

    }

    override fun onCameraOpenFailed() {
        showPermissionTips()
    }

    override fun onDecode(result: String) {
        val intent = Intent()

        try {
            val jsonObject = JSONObject(result)
            val pageParams: JSONObject? = jsonObject.optJSONObject("pageParams")
            val gotoTransfer = pageParams?.run {
                if (!pageParams.has("innerPay")) {
                    // 增加App内部支付字段
                    this.put("innerPay", true)
                }
                DataCenter.setData(Constants.KEY_DAPP_TRANSFER_JSON, this.toString())
                true
            } ?: false

            intent.putExtra(TransferActivity.HAS_INTENT, true)
            intent.putExtra(TransferActivity.HAS_PROTO, true)
            intent.putExtra(NEED_GOTO_TRANSFER, gotoTransfer)
            intent.putExtra(TransferActivity.FROM_ACTIVITY, QRScanActivity::class.java.name)
        }catch (e: java.lang.Exception){
        }

        intent.putExtra(SCAN_QRCODE_RESULT_TEXT, result)
        setResultAndFinish(intent)
    }

    private fun decodePic(imagePath: String) {
        picBitmap = compressPicture(imagePath)
        doAsync {
            if (picBitmap == null)
                return@doAsync

            val picWidth = picBitmap.width
            val picHeight = picBitmap.height
            val pix = IntArray(picWidth * picHeight)

            picBitmap.getPixels(pix, 0, picWidth, 0, 0, picWidth, picHeight)

            //构造LuminanceSource对象
            val rgbLuminanceSource = RGBLuminanceSource(picWidth, picHeight, pix)
            val bb = BinaryBitmap(HybridBinarizer(rgbLuminanceSource))
            //因为解析的条码类型是二维码，所以这边用QRCodeReader最合适。
            val qrCodeReader = QRCodeReader()

            val hints: MutableMap<DecodeHintType, Any> = mutableMapOf()
            hints[DecodeHintType.CHARACTER_SET] = "utf-8"
            hints[DecodeHintType.TRY_HARDER] = true

            var result: Result? = null
            try {
                result = qrCodeReader.decode(bb, hints)

                uiThread {
                    val intent = Intent()


                    try {
                        val jsonObject = JSONObject(result.toString())
                        val pageParams: JSONObject? = jsonObject.optJSONObject("pageParams")
                        val gotoTransfer = pageParams?.run {
                            if (!pageParams.has("innerPay")) {
                                // 增加App内部支付字段
                                this.put("innerPay", true)
                            }
                            DataCenter.setData(Constants.KEY_DAPP_TRANSFER_JSON, this.toString())

                            true
                        } ?: false

                        intent.putExtra(TransferActivity.HAS_INTENT, true)
                        intent.putExtra(TransferActivity.HAS_PROTO, true)
                        intent.putExtra(NEED_GOTO_TRANSFER, gotoTransfer)
                        intent.putExtra(TransferActivity.FROM_ACTIVITY, QRScanActivity::class.java.name)
                    }catch (e: java.lang.Exception){
                    }

                    intent.putExtra(SCAN_QRCODE_RESULT_TEXT, result.toString())
                    setResultAndFinish(intent)
                }

                return@doAsync
            } catch (e: Exception) {
                e.printStackTrace()

                uiThread {
                    toastErrorMessage(R.string.scan_failed)
                }


                return@doAsync
            }

        }
    }

    private fun setResultAndFinish(intent: Intent) {

        //sleep some seconds so that the beep can play
        doAsync {
            Thread.sleep(200)
            uiThread {
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SETTING_PAGE -> {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!checkPermissions(cameraPermission)) {
                        showPermissionTips()
                    } else {
                        onResume()
                    }
                }
            }

            REQUEST_CODE_ALBUM -> {
                if (resultCode == Activity.RESULT_OK) {
                    val uri = data!!.data
                    decodePic(FileTools.getPath(this, uri)!!)
                }
            }

            BalanceDetailActivity.REQUEST_CODE_TRANSFER -> {
                if (resultCode == Activity.RESULT_OK) {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }

        }
    }

    private fun compressPicture(imgPath: String): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imgPath, options)

        options.inSampleSize = calculateInSampleSize(options, 300, 300)

        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565
        val afterCompressBm = BitmapFactory.decodeFile(imgPath, options)
        //      //默认的图片格式是Bitmap.Config.ARGB_8888

        return afterCompressBm
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

}
