package io.nebulas.wallet.android.network.cache

import android.util.Log
import io.nebulas.wallet.android.common.URLConstants
import okhttp3.*
import okio.Buffer
import java.io.IOException
import java.lang.Exception
import java.nio.charset.Charset


/**
 * 缓存处理拦截器，支持POST请求
 * 对于POST请求，将POST请求的url和params拼接在一起作为key对请求结果进行缓存
 *
 * Created by Heinoc on 2018/7/25.
 */
class CacheInterceptor : Interceptor {

    object CacheType {
        val DISK_CACHE = "from disk cache"   //从硬盘缓存取值
        val MEMORY_CACHE = "from memory cache" //从内存缓存取值
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        try {

            val response = chain.proceed(request)

            //如果请求成功，对返回值进行缓存处理
            if (response.isSuccessful) {
                val requestBody = request.body()
                var charset = Charset.forName("UTF-8")
                /**
                 * post请求处理
                 */
                if (request.method().equals("POST")) {
                    val contentType = requestBody?.contentType()
                    if (contentType != null) {
                        charset = contentType.charset(Charset.forName("UTF-8"))
                    }

                }

                val responseBody = response.body()
                val contentType = responseBody?.contentType()

                val source = responseBody?.source()
                source?.request(java.lang.Long.MAX_VALUE)
                val buffer = source?.buffer()

                if (contentType != null) {
                    charset = contentType.charset(Charset.forName("UTF-8"))
                }
                //服务器返回的json原始数据
                val json = buffer?.clone()?.readString(charset) ?: ""

                CacheManager.instance.putCache(getCacheKeyOfRequest(request), json)

            }

            return response

        } catch (e: Exception) {

            return getCacheResponse(request) ?: chain.proceed(request)

        }

    }

    fun getCacheKeyOfRequest(request: Request): String {

        val url = request.url().toString()
        val requestBody = request.body()
        var charset = Charset.forName("UTF-8")
        val sb = StringBuilder()
        if(isBlurryCacheKey(url)){
            sb.append(doCacheKey(url))
        }else {
            sb.append(url)
        }
        /**
         * post请求处理
         */
        if (request.method() == "POST" && (!isBlurryCacheKey(url))) {
            val contentType = requestBody?.contentType()
            if (contentType != null) {
                charset = contentType.charset(Charset.forName("UTF-8"))
            }
            val buffer = Buffer()
            try {
                requestBody?.writeTo(buffer)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            sb.append(buffer.readString(charset))
            buffer.close()
        }
        Log.d("getCacheKeyOfRequest", sb.toString())
        return CacheManager.encryptMD5(sb.toString())
    }

    /**
     * 部分可以模糊匹配的cache key
     */
    private fun isBlurryCacheKey(url: String): Boolean {
        if (url.contains(URLConstants.BALANCE) || url.contains(URLConstants.FEED_FLOW) || url.contains(URLConstants.SWAP_ENTRANCE) || url.contains(URLConstants.TX + "?")) {
            return true
        }
        return false
    }

    private fun doCacheKey(url: String): String {
        if (url.contains(URLConstants.BALANCE) || url.contains(URLConstants.FEED_FLOW) || url.contains(URLConstants.SWAP_ENTRANCE)) {
            return url
        } else if (url.contains(URLConstants.TX + "?")) {
            val sb = StringBuilder()
            var list: List<String> = url.split("&")
            for (str in list) {
                if (str.contains("address")) {
                    sb.append(str.substringBefore("address"))
                } else {
                    sb.append(str)
                }
            }
            return sb.toString()
        }
        return url
    }

    private fun getCacheResponse(request: Request): Response? {
        val cacheStr = CacheManager.instance.getCache(getCacheKeyOfRequest(request))
        return if (cacheStr?.isNotEmpty() == true) {
            Response.Builder()
                    .code(200)
                    .body(ResponseBody.create(null, cacheStr))
                    .request(request)
                    .message(CacheType.DISK_CACHE)
                    .protocol(Protocol.HTTP_1_0)
                    .build()


        } else {
            null
        }
    }

}