package io.nebulas.wallet.android.module.detail.fragment.transaction

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.annotation.DrawableRes
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.young.binder.*
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.atp.AtpHolder
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.firebaseAnalytics
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.detail.BalanceDetailActivity
import io.nebulas.wallet.android.module.transaction.TxDetailActivity
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.util.Converter
import io.nebulas.wallet.android.view.LineDecoration
import kotlinx.android.synthetic.nas_nano.fragment_transaction_list_for_token_detail.view.*
import org.jetbrains.anko.dip
import org.jetbrains.anko.findOptional

/**
 * Created by young on 2018/6/27.
 */
class TransactionListViewBinder(val context: Activity) : NormalBinder<TransactionListController, TransactionListDataCenter> {

    private lateinit var adapter: TransactionAdapter
    private lateinit var controller: TransactionListController
    private lateinit var dataCenter: TransactionListDataCenter

    private val layoutManager = LinearLayoutManager(context)
    private var lastVisibleItem: Int = 0

    override fun bind(view: View, controller: TransactionListController, dataCenter: TransactionListDataCenter) {
        this.controller = controller
        this.dataCenter = dataCenter
        adapter = TransactionAdapter()
        view.recyclerViewForTransactions.layoutManager = layoutManager
        view.recyclerViewForTransactions.adapter = adapter
        view.recyclerViewForTransactions.addItemDecoration(LineDecoration(context.resources.getColor(R.color.divider_line), context.dip(1), context.dip(18)))

        view.recyclerViewForTransactions.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                if (dataCenter.hasMore) {
                    if (lastVisibleItem >= adapter.itemCount - 1 && dataCenter.loadingStatus == TransactionListDataCenter.LOADING_STATUS_NONE) {
                        controller.loadData()
                    }
                }
            }
        })


        adapter.bind(dataCenter, TransactionListEvents.event_data_source_updated, false) {
            notifyDataSetChanged()
        }

        view.swipeRefreshLayout.bind(dataCenter, TransactionListEvents.event_loading_status_changed) {
            isRefreshing = dataCenter.loadingStatus == TransactionListDataCenter.LOADING_STATUS_REFRESH
            isEnabled = dataCenter.loadingStatus != TransactionListDataCenter.LOADING_STATUS_LOAD_MORE
        }

        view.swipeRefreshLayout.setOnRefreshListener {
            controller.refresh()
        }

        dataCenter.whenEvent(TransactionListEvents.event_data_source_updated) {
            if (dataCenter.dataSource.isEmpty()) {
                view.layoutEmpty.visibility = View.VISIBLE
            } else {
                view.layoutEmpty.visibility = View.GONE
            }
        }
    }

    @DrawableRes
    private fun getStatusIconRes(transaction: Transaction): Int {
        return if (transaction.status == "fail") {
            R.drawable.home_icon_feed_tx_fail
        } else {
            if (transaction.isSend) {
                R.drawable.home_icon_feed_send
            } else {
                R.drawable.home_icon_feed_receive
            }
        }
    }

    @ColorInt
    private fun getValueTextColor(transaction: Transaction): Int {
        return if (transaction.status == "fail") {
            context.resources.getColor(R.color.color_FF5552)
        } else if (transaction.confirmed) {
            if (transaction.isSend) {
                context.resources.getColor(R.color.color_202020)
            } else {
                context.resources.getColor(R.color.color_038AFB)
            }
        } else {
            context.resources.getColor(R.color.color_FF8F00)
        }
    }

    private fun getFormattedDate(time: Long): String {
        if (time <= 0) {
            return ""
        }
        return DateFormat.format("MM/dd kk:mm", time).toString()
    }

    private fun onItemClicked(index: Int) {
        if (index >= dataCenter.dataSource.size) {
            return
        }
        val t = dataCenter.dataSource[index]
        if (AtpHolder.isRenderable(t.txData)) {
            val address = if (t.isSend) {
                t.sender?:""
            } else {
                t.receiver?:""
            }
            firebaseAnalytics.logEvent(Constants.TxRecord_ATPAds_Click, Bundle())
            AtpHolder.route(context, t.txData, address)
            return
        }
        val coin = if (DataCenter.containsData(BalanceDetailActivity.BALANCE_DETAIL_COIN))
            DataCenter.getData(BalanceDetailActivity.BALANCE_DETAIL_COIN, false) as Coin
        else
            DataCenter.coinsGroupByCoinSymbol[0]

        TxDetailActivity.launch(context, coin, t)
    }


    open class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivTransactionStatus: LottieAnimationView? = itemView.findOptional(R.id.ivTransactionStatus)
        val tvTransactionTarget: TextView? = itemView.findOptional(R.id.tvTransactionTarget)
        val tvTransactionValue: TextView? = itemView.findOptional(R.id.tvTransactionValue)
        val tvTransactionTime: TextView? = itemView.findOptional(R.id.tvTransactionTime)
        val tvTransactionWallet: TextView? = itemView.findOptional(R.id.tvTransactionWallet)
        val ivAtpAds: ImageView? = itemView.findOptional(R.id.ivAtpAds)
    }

    class VHLoading(itemView: View) : VH(itemView)

    inner class TransactionAdapter : RecyclerView.Adapter<VH>(), View.OnClickListener {

        private val viewTypeItem = 0
        private val viewTypeLoading = 1

        override fun onClick(v: View) {
            onItemClicked(v.tag as Int)
        }

        override fun getItemViewType(position: Int): Int {
            if (dataCenter.hasMore) {
                if (position == dataCenter.dataSource.size) {
                    return viewTypeLoading
                }
            }
            return viewTypeItem
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return if (viewType == viewTypeItem) {
                VH(LayoutInflater.from(context).inflate(R.layout.item_transaction_list_for_token_detail, parent, false).apply {
                    setOnClickListener(this@TransactionAdapter)
                })
            } else {
                val layout = RelativeLayout(context)
                val vPadding = context.dip(12)
                val loadingView = LayoutInflater.from(context).inflate(R.layout.loading_view, layout, false)
                layout.setPadding(0, vPadding, 0, vPadding)
                layout.addView(loadingView, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                    addRule(RelativeLayout.CENTER_IN_PARENT)
                })
                layout.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                VHLoading(layout)
            }
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            if (holder is VHLoading) {
                holder.itemView.layoutParams?.apply {
                    width = MATCH_PARENT
                    height = WRAP_CONTENT
                }
                return
            }
            val t = dataCenter.dataSource[position]
            holder.ivTransactionStatus?.rotation = 0f
            if (!t.confirmed && t.status != "fail") {
                if (t.isSend) {
                    holder.ivTransactionStatus?.rotation = 180f
                }
                holder.ivTransactionStatus?.setAnimation("home_tx_processing.json")
                holder.ivTransactionStatus?.playAnimation()
            } else {
                holder.ivTransactionStatus?.cancelAnimation()
                holder.ivTransactionStatus?.setImageResource(getStatusIconRes(t))
            }
            if (AtpHolder.isRenderable(t.txData)) {
                firebaseAnalytics.logEvent(Constants.TxRecord_ATPAds_Show, Bundle())
                holder.ivAtpAds?.visibility = View.VISIBLE
                holder.tvTransactionValue?.visibility = View.GONE
            } else {
                holder.ivAtpAds?.visibility = View.GONE
                holder.tvTransactionValue?.visibility = View.VISIBLE
                holder.tvTransactionValue?.setTextColor(getValueTextColor(t))
                holder.tvTransactionValue?.text = if (t.isSend) "-" + t.amountString else "+" + t.amountString
            }

            holder.tvTransactionTarget?.text = if (t.status == "fail") context.getString(R.string.home_tx_failed_des) else if (t.isSend) t.receiver else t.sender
            holder.tvTransactionTarget?.setTextColor(if (t.status == "fail") context.resources.getColor(R.color.color_FF5552) else context.resources.getColor(R.color.color_202020))
            Converter.dateMMddTime(holder.tvTransactionTime, t.sendTimestamp ?: 0)
            holder.tvTransactionWallet?.text = t.wallet?.walletName
            holder.itemView.tag = position
        }

        override fun getItemCount(): Int {
            if (dataCenter.dataSource.size == 0) {
                return 0
            }
            return dataCenter.dataSource.size + (if (dataCenter.hasMore) 1 else 0)  //用于判断是否增加底部loading view
        }

    }

}