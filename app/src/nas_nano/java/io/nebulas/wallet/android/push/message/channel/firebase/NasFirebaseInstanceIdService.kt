package io.nebulas.wallet.android.push.message.channel.firebase

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.push.message.PushManager

class NasFirebaseInstanceIdService : FirebaseInstanceIdService() {

//    override fun onMessageReceived(p0: RemoteMessage?) {
//        super.onMessageReceived(p0)
//        p0?:return
//        for((k,v) in p0.data){
//            println("Firebase Data: $k - $v")
//        }
//    }
//
//    override fun onNewToken(p0: String?) {
//        super.onNewToken(p0)
//        println("Firebase Token: $p0")
//        sendRegistrationToServer(p0)
//    }

    override fun onTokenRefresh() {
        val refreshedToken = FirebaseInstanceId.getInstance().token
        logD("PUSH : FCM : onTokenRefresh : $refreshedToken")
        PushManager.updateToken(refreshedToken)
    }

}
