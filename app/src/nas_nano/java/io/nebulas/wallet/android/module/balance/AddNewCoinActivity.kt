package io.nebulas.wallet.android.module.balance

import android.content.Context
import android.os.Bundle
import android.view.Menu
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import kotlinx.android.synthetic.nas_nano.activity_add_new_coin.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import org.jetbrains.anko.startActivity
import org.jetbrains.annotations.NotNull

/**
 * Created by Heinoc on 2018/2/2.
 */

class AddNewCoinActivity : BaseActivity() {

    companion object {
        /**
         * 启动AddNewCoinActivity
         *
         * @param context
         */
        fun launch(@NotNull context: Context) {
            context.startActivity<AddNewCoinActivity>()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_new_coin)
    }

    override fun initView() {
        showBackBtn(true, toolbar)
        titleTV.setText(R.string.add_coin_title)

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
        }


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.clear()
        menuInflater.inflate(R.menu.add_new_coin_menu, menu)

        return super.onCreateOptionsMenu(menu)

    }

}
