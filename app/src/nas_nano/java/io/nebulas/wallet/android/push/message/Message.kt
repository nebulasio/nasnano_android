package io.nebulas.wallet.android.push.message

import io.nebulas.wallet.android.push.message.channel.ChannelEnum
import java.io.Serializable

const val MESSAGE_TYPE_TRANSACTION = "TRANSACTION"
const val MESSAGE_TYPE_NOTICE = "NOTICE"

data class Message(
        val channel: ChannelEnum,
        val type: String,
        val data: String
) : Serializable