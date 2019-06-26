package io.nebulas.wallet.android.extensions

import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction

/**
 * Created by Heinoc on 2018/1/31.
 */

inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
    //transaction.commit();-->出现了这个错误 IllegalStateException: Can not perform this action after onSaveInstanceState
    beginTransaction().func().commitAllowingStateLoss()
}