package io.nebulas.wallet.android.module.setting.model

import io.nebulas.wallet.android.module.wallet.create.model.Wallet

/**
 * Created by Alina on 2018/6/22
 */
data class DefaultWalletListModel(val wallet:Wallet,var selected:Boolean)