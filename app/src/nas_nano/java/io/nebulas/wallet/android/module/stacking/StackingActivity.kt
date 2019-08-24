package io.nebulas.wallet.android.module.stacking

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.qrscan.QRScanActivity
import io.nebulas.wallet.android.module.transaction.transfer.TransferActivity
import kotlinx.android.synthetic.nas_nano.activity_stacking.*

class StackingActivity : BaseActivity() {

    companion object {
        fun launch(context: Context){
            context.startActivity(Intent(context, StackingActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stacking)
    }

    override fun initView() {
        showBackBtn(toolbar = toolbar)
        tvTitle.text = "质押"
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = StackingAdapter(DataCenter.wallets)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.qr_scan_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?:return false
        when (item.itemId) {
            R.id.actionScanQR -> {
                QRScanActivity.launch(this, TransferActivity.REQUEST_CODE_SCAN_QRCODE)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
