package io.nebulas.wallet.android.common

import android.os.Handler
import android.os.Looper
import com.google.firebase.analytics.FirebaseAnalytics
import io.nebulas.wallet.android.app.WalletApplication

/**
 * Created by young on 2018/6/26.
 */

val mainHandler = Handler(Looper.getMainLooper())

val firebaseAnalytics:FirebaseAnalytics by lazy {
    FirebaseAnalytics.getInstance(WalletApplication.INSTANCE)
}