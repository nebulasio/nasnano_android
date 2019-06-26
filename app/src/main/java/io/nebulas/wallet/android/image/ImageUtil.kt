package io.nebulas.wallet.android.image

import android.app.Activity
import android.content.Context
import android.widget.ImageView
import io.nebulas.wallet.android.GlideApp
import io.nebulas.wallet.android.R
import org.jetbrains.anko.doAsync

/**
 * Created by Heinoc on 2018/2/6.
 */

object ImageUtil {

    fun load(context: Context, imageView: ImageView, imageUrl: String) {
        load(context, imageView, imageUrl, R.drawable.default_img)
    }

    fun load(context: Context, imageView: ImageView, imageUrl: String, placeholder: Int = R.drawable.default_img, errImg: Int = R.drawable.default_img) {
        if (null != context) {
            if (context is Activity) {
                if (context.isFinishing || context.isDestroyed) {
                    return
                }
            }
            GlideApp.with(context).load(imageUrl).placeholder(placeholder).error(errImg).into(imageView)
        }
    }

    fun clearCache(context: Context, onFinish: () -> Unit = {}) {
        //内存缓存，需要在主线程执行
        GlideApp.get(context).clearMemory()
        doAsync {
            //磁盘缓存，需要在子线程执行
            GlideApp.get(context).clearDiskCache()

            onFinish()
        }
    }

}