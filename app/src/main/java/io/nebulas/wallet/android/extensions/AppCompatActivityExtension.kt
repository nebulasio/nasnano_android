package io.nebulas.wallet.android.extensions

import android.app.Activity
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import io.nebulas.wallet.android.view.research.CurtainResearch

/**
 * Created by Heinoc on 2018/1/31.
 */

fun AppCompatActivity.addFragment(frameId: Int, fragment: Fragment){
    supportFragmentManager.inTransaction { add(frameId, fragment) }
}


fun AppCompatActivity.replaceFragment(frameId: Int, fragment: Fragment) {
    supportFragmentManager.inTransaction{replace(frameId, fragment)}
}

fun AppCompatActivity.hideFragment(fragment: Fragment) {
    supportFragmentManager.inTransaction{hide(fragment)}
}

fun AppCompatActivity.showFragment(fragment: Fragment) {
    supportFragmentManager.inTransaction{show(fragment)}
}

fun Activity.errorToast(message: String) {
    CurtainResearch.create(this)
            .withContent(message)
            .withLevel(CurtainResearch.CurtainLevel.ERROR)
            .show()
}

fun Activity.errorToast(@StringRes stringResId: Int) {
    CurtainResearch.create(this)
            .withContentRes(stringResId)
            .withLevel(CurtainResearch.CurtainLevel.ERROR)
            .show()
}

fun Activity.successToast(message: String) {
    CurtainResearch.create(this)
            .withContent(message)
            .withLevel(CurtainResearch.CurtainLevel.SUCCESS)
            .show()
}

fun Activity.successToast(@StringRes stringResId: Int) {
    CurtainResearch.create(this)
            .withContentRes(stringResId)
            .withLevel(CurtainResearch.CurtainLevel.SUCCESS)
            .show()
}

fun Activity.normalToast(message: String) {
    CurtainResearch.create(this)
            .withContent(message)
            .withLevel(CurtainResearch.CurtainLevel.NORMAL)
            .show()
}

fun Activity.normalToast(@StringRes stringResId: Int) {
    CurtainResearch.create(this)
            .withContentRes(stringResId)
            .withLevel(CurtainResearch.CurtainLevel.NORMAL)
            .show()
}