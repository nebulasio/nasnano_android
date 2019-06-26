package io.nebulas.wallet.android.module.swap.detail

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.module.swap.model.ExchangeRecordModel
import io.nebulas.wallet.android.module.swap.detail.adapter.ExchangeRecordsAdapter
import io.nebulas.wallet.android.module.swap.viewmodel.SwapTransferViewModel
import kotlinx.android.synthetic.nas_nano.activity_exchange_records.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.annotations.NotNull

class ExchangeRecordsActivity : BaseActivity() {
    companion object {
        const val ETH_ADDRESS = "eth_address"
        fun launch(@NotNull context: Context, requestCode: Int = 7001, ethAddress: String?) {
            (context as AppCompatActivity).startActivityForResult<ExchangeRecordsActivity>(requestCode,  ETH_ADDRESS to ethAddress)
        }
    }

    lateinit var swapTransferViewModel: SwapTransferViewModel
    private var currentPage = 1
    private val pageSize = 10
    private lateinit var adapter: ExchangeRecordsAdapter
    private var lastVisibleItem = 0
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var hasMore = false
    private var isLoading = false
    private var ethAddress: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exchange_records)
    }

    override fun initView() {
        swapTransferViewModel = ViewModelProviders.of(this).get(SwapTransferViewModel::class.java)
        showBackBtn(true, toolbar = toolbar)
        titleTV.text = getString(R.string.exchange_records_title)
        ethAddress = intent.getStringExtra(ETH_ADDRESS)
        adapter = ExchangeRecordsAdapter(this)
        linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = adapter
        swipeRefreshLayout.setOnRefreshListener {
            if (isLoading) {
                return@setOnRefreshListener
            }
            currentPage = 1
            loadData()
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (hasMore && (!isLoading) && adapter.itemCount - 1 == lastVisibleItem) {
                    currentPage++
                    loadData()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition()
            }
        })


    }

    override fun onResume() {
        super.onResume()
        loadData()

    }

    private fun loadData() {
        isHasNetWork()
        isLoading = true
        swipeRefreshLayout.isRefreshing = true
        val list: MutableList<ExchangeRecordModel> = mutableListOf()
        if (ethAddress.isNullOrEmpty()) {
            list.add(ExchangeRecordModel(emptyView = true))
            setData(list)
            return
        }
        swapTransferViewModel.getSwapTransferList(ethAddress!!, currentPage, pageSize, lifecycle, {
            list.addAll(it)
            setData(list)
            hasMore = list.size == pageSize
        }, {
            if (currentPage == 1) {
                list.add(ExchangeRecordModel(emptyView = true))
                hasMore = false
            }
            setData(list)
        })
    }

    private fun setData(list: MutableList<ExchangeRecordModel>) {
        swipeRefreshLayout.isRefreshing = false
        isLoading = false
        adapter.items.removeAll { it.emptyView }
        if (currentPage == 1) {
            adapter.items.clear()
        }
        adapter.items.removeAll { it.hasMore }
        adapter.items.addAll(list)
    }
}