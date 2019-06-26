package io.nebulas.wallet.android.push.message.channel.mi

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import io.nebulas.wallet.android.push.message.PushManager

class PermissionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 23) {
            val intent = intent
            val permissions = intent.getStringArrayExtra("permissions")
            for (i in permissions.indices) {
                if (checkSelfPermission(permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(permissions, PERMISSION_REQUEST)
                    break
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST) {
            var granted = false
            for (i in grantResults.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    granted = true
                }
            }

            if (granted) {
                PushManager.reInit()
            }
            finish()
        }
    }

    companion object {
        private val PERMISSION_REQUEST = 1
    }
}
