package io.nebulas.wallet.android.view

import android.content.Context
import android.support.annotation.IdRes
import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow

/**
 * Created by Heinoc on 2018/3/20.
 */
class PopupWindowBuilder (private val context: Context) {

    companion object {
        fun generate(context: Context, body: PopupWindowBuilder.() -> PopupWindow): PopupWindow {
            return with(PopupWindowBuilder(context)) {
                body()
            }
        }
    }


    @LayoutRes
    var layoutId = 0
    var width = 100
    //设置监听器需要知道view的id和对应的响应事件，这里用 Pair 把他们包起来
    private val listeners: MutableList<Pair<Int, (View) -> Unit>> = mutableListOf()

    fun addListener(@IdRes id: Int, body: (View) -> Unit) {
        listeners.add(Pair(id, body))
    }

    fun build(): PopupWindow {
        val popView = LayoutInflater.from(context).inflate(layoutId, null, false)
        listeners.forEach {
            val view = popView.findViewById<View>(it.first)
            view.setOnClickListener { v -> it.second(v) }
        }
        val popupWindow = PopupWindow(popView, width, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        return popupWindow
    }
}