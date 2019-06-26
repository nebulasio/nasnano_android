package io.nebulas.wallet.android.network.parser

import okhttp3.HttpUrl

/**
 * Created by Heinoc on 2018/2/6.
 */

interface UrlParser {
    abstract fun parseUrl(domainUrl: HttpUrl, url: HttpUrl): HttpUrl
}