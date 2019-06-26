package io.nebulas.wallet.android.atp

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.support.annotation.WorkerThread
import com.atp.manager.AtpKit
import com.atp.manager.RetCallback
import com.atp.model.Transaction
import com.google.firebase.analytics.FirebaseAnalytics
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.common.*
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.dialog.VerifyPasswordDialog
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.extensions.logE
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.util.SecurityHelper
import io.nebulas.wallet.android.util.Util
import io.nebulas.wallet.android.view.research.CurtainResearch
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import renderer.StepCallback
import walletcore.Payload
import walletcore.Response
import walletcore.Walletcore
import io.nebulas.wallet.android.module.transaction.model.Transaction as NanoTransaction

object AtpHolder {

    @SuppressLint("StaticFieldLeak")
    private var dialog: VerifyPasswordDialog? = null

    @SuppressLint("StaticFieldLeak")
    private var confirmDialog: AtpConfirmDialog? = null

    private var nanoTransaction: NanoTransaction? = null

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(WalletApplication.INSTANCE)

    private val callback = object : RetCallback {

        override fun beforeSubmit(activity: Activity, transaction: Transaction?, stepCallback: StepCallback) {
            transaction ?: return
            nanoTransaction = prepareNanoTransaction(transaction)
            confirm(activity, transaction, stepCallback)
        }

        override fun cancel() {
        }

        override fun failed(errCode: String?, errMsg: String?) {
        }

        override fun succeed() {
        }

    }

    fun isRenderable(payload: String?): Boolean {
        if (payload.isNullOrEmpty()) {
            return false
        }
        return try {
            AtpKit.isRenderable(payload!!)
        } catch (e: Exception) {
            false
        }
    }

    fun route(activity: Activity, payload: String, address: String) {
        AtpKit.setConfig(payload, address)
        refreshLanguage(activity)
        AtpKit.setCallback(callback)
        AtpKit.start(activity)
    }

    fun refreshLanguage(activity: Activity) {
        var lang = Util.getCurLanguage()
        lang = when (lang) {
            "cn" -> "zh"
            "kr" -> "kr"
            else -> "en"
        }
        AtpKit.setLanguage(activity, lang)
    }

    private fun confirm(activity: Activity, transaction: Transaction, stepCallback: StepCallback) {
        val address = transaction.fromAddress ?: return
        val contract = transaction.contractAddress ?: return
        val contractInfo = AtpConfirmDialog.ContractInfo(contract, transaction.functionName
                ?: "", transaction.args)
        confirmDialog = AtpConfirmDialog(activity, address, contractInfo, object : AtpConfirmDialog.OnConfirmListener {
            override fun onConfirmedWithoutAutoDismiss(atpConfirmDialog: AtpConfirmDialog, gasWrapper: AtpConfirmDialog.GasWrapper) {
                nanoTransaction?.apply {
                    gasPrice = gasWrapper.gasPrice
                    gasLimit = gasWrapper.gasLimit
                    nonce = gasWrapper.nonce.toString()
                }

                var addressModel: Address? = null
                var walletModel: Wallet? = null

                DataCenter.addresses.forEach {
                    if (it.address == address) {
                        addressModel = it
                        return@forEach
                    }
                }
                if (addressModel == null) {
                    logD("未找到对应的钱包")
                    return
                }

                val finalAddressModel = addressModel ?: return
                DataCenter.wallets.forEach {
                    if (it.id == finalAddressModel.walletId) {
                        walletModel = it
                        return@forEach
                    }
                }
                if (walletModel == null) {
                    logD("未找到对应的钱包")
                    return
                }

                val finalWalletModel = walletModel ?: return

                if (SecurityHelper.isWalletLocked(finalWalletModel)) {
                    CurtainResearch.create(atpConfirmDialog)
                            .withLevel(CurtainResearch.CurtainLevel.ERROR)
                            .withContent(activity.getString(R.string.tip_password_has_locked))
                            .show()
                    mainHandler.postDelayed({
                        atpConfirmDialog.dismiss()
                    }, 1500)
                    return
                }
                atpConfirmDialog.dismiss()
                val passwordType = if (finalWalletModel.isComplexPwd) {
                    PASSWORD_TYPE_COMPLEX
                } else {
                    PASSWORD_TYPE_SIMPLE
                }
                showPasswordDialog(activity, passwordType, address, stepCallback)
                confirmDialog = null
            }
        })

        try {
            confirmDialog?.show()
        } catch (e: Exception) {
            logE("$e")
        }
    }

