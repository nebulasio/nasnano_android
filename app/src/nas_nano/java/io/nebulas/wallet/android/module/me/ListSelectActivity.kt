package io.nebulas.wallet.android.module.me

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.main.MainActivity
import io.nebulas.wallet.android.module.me.adapter.ListSelectAdapter
import io.nebulas.wallet.android.module.me.model.ListSelectModel
import io.nebulas.wallet.android.push.message.PushManager
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.nas_nano.activity_list_select.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.annotations.NotNull

class ListSelectActivity : BaseActivity() {

    companion object {
        const val LIST_TITLE = "listTitle"
        const val LIST_ITEMS = "listItems"

        /**
         * 启动ListSelectActivity
         *
         * @param context
         * @param requestCode
         * @param title
         */
        fun launch(@NotNull context: Context, requestCode: Int, title: String) {
            (context as AppCompatActivity).startActivityForResult<ListSelectActivity>(requestCode, LIST_TITLE to title)
        }
    }

    lateinit var adapter: ListSelectAdapter

    lateinit var items: List<ListSelectModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_select)
    }

    override fun initView() {
        showBackBtn(true, toolbar)

        titleTV.text = intent.getStringExtra(LIST_TITLE)

        recyclerView.layoutManager = LinearLayoutManager(this)


        adapter = ListSelectAdapter(this)
        recyclerView.adapter = adapter

        if (DataCenter.containsData(LIST_ITEMS)) {
            items = DataCenter.getData(LIST_ITEMS, true) as List<ListSelectModel>
            adapter.items.addAll(items)
        }


        adapter.setOnClickListener(object : BaseBindingAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                adapter.items.forEach {
                    it.selected = false
                }
                adapter.items[position].selected = true
                adapter.notifyDataSetChanged()

                when (titleTV.text.toString()) {
                    getString(R.string.me_language) -> {
                        val language = items[position].value!!
                        if (Constants.LANGUAGE != language) {
                            val oldLang = Util.getCurLanguage()
                            PushManager.unsubscribeTopic(oldLang)
                            Util.setAppLanguage(WalletApplication.INSTANCE, language)
                            val newLang = Util.getCurLanguage()
                            PushManager.subscribeTopic(newLang)
                            MainActivity.launch(this@ListSelectActivity, false)
                        }
                    }

                    getString(R.string.me_token_setting) -> {
                        Constants.CURRENCY_SYMBOL_NAME = items[position].value!!
                        Util.setAppCurrencySymbol(WalletApplication.INSTANCE, Constants.CURRENCY_SYMBOL_NAME)
                    }
                }

            }

            override fun onItemLongClick(view: View, position: Int) {

            }
        })

    }


}
