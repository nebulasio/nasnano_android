package io.nebulas.wallet.android.update

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Created by young on 2018/6/1.
 *
 * Retrofit API interface for download file
 */
interface DownloadAPI {

    /**
     * @param url   The whole url for the file. For example: http://host/path/file.xxx
     */
    @GET
    @Streaming
    fun download(@Url url:String): Call<ResponseBody>

}