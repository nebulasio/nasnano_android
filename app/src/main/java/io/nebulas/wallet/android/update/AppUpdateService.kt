package io.nebulas.wallet.android.update

import android.app.IntentService
import android.content.Context
import android.content.Intent
import io.nebulas.wallet.android.common.PreferenceConstants
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.extensions.sharedPreference
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.network.server.model.VersionResp
import io.nebulas.wallet.android.util.Util
import io.reactivex.disposables.Disposable

/**
 * Service for App update
 */
class AppUpdateService : IntentService("AppUpdateService") {

    private var isWorking = false

    override fun onHandleIntent(intent: Intent?) {
        if (isWorking) {
            return
        }
        isWorking = true
        if (intent != null) {
            val action = intent.action
            if (ACTION_UPDATE == action) {
                handleActionUpdate()
            }
        }
    }

    var disposable: Disposable? = null

    override fun onDestroy() {
        super.onDestroy()
        disposable?.apply {
            dispose()
        }
    }

    /**
     * Handle action Update in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionUpdate() {
        disposable =
                HttpManager.getServerApi().getVersion(HttpManager.getHeaderMap(), Util.appVersion(this))
                        .map { it.data }
                        .subscribe(
                                {
                                    it?.apply {
                                        if (updateType != VersionResp.UPDATE_TYPE_NONE) {
                                            if (updateType == VersionResp.UPDATE_TYPE_NORMAL) {
                                                val savedIgnoreVersion:String? = sharedPreference("News", PreferenceConstants.PREFERENCE_UPDATE_VERSION)
                                                if (versionName == savedIgnoreVersion) {
                                                    return@apply
                                                }
                                            }
                                            AppUpdateActivity.launch(context = this@AppUpdateService,
                                                    versionName = versionName,
                                                    updateType = updateType,
                                                    content = comment,
                                                    fileUrl = url)
                                        }
                                    }
                                },
                                {
                                    logD("Update : check version failed")
                                }
                        )
    }

    companion object {
        private const val ACTION_UPDATE = "io.nebulas.wallet.android.update.action.UPDATE"

        /**
         * Starts this service to perform action Foo with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        fun startActionUpdate(context: Context) {
            val intent = Intent(context, AppUpdateService::class.java)
            intent.action = ACTION_UPDATE
            context.startService(intent)
        }
    }
}
