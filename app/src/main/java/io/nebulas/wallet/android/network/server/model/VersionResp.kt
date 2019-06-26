package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by Heinoc on 2018/4/9.
 */
class VersionResp : BaseEntity() {

    companion object {
        /**
         * 非强制更新
         */
        const val UPDATE_TYPE_NORMAL = 0
        /**
         * 强制更新
         */
        const val UPDATE_TYPE_FORCE = 1
        /**
         * 无更新
         */
        const val UPDATE_TYPE_NONE = 2
    }

    var comment: String? = null
    var versionName: String? = null
    var url: String? = null
    var versionCode: String? = null
    var updateType: Int = UPDATE_TYPE_NONE
}