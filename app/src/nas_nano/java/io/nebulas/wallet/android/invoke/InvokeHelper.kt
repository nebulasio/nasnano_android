package io.nebulas.wallet.android.invoke

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import io.nebulas.wallet.android.module.auth.AuthActivity
import io.nebulas.wallet.android.module.transaction.transfer.TransferActivity
import org.jetbrains.anko.newTask

object InvokeHelper {

    private var originAppPackage: String? = null
    private var resultActivity: String? = null
    private var category: String? = null

    fun setupPackage(bundle: Bundle) {
        originAppPackage = bundle.getString("package")
        resultActivity = bundle.getString("resultActivity")
    }

    fun dispatch(activity: Activity, bundle: Bundle) {
        category = bundle.getString("category")
        val params = bundle.getBundle("parameters")
        when (category) {
            "category_transfer" -> {
                TransferActivity.launchByInvoke(activity, params)
                activity.finish()
            }
            "category_auth" -> {
                AuthActivity.launch(activity, params)
                activity.finish()
            }
            else -> {
                error(activity, InvokeError.errorUnsupportedCategory)
            }
        }
    }

    fun goBack(activity: Activity, result: Bundle): Boolean {
        if (originAppPackage.isNullOrEmpty() || resultActivity.isNullOrEmpty()) {
            return false
        }
        val targetIntent = Intent().apply {
            newTask()
            putExtras(InvokeError.success.toBundle())
            putExtra("category", category)
            putExtra("data", result)
            component = ComponentName(originAppPackage, resultActivity)
        }
        reset()
        activity.startActivity(targetIntent)
        activity.finish()
//        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        activity.overridePendingTransition(0, 0)
        return true
    }

    fun error(activity: Activity, error: InvokeError) {
        val targetIntent = Intent().apply {
            newTask()
            component = ComponentName(originAppPackage, resultActivity)
            putExtras(error.toBundle())
        }
        reset()
        activity.startActivity(targetIntent)
        activity.finish()
//        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        activity.overridePendingTransition(0, 0)
    }

    private fun reset(){
        originAppPackage = null
        resultActivity = null
        category = null
    }

}