package io.nebulas.wallet.android.extensions

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * 弹出键盘
 */
fun Activity.showKeyBoard(editText: EditText, delay: Long = 300) {
    doAsync {
        Thread.sleep(delay)
        uiThread {
            editText.requestFocus()
            val im: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
    }
}

/**
 * 隐藏键盘
 */
fun Activity.removeKeyBoard() {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val view = currentFocus
    if (view != null) {
        imm.hideSoftInputFromWindow(view.windowToken, 0) //强制隐藏键盘
    }
}


/**
 * 获取屏幕高度单位px
 *
 * @return
 */
fun Activity.getScreenHeight(): Int {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val outMetrics = DisplayMetrics()
    wm.defaultDisplay.getMetrics(outMetrics)
    return outMetrics.heightPixels
}