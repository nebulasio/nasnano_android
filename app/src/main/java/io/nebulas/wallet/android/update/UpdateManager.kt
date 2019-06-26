package io.nebulas.wallet.android.update

import android.content.Context
import io.nebulas.wallet.android.util.FileTools
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe
import io.reactivex.FlowableSubscriber
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.reactivestreams.Subscription
import retrofit2.Retrofit
import java.io.File

/**
 * Created by young on 2018/6/1.
 *
 * Manager for app update.
 * @see update
 */
class UpdateManager {

    interface UpdateListener {
        fun onStart()
        fun onProgress(percent: Int)
        fun onSuccess(localPath: String)
        fun onFailure(throwable: Throwable?)
    }

    private val filePath = "update"
    private val fileName = "nabulas.apk"
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder().baseUrl("http://localhost").build()
    }

    /**
     * Entrance of Manager
     * @param context   上下文
     * @param url   下载链接
     * @param listener  下载监听
     */
    fun update(context: Context, url: String, targetMD5: String, listener: UpdateListener?) {
        val api: DownloadAPI = retrofit.create(DownloadAPI::class.java)
        val dir = FileTools.getDiskCacheDir(context, filePath)
        val apkFile = File(dir.absolutePath.plus(File.separator).plus(fileName))
        if (apkFile.exists() && md5Check(apkFile, "")){
            InstallManager.install(context, filePath)
            return
        }
        var subscription: Subscription? = null
        listener?.onStart()
        Flowable.create(FlowableOnSubscribe<ResponseBody> { emitter ->
            val body = api.download(url).execute().body()
            if (body != null) {
                emitter.onNext(body)
                emitter.onComplete()
            } else {
                emitter.onError(IllegalArgumentException("ResponseBody is null"))
            }
        }, BackpressureStrategy.BUFFER)
                .subscribeOn(Schedulers.io())
                .flatMap { body ->
                    Flowable.create(FlowableOnSubscribe<Int> {
                        val totalLength = body.contentLength()
                        if (apkFile.exists()) {
                            apkFile.delete()
                        }
                        apkFile.createNewFile()
                        val inputSteam = body.byteStream()
                        val outputSteam = apkFile.outputStream()
                        try {
                            val byteArray = ByteArray(4096)
                            var downloadedLength = 0L
                            var t = 0
                            while (t != -1) {
                                t = inputSteam.read(byteArray)
                                if (t != -1) {
                                    outputSteam.write(byteArray, 0, t)
                                    downloadedLength += t
                                    val progress = (downloadedLength.toDouble() / totalLength.toDouble() * 100).toInt()
                                    it.onNext(progress)
                                    if (downloadedLength == totalLength) {
                                        it.onComplete()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            it.onError(e)
                        } finally {
                            inputSteam.close()
                            outputSteam.close()
                        }
                    }, BackpressureStrategy.DROP)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : FlowableSubscriber<Int> {
                    override fun onError(t: Throwable?) {
                        subscription?.cancel()
                        listener?.onFailure(t)
                    }

                    override fun onComplete() {
                        subscription?.cancel()
                        listener?.onSuccess(apkFile.absolutePath)
                    }

                    override fun onSubscribe(s: Subscription) {
                        subscription = s
                        s.request(Long.MAX_VALUE)
                    }

                    override fun onNext(t: Int?) {
                        t?.apply {
                            listener?.onProgress(t)
                        }
                    }

                })
    }

    private fun md5Check(file:File, targetMD5:String):Boolean{
        if (targetMD5.isEmpty())
            return false
        val fileMD5 = FileTools.getFileMD5(file)
        return fileMD5==targetMD5
    }

}