    private fun showPasswordDialog(activity: Activity, @PasswordType passwordType: Int, address: String, stepCallback: StepCallback) {
        dialog = VerifyPasswordDialog(
                activity = activity,
                title = activity.getString(R.string.payment_password_text),
                passwordType = passwordType,
                onNext = { password ->
                    doAsync {

                        var addressModel: Address? = null
                        var walletModel: Wallet? = null

                        DataCenter.addresses.forEach {
                            if (it.address == address) {
                                addressModel = it
                                return@forEach
                            }
                        }
                        if (addressModel == null) {
                            logD("未找到对应的钱包")
                            return@doAsync
                        }

                        val finalAddressModel = addressModel ?: return@doAsync
                        DataCenter.wallets.forEach {
                            if (it.id == finalAddressModel.walletId) {
                                walletModel = it
                                return@forEach
                            }
                        }
                        if (walletModel == null) {
                            logD("未找到对应的钱包")
                            return@doAsync
                        }

                        val finalWalletModel = walletModel ?: return@doAsync

                        if (SecurityHelper.isWalletLocked(finalWalletModel)) {
                            uiThread {
                                dialog?.toastErrorMessage(activity.getString(R.string.tip_password_has_locked))
                                dialog?.reset()
                            }
                            return@doAsync
                        }

                        val signResponse = signNanoTransaction(finalAddressModel.getKeyStore(), password)
                        uiThread {
                            if (signResponse == null) {
                                dialog?.toastErrorMessage(activity.getString(R.string.status_fail))
                                dialog?.reset()
                                return@uiThread
                            }
                            if (signResponse.errorCode == 0L) {
                                val sign = signResponse.rawTransaction
                                SecurityHelper.walletCorrectPassword(finalWalletModel)
                                refreshWallet(finalWalletModel)
                                dialog?.dismiss()
                                dialog = null
                                firebaseAnalytics?.logEvent(Constants.ATPAds_Password_Click, Bundle())
                                stepCallback.confirm(sign)
                            } else {
                                var errMsg = Formatter.formatWalletErrorMsg(activity, signResponse.errorMsg)
                                if (errMsg == activity.getString(R.string.wrong_pwd)) {
                                    errMsg = if (SecurityHelper.walletWrongPassword(finalWalletModel)) {
                                        activity.getString(R.string.tip_password_error_to_lock)
                                    } else {
                                        activity.getString(R.string.wrong_pwd)
                                    }
                                    refreshWallet(finalWalletModel)
                                }
                                dialog?.toastErrorMessage(errMsg)
                                dialog?.reset()
                            }
                        }
                    }
                },
                onCancel = {
                    stepCallback.cancel()
                    dialog = null
                })
        try {
            dialog?.show()
            firebaseAnalytics.logEvent(Constants.ATPAds_Password_Show, Bundle())
        } catch (e: Exception) {
            logE("$e")
        }
    }

    private fun prepareNanoTransaction(transaction: Transaction): NanoTransaction {
        return NanoTransaction().apply {
            sender = transaction.fromAddress
            account = transaction.fromAddress
            receiver = transaction.contractAddress
            platform = Walletcore.NAS
            amount = "0"
            payload = Payload().apply {
                nasType = Walletcore.TxPayloadCallType
                nasFunction = transaction.functionName
                nasArgs = getArgsString(transaction.args)
                nasSource = ""
                nasSourceType = "js"
            }
        }
    }

    @WorkerThread
    private fun signNanoTransaction(keystore: String, password: String): Response? {
        val nanoTransaction = this.nanoTransaction ?: return null
        try {
            return Walletcore.getRawTransaction(
                    nanoTransaction.platform,
                    Constants.NAS_CHAIN_ID.toString(),
                    nanoTransaction.account,
                    password,
                    keystore,
                    nanoTransaction.receiver,
                    nanoTransaction.amount,
                    nanoTransaction.nonce,
                    nanoTransaction.payload,
                    nanoTransaction.gasPrice,
                    nanoTransaction.gasLimit
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun refreshWallet(originWallet: Wallet) {
        val walletId = originWallet.id
        DataCenter.deleteWallet(originWallet)
        doAsync {
            val wallet = DBUtil.appDB.walletDao().loadWalletById(walletId)
            DataCenter.wallets.add(wallet)
            DataCenter.wallets.sortWith(kotlin.Comparator { w1, w2 ->
                return@Comparator (w1.id - w2.id).toInt()
            })
        }
    }

    private fun getArgsString(args: Array<String>): String {
        val builder = StringBuilder()
        builder.append("[")
        args.forEach {
            builder.append("\"")
            builder.append(it)
            builder.append("\"")
            builder.append(",")
        }
        if (builder.length > 1) {
            builder.deleteCharAt(builder.length - 1)
        }
        builder.append("]")
        return builder.toString()
    }

}