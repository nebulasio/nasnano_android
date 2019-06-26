package io.nebulas.wallet.android.module.balance.adapter

import android.content.Context
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.alibaba.fastjson.JSON
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.atp.AtpHolder
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.base.BaseBindingViewHolder
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.Constants.voteContracts
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.firebaseAnalytics
import io.nebulas.wallet.android.databinding.ItemBalanceRecyclerviewBinding
import io.nebulas.wallet.android.dialog.NasBottomListDialog
import io.nebulas.wallet.android.module.balance.BalanceFragment
import io.nebulas.wallet.android.module.balance.ScreeningItemHelper
import io.nebulas.wallet.android.module.balance.model.BalanceListModel
import io.nebulas.wallet.android.module.wallet.create.CreateWalletActivity
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.manage.WalletBackupActivity
import org.jetbrains.anko.find
import java.lang.Exception
import java.util.*

/**
 * Created by Heinoc on 2018/2/26.
 */
class BalanceRecyclerViewAdapter(context: Context) : BaseBindingAdapter<BalanceListModel, ItemBalanceRecyclerviewBinding>(context) {

    data class WalletWrapper(val wallet: Wallet) : NasBottomListDialog.ItemWrapper<Wallet> {
        override fun getDisplayText(): String = wallet.walletName
        override fun getOriginObject(): Wallet = wallet
        override fun isShow(): Boolean = false
    }

    private var selectWalletDialog: NasBottomListDialog<Wallet, WalletWrapper>? = null

    class BalanceViewHolder(itemView: View) : BaseBindingViewHolder(itemView) {

        val backupLayout = itemView.find<ConstraintLayout>(R.id.backupLayout)
        val goToCreateBtn = itemView.find<TextView>(R.id.goToCreateBtn)

        val txStatusIconView = itemView.find<LottieAnimationView>(R.id.txStatusIconView)

        val createWalletBtn = itemView.find<TextView>(R.id.createWalletBtn)
        val feedCloseIV = itemView.find<ImageView>(R.id.feedCloseIV)

        val txCloseIV = itemView.find<ImageView>(R.id.txCloseIV)
        val iv_atp_ads = itemView.find<ImageView>(R.id.iv_atp_ads)
        val tv_transaction_time = itemView.find<TextView>(R.id.tv_transaction_time)

    }

    override fun getViewHolder(itemView: View, viewType: Int): BaseBindingViewHolder {
        return BalanceViewHolder(itemView)
    }

    override fun getLayoutResId(viewType: Int): Int {
        return R.layout.item_balance_recyclerview
    }

    override fun onBindItem(binding: ItemBalanceRecyclerviewBinding?, item: BalanceListModel) {

        binding?.item = item

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        if (holder is BalanceViewHolder) {
            /**
             * 动画效果
             */
            val tx = items[position].tx
            if (tx != null) {

                if (AtpHolder.isRenderable(tx.txData)) {
                    firebaseAnalytics.logEvent(Constants.Home_ATPAds_Show, Bundle())
                    holder.iv_atp_ads.visibility = View.VISIBLE
                    holder.tv_transaction_time.gravity = Gravity.BOTTOM
                } else {
                    holder.iv_atp_ads.visibility = View.GONE
                    holder.tv_transaction_time.gravity = Gravity.TOP
                }

                holder.txStatusIconView.rotation = 0F
                if (!tx.confirmed && tx.status != "fail") {
                    holder.txStatusIconView.setAnimation("home_tx_processing.json")
                    holder.txStatusIconView.scale = 0.5F
                    holder.txStatusIconView.repeatMode = LottieDrawable.RESTART
                    holder.txStatusIconView.repeatCount = -1
                    if (items[position].tx?.isSend == true)
                        holder.txStatusIconView.rotation = 180F
                    holder.txStatusIconView.playAnimation()
                }
                if (voteContracts.contains(tx.receiver)) {
                    val data = tx.txData
                    if (!data.isNullOrEmpty()) {
                        try {
                            val txData = String(Base64.decode(data.toByteArray(), Base64.DEFAULT))
                            val json = JSON.parseObject(txData)
                            val function = json.getString("Function")
                            if (function == "vote") {
                                holder.txStatusIconView.rotation = 0F
                                holder.txStatusIconView.setImageResource(R.drawable.ic_nat_vote)
                            }
                        } catch (e: Exception) {

                        }
                    }
                }

            }

            holder.backupLayout.setOnClickListener {

                /**
                 * 检查钱包备份情况
                 */
                var needBackupWallets = DataCenter.wallets.filter { it.isNeedBackup() }

                if (needBackupWallets.isEmpty())
                    return@setOnClickListener
                if (needBackupWallets.size > 1) {
                    val dataSource = MutableList(needBackupWallets.size) {
                        WalletWrapper(needBackupWallets[it])
                    }
                    if (null == selectWalletDialog) {
                        selectWalletDialog = NasBottomListDialog(context = context,
                                title = context.getString(R.string.select_wallet_title),
                                withSelectedStatusIcon = false)
                        selectWalletDialog?.onItemSelectedListener = object : NasBottomListDialog.OnItemSelectedListener<Wallet, WalletWrapper> {
                            override fun onItemSelected(itemWrapper: WalletWrapper) {
                                WalletBackupActivity.launch((context as BaseActivity),
                                        BalanceFragment.REQUEST_CODE_BACKUP_WALLET,
                                        context.getString(R.string.wallet_backup_mnemonic),
                                        itemWrapper.wallet)
                                selectWalletDialog?.dismiss()
                            }
                        }
                    }
                    selectWalletDialog?.dataSourceSetter = dataSource
                    selectWalletDialog?.show()
                } else {
                    WalletBackupActivity.launch((context as BaseActivity),
                            BalanceFragment.REQUEST_CODE_BACKUP_WALLET,
                            context.getString(R.string.wallet_backup_mnemonic),
                            needBackupWallets[0])
                }

                /**
                 * GA
                 */
                (context as BaseActivity).firebaseAnalytics?.logEvent(Constants.kAHomeBackupClick, Bundle())

            }

            holder.goToCreateBtn.setOnClickListener {
                /**
                 * GA
                 */
                CreateWalletActivity.launch(context as BaseActivity, BalanceFragment.REQUEST_CODE_CREATE_WALLET, showBackBtn = true)
            }

            holder.createWalletBtn.setOnClickListener {

                CreateWalletActivity.launch(context as BaseActivity, BalanceFragment.REQUEST_CODE_CREATE_WALLET, showBackBtn = true)
            }


            holder.feedCloseIV.tag = items[position]
            holder.feedCloseIV.setOnClickListener {
                val model = holder.feedCloseIV.tag as BalanceListModel
                val feedItem = model.feedItem
                feedItem?.apply {
                    ScreeningItemHelper.toScreenFeedItem(it.context, this)
                    items.remove(model)
                }
            }

            holder.txCloseIV.tag = items[position]
            holder.txCloseIV.setOnClickListener {
                val model = holder.txCloseIV.tag as BalanceListModel
                val tx = model.tx
                tx?.apply {
                    ScreeningItemHelper.toScreenTransaction(it.context, this)
                    items.remove(model)
                }
            }

        }

    }

}