package io.nebulas.wallet.android.network.parser

import okhttp3.HttpUrl

/**
 * Created by Heinoc on 2018/2/6.
 */

class DefaultUrlParser : UrlParser {
    override fun parseUrl(domainUrl: HttpUrl, url: HttpUrl): HttpUrl {
        if (null == domainUrl) return url
        return url.newBuilder()
                .scheme(domainUrl.scheme())
                .host(domainUrl.host())
                .port(domainUrl.port())
                .build()
    }
}