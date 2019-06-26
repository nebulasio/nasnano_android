package io.nebulas.wallet.android.module.wallet.create

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import kotlinx.android.synthetic.nas_nano.activity_key_store_coin_list.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import walletcore.Walletcore

class KeyStoreCoinListActivity : BaseActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_store_coin_list)
    }

    override fun initView() {
        showBackBtn(true, toolbar)

        titleTV.setText(R.string.import_from_keystore_title)


        nasLayout.setOnClickListener {
            ImportFromKeyStoreActivity.launch(this,
                    CreateWalletActivity.IMPORT_WALLET_REQUEST_CODE,
                    Walletcore.NAS)
        }

        ethLayout.setOnClickListener {
            ImportFromKeyStoreActivity.launch(this,
                    CreateWalletActivity.IMPORT_WALLET_REQUEST_CODE,
                    Walletcore.ETH)
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Activity.RESULT_OK == resultCode) {
            setResult(resultCode, data)
            finish()
        }
    }

}
