package io.nebulas.wallet.android.app

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDex
import com.atp.manager.AtpKit
import com.leon.channel.helper.ChannelReaderUtil
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.configuration.Configuration
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.extensions.DelegatesExtension
import io.nebulas.wallet.android.push.message.PushManager
import io.nebulas.wallet.android.util.Util

/**
 * Created by Heinoc on 2018/2/6.
 */
class WalletApplication : Application() {

    companion object {
        var INSTANCE: WalletApplication by DelegatesExtension.notNullSingleValue<WalletApplication>()
    }

    var channel: String = "debug"
        get() {
            return ChannelReaderUtil.getChannel(this) ?: field
        }

    var uuid: String = ""
        get() {

            if (field.isNullOrEmpty()) {

                field = Util.getUUID(this)

                //todo: 该方法需要写入文件，是较好的方式，但缺点太明显，一打开应用就需要授权，体验极差！！
//                var sdPath = if (FileTools.sdPath!!.endsWith("/")) FileTools.sdPath!! else FileTools.sdPath + "/"
//                var uuidFile = sdPath + Constants.UUID_FILE_PATH + Constants.UUID_FILE_NAME
//                if (File(uuidFile).exists())
//                    field = FileTools.readFile(uuidFile)
//                else {
//                    field = UUID.randomUUID().toString()
//
//                    FileTools.writeFileToSD(Constants.UUID_FILE_PATH, Constants.UUID_FILE_NAME, field)
//
//                }

            }

            return field
        }

    var activity: BaseActivity? = null


    override fun attachBaseContext(base: Context) {

//        val locale: Locale? = when (Constants.LANGUAGE) {
//            "cn" -> {
//                Locale.SIMPLIFIED_CHINESE
//            }
//            "en" -> {
//                Locale.ENGLISH
//            }
//            else -> {
//                Locale.ENGLISH
//            }
//        }
//
//        val res = base!!.resources
//        val config = res.configuration
//        config.setLocale(locale)
//        val newContext = base.createConfigurationContext(config)
//        super.attachBaseContext(newContext)
        super.attachBaseContext(base)

        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        init()

    }

    private fun init() {

        /**
         * 初始化DBUtil
         */
        DBUtil.initAppDB(this)

        Constants.CURRENCY_SYMBOL_NAME = Util.getAppCurrencySymbol(this)
        Constants.LANGUAGE = Util.getAppLanguage(this)
        Constants.voteContractsMap = Configuration.getVoteContractsMap(this)
        Constants.voteContracts = Configuration.getVoteContracts(this)
        //MobSDK.init(this)
        PushManager.init(this)
        AtpKit.init(this, "id")
    }

}