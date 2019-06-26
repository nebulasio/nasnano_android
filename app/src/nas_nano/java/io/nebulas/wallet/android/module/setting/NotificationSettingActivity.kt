package io.nebulas.wallet.android.module.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.configuration.Configuration
import io.nebulas.wallet.android.module.detail.fragment.transaction.ErrorHandler
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.network.server.model.NotificationSwitchRequest
import io.nebulas.wallet.android.push.message.PushManager
import kotlinx.android.synthetic.nas_nano.activity_notification_setting.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.lang.RuntimeException
import java.util.concurrent.Future

class NotificationSettingActivity : BaseActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, NotificationSettingActivity::class.java))
        }
    }

    private val futures = mutableListOf<Future<*>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_setting)
    }

    override fun onDestroy() {
        super.onDestroy()
        for (future in futures) {
            future.apply {
                if (!isDone && !isCancelled) {
                    cancel(true)
                }
            }
        }
    }

    private val transactionNotificationCheckListener = object : CompoundButton.OnCheckedChangeListener{
        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            layout_loading.visibility = View.VISIBLE
            if (isChecked) {
                val future = doAsync(ErrorHandler { _, _ ->
                    runOnUiThread {
                        switch_transaction_notification.setOnCheckedChangeListener(null)
                        switch_transaction_notification.isChecked = false
                        switch_transaction_notification.setOnCheckedChangeListener(this)
                        layout_loading.visibility = View.GONE
                    }
                }.defaultHandler) {
                    val api = HttpManager.getServerApi()
                    val call = api.notificationSwitch(HttpManager.getHeaderMap(), NotificationSwitchRequest(1))
                    val response = call.execute().body()?: throw RuntimeException("")
                    val code = response.code?: throw RuntimeException("")
                    if (code!=0){
                        throw RuntimeException("")
                    }
                    Configuration.enableTransactionNotification(this@NotificationSettingActivity)
                    uiThread {
                        layout_loading.visibility = View.GONE
                    }
                }
                futures.add(future)
            } else {
                val future = doAsync(ErrorHandler { _, _ ->
                    runOnUiThread {
                        switch_transaction_notification.setOnCheckedChangeListener(null)
                        switch_transaction_notification.isChecked = true
                        switch_transaction_notification.setOnCheckedChangeListener(this)
                        layout_loading.visibility = View.GONE
                    }
                }.defaultHandler) {
                    val api = HttpManager.getServerApi()
                    val call = api.notificationSwitch(HttpManager.getHeaderMap(), NotificationSwitchRequest(0))
                    val response = call.execute().body()?: throw RuntimeException("")
                    val code = response.code?: throw RuntimeException("")
                    if (code!=0){
                        throw RuntimeException("")
                    }
                    Configuration.disableTransactionNotification(this@NotificationSettingActivity)
                    uiThread {
                        layout_loading.visibility = View.GONE
                    }
                }
                futures.add(future)
            }
        }
    }

    override fun initView() {
        showBackBtn(true, toolbar)
        titleTV.text = "消息提醒"
        switch_transaction_notification.isChecked = Configuration.isTransactionNotificationEnabled(this)
        switch_transaction_notification.setOnCheckedChangeListener(transactionNotificationCheckListener)
        switch_news_notification.isChecked = Configuration.isNoticeNotificationEnabled(this)
        switch_news_notification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                PushManager.subscribeTopic(PushManager.TOPIC_NOTICE)
                Configuration.enableNoticeNotification(this)
            } else {
                PushManager.unsubscribeTopic(PushManager.TOPIC_NOTICE)
                Configuration.disableNoticeNotification(this)
            }
        }
    }
}
