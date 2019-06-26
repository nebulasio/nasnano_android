package io.nebulas.wallet.android.extensions

import android.view.View
import io.nebulas.wallet.android.base.BaseActivity
import kotlinx.android.synthetic.nas_nano.app_bar_collapse.*

/**
 * Created by Heinoc on 2018/5/18.
 */


/**
 * 初始化可折叠toolbar
 */
inline fun BaseActivity.initCollapsingToolbar(title: String, crossinline onBackBtnClick: (view: View) -> Unit) {

    setSupportActionBar(toolbar)
    backBtn.setOnClickListener {
        onBackBtnClick(it)

    }
    collapsingToolbar.title = title

}