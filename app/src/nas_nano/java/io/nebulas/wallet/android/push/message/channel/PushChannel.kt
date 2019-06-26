package io.nebulas.wallet.android.push.message.channel

import android.app.Application

interface PushChannel {

    fun init(application: Application)

    fun getChannelEnum():ChannelEnum

    fun getToken():String

    fun subscribeTopic(application: Application, topic: String, what: String?=null)

    fun unsubscribeTopic(application: Application, topic: String, what: String?=null)
}