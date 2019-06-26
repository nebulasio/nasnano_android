package io.nebulas.wallet.android.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v4.content.FileProvider
import io.nebulas.wallet.android.app.WalletApplication
import java.io.File

/**
 * Created by young on 2018/6/1.
 *
 * Manager for install apk.
 * Compat Android N
 */
object InstallManager {
    /**
     * 通过隐式意图调用系统安装程序安装APK
     */
    fun install(context: Context, filePath:String) {
        val file = File(filePath)
        val intent = Intent(Intent.ACTION_VIEW)
        // 由于没有在Activity环境下启动Activity,设置下面的标签
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N) { //判读版本是否在7.0以上
            //参数1 上下文, 参数2 Provider主机地址 和配置文件中保持一致   参数3  共享的文件
            val applicationID = WalletApplication.INSTANCE.applicationInfo.packageName
            val apkUri = FileProvider.getUriForFile(context, "$applicationID.update.FileProvider", file)
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        }else{
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
        }
        context.startActivity(intent)
    }

}