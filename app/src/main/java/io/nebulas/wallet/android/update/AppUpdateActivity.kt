package io.nebulas.wallet.android.update

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.RelativeLayout
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.PreferenceConstants
import io.nebulas.wallet.android.extensions.Preference
import io.nebulas.wallet.android.extensions.centerToast
import io.nebulas.wallet.android.network.server.model.VersionResp
import org.jetbrains.anko.progressDialog


class AppUpdateActivity : BaseActivity() {

    companion object {


        /**
         * 申请未知应用来源权限的requestCode
         */
        const val REQUEST_CODE_INSTALL_PACKAGES = 0x00089

        /**
         * 系统设置requestcode
         */
        const val REQUEST_CODE_SYS_SETTING = 0x00088

        private const val PARAM_VERSION_NAME = "versionName"
        private const val PARAM_UPDATE_TYPE = "updateType"
        private const val PARAM_CONTENT = "content"
        private const val PARAM_FILE_URL = "fileUrl"
        fun launch(context: Context,
                   versionName: String?,
                   updateType: Int,
                   content: String?,
                   fileUrl: String?) {
            val intent = Intent(context, AppUpdateActivity::class.java)
            intent.putExtra(PARAM_VERSION_NAME, versionName)
            intent.putExtra(PARAM_UPDATE_TYPE, updateType)
            intent.putExtra(PARAM_CONTENT, content)
            intent.putExtra(PARAM_FILE_URL, fileUrl)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    var installPackagesPermission: Array<String> = arrayOf(android.Manifest.permission.REQUEST_INSTALL_PACKAGES)

    private var versionName: String? = null
    private var updateType: Int = VersionResp.UPDATE_TYPE_NONE
    private var content: String? = null
    private var fileUrl: String? = null

    private var apkLocalPath: String? = null

    private val updateManager: UpdateManager = UpdateManager()
    private var progressDialog: ProgressDialog? = null
    var updateVersionSP: String by Preference(this, PreferenceConstants.PREFERENCE_UPDATE_VERSION, "")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(RelativeLayout(this))
        handleIntent()

        doUpdate()
    }

    override fun initView() {

    }

    private fun handleIntent() {
        versionName = intent.getStringExtra(PARAM_VERSION_NAME)
        updateType = intent.getIntExtra(PARAM_UPDATE_TYPE, VersionResp.UPDATE_TYPE_NONE)
        content = intent.getStringExtra(PARAM_CONTENT)
        fileUrl = intent.getStringExtra(PARAM_FILE_URL)
    }

    private fun doUpdate() {
        if ((updateType == VersionResp.UPDATE_TYPE_NORMAL || updateType == VersionResp.UPDATE_TYPE_FORCE)
                && fileUrl != null && fileUrl!!.isNotEmpty()) {

            if (updateType == VersionResp.UPDATE_TYPE_NORMAL) {
                if (updateVersionSP == versionName) {
                    return
                }
            }

            val negativeBtnText = if (updateType == VersionResp.UPDATE_TYPE_FORCE) {
                getString(R.string.force_update_no_btn)
            } else {
                getString(R.string.not_force_update_no_btn)
            }

            val onCustomBackPressed: (() -> Unit)? = if (updateType == VersionResp.UPDATE_TYPE_FORCE) {
                { }
            } else {
                {

                    this.finish()
                    overridePendingTransition(0, R.anim.exit_bottom_out)

                }
            }

            showTipsDialog(
                    title = getString(R.string.tips_title),
                    message = content!!,
                    negativeTitle = negativeBtnText,
                    onCancel = {
                        updateVersionSP = versionName!!
                        onUpdateCanceled()
                    },
                    positiveTitle = getString(R.string.update_btn),
                    onConfirm = {
                        onUpdateConfirmed()
                    },
                    onCustomBackPressed = onCustomBackPressed)
        }
    }

    private fun onUpdateCanceled() {
        checkIfExitNeeded()
    }

    private fun onUpdateConfirmed() {
        updateManager.update(
                context = this,
                url = fileUrl!!,
                targetMD5 = "",
                listener = object : UpdateManager.UpdateListener {
                    override fun onStart() {
                        if (progressDialog == null) {
                            progressDialog = progressDialog(title = getString(R.string.tip_new_app_version)) {
                                max = 100
                                setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                                setCanceledOnTouchOutside(false)
                                setCancelable(false)
                            }
                        }
                        progressDialog?.show()
                    }

                    override fun onProgress(percent: Int) {
                        progressDialog?.progress = percent
                    }

                    override fun onSuccess(localPath: String) {
                        apkLocalPath = localPath
                        //下载完成
                        progressDialog?.dismiss()

                        installApk()

                    }

                    override fun onFailure(throwable: Throwable?) {
                        centerToast(getString(R.string.toast_download_failed))
                        //下载失败，清除SP保存的版本检查信息
                        updateVersionSP = ""
                        progressDialog?.dismiss()
                        checkIfExitNeeded()
                    }
                }
        )
    }

    private fun installApk() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canInstallApk = packageManager.canRequestPackageInstalls()
            if (canInstallApk) {
                InstallManager.install(this@AppUpdateActivity, apkLocalPath ?: "")
                checkIfExitNeeded()
            } else {
                requestPermission(installPackagesPermission, REQUEST_CODE_INSTALL_PACKAGES)
                false
            }
        } else {
            InstallManager.install(this@AppUpdateActivity, apkLocalPath ?: "")
            checkIfExitNeeded()
        }
    }

    private fun checkIfExitNeeded() {
        finish()
        //强制升级
        if (updateType == VersionResp.UPDATE_TYPE_FORCE) {
            // 杀掉进程
            Process.killProcess(Process.myPid())
            System.exit(0)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_INSTALL_PACKAGES -> {
                if (verifyPermissions(grantResults)) {
                    installApk()
                } else {
                    showTipsDialog(getString(R.string.alert_tips), getString(R.string.need_install_unknown_app_permission),
                            getString(R.string.alert_negative_title), { finish() },
                            getString(R.string.alert_positive_title), {

                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        intent.data = Uri.parse("package:$packageName")
                        startActivityForResult(intent, REQUEST_CODE_SYS_SETTING)

                    })

                }
            }
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_SYS_SETTING -> {
                installApk()
            }
        }

    }

}