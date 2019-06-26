package io.nebulas.wallet.android.module.feedback

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import io.nebulas.wallet.android.base.BaseActivity
import org.jetbrains.annotations.NotNull
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.util.FileTools
import io.nebulas.wallet.android.util.PhotoChooseUtil
import kotlinx.android.synthetic.nas_nano.activity_feedback.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import org.jetbrains.anko.startActivity
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.text.TextUtils
import android.widget.Toast
import io.nebulas.wallet.android.app.WalletApplication
import java.io.File
import java.io.IOException


class FeedBackActivity : BaseActivity() {
    companion object {
        fun launch(@NotNull context: Context) {
            context.startActivity<FeedBackActivity>()
        }

        const val REQUEST_CODE_ALBUM = 10001
        const val REQUEST_CODE_CAMERA_PERMISSION = 10002
        const val REQUEST_CODE_STORAGE_PERMISSION = 10003
        const val REQUEST_CODE_CAPTURE = 10004
    }

    val cameraPermission: Array<String> = arrayOf(android.Manifest.permission.CAMERA)
    val writeExternalStoragePermission: Array<String> = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    val picBitmap: ArrayList<Bitmap> = arrayListOf<Bitmap>()
    var mFile: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)
    }

    override fun initView() {
        showBackBtn(true, toolbar)
        titleTV.text = getString(R.string.feed_back)
        setImage()
        questionDes.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                word_num.text = p0!!.length.toString() + "/200"
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }
        })

        submitBtn.setOnClickListener {
            if(TextUtils.isEmpty(questionDes.text)||TextUtils.isEmpty(email_ev.text) ){
                toastErrorMessage(R.string.feedback_not_fill)
                return@setOnClickListener
            }
            picBitmap
        }
    }


    private fun choosePicFromAlbum() {
        if (!checkPermissions(writeExternalStoragePermission))
            requestPermission(writeExternalStoragePermission, REQUEST_CODE_STORAGE_PERMISSION)
        else
            getPic()
    }

    private fun getPic() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_ALBUM)
    }

    private fun toTakePhoto(){
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            mFile = File(Environment.getExternalStorageDirectory(), "feedback")
        } else {
            val toast = Toast.makeText(this, null, Toast.LENGTH_SHORT)
            toast.setText("请插入sd卡")
            toast.show()
            return
        }

        try {
            mFile?.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val intent = Intent()
        intent.action = MediaStore.ACTION_IMAGE_CAPTURE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {   //如果在Android7.0以上,使用FileProvider获取Uri
            intent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val applicationID = WalletApplication.INSTANCE.applicationInfo.packageName
            val contentUri: Uri = FileProvider.getUriForFile(this, "$applicationID.update.FileProvider", mFile!!);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri)
        } else {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mFile))
        }
        startActivityForResult(intent, REQUEST_CODE_CAPTURE)
    }
    private fun takePhoto() {
        if (!checkPermissions(cameraPermission)) {
            requestPermission(cameraPermission, REQUEST_CODE_CAMERA_PERMISSION)
        } else {
            toTakePhoto()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        when (requestCode) {
            REQUEST_CODE_ALBUM -> {
                val uri = data!!.data
                picBitmap.add(PhotoChooseUtil.Companion.instance.compressPicture(FileTools.getPath(this, uri)!!))
                setImage()
            }
            REQUEST_CODE_CAPTURE -> {
                picBitmap.add(PhotoChooseUtil.Companion.instance.compressPicture(mFile?.absolutePath!!))
                setImage()
            }
        }
    }

    private fun setImage() {
        val size = picBitmap.size
        when (size) {
            0 -> {
                image_iv1.setImageResource(R.drawable.uploadpic)
                image_iv1_tag.visibility = View.GONE
                image_iv2_layout.visibility = View.GONE
                image_iv3_layout.visibility = View.GONE
            }

            1 -> {
                image_iv1.setImageBitmap(picBitmap.get(index = 0))
                image_iv1_tag.visibility = View.VISIBLE
                image_iv2_layout.visibility = View.VISIBLE
                image_iv2.setImageResource(R.drawable.uploadpic)
                image_iv2_tag.visibility = View.GONE
                image_iv3_layout.visibility = View.GONE
            }

            2 -> {
                image_iv1.setImageBitmap(picBitmap.get(index = 0))
                image_iv1_tag.visibility = View.VISIBLE
                image_iv2_layout.visibility = View.VISIBLE
                image_iv2.setImageBitmap(picBitmap.get(index = 1))
                image_iv2_tag.visibility = View.VISIBLE
                image_iv3.setImageResource(R.drawable.uploadpic)
                image_iv3_tag.visibility = View.GONE
                image_iv3_layout.visibility = View.VISIBLE
            }
            3 -> {
                image_iv1.setImageBitmap(picBitmap.get(index = 0))
                image_iv1_tag.visibility = View.VISIBLE
                image_iv2_layout.visibility = View.VISIBLE
                image_iv2.setImageBitmap(picBitmap.get(index = 1))
                image_iv2_tag.visibility = View.VISIBLE
                image_iv3.setImageBitmap(picBitmap.get(index = 2))
                image_iv3_tag.visibility = View.VISIBLE
                image_iv3_layout.visibility = View.VISIBLE
            }
        }
    }


    fun picDelete(v: View?) {
        var tempBitmap: Bitmap? = null
        when (v?.id) {
            R.id.image_iv1_tag -> {
                tempBitmap = picBitmap.get(index = 0)

            }
            R.id.image_iv2_tag -> {
                tempBitmap = picBitmap.get(index = 1)

            }
            R.id.image_iv3_tag -> {
                tempBitmap = picBitmap.get(index = 2)

            }

        }
        if (tempBitmap == null) {
            return
        }
        picBitmap.remove(tempBitmap)
        if (!tempBitmap!!.isRecycled) {
            tempBitmap.recycle()
        }
        setImage()
    }

    fun showDialog(v: View?) {
        when (v?.id) {
            R.id.image_iv1 -> {
                if (picBitmap.size == 0)
                    PhotoChooseUtil.instance.showDilag(this, { takePhoto() }, { choosePicFromAlbum() })

            }
            R.id.image_iv2 -> {
                if (picBitmap.size == 1)
                    PhotoChooseUtil.instance.showDilag(this, { takePhoto() }, { choosePicFromAlbum() })
            }
            R.id.image_iv3 -> {
                if (picBitmap.size == 2)
                    PhotoChooseUtil.instance.showDilag(this, { takePhoto() }, { choosePicFromAlbum() })
            }

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (verifyPermissions(grantResults)) {
            //权限开启成功
            when (requestCode) {
                REQUEST_CODE_CAMERA_PERMISSION -> {
                    toTakePhoto()
                }

                REQUEST_CODE_STORAGE_PERMISSION -> {
                    getPic()
                }
            }
        } else {
            //权限开启失败
            showPermissionTips()
        }
    }
}