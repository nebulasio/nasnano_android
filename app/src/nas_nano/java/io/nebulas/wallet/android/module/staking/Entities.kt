package io.nebulas.wallet.android.module.staking

import io.nebulas.wallet.android.base.BaseEntity

class Info: BaseEntity() {
    var value: String? = null
    var t: String? = null
}

class PledgeDetail: BaseEntity() {
    var address: String? = null
    var info: Info? = null
}


class StageInfo: BaseEntity() {
    var pledged_nas: String? = null
    var total_nas: String? = null
    var estimate_nax: String? = null
    var last_destroyed_nax: String? = null
    var last_distributed_nax: String? = null
}

class ProfitsInfo: BaseEntity() {
    var last_profits: Map<String, String>? = null
    var total_profits: String? = null
    var last_total_profits: String? = null
}

class StakingSummary: BaseEntity() {
    var stage: StageInfo? = null
    var profits: ProfitsInfo? = null
}


class ProfitRecord: BaseEntity() {
    var profit: String? = null
    var stage: Int = -1
    var timestamp: Long = 0
}

class AddressProfits: BaseEntity() {
    var total_profits: String? = null
    var total_count: Int = 0
    var total_page: Int = 0
    var current_page: Int = 0
    var page_size: Int = 0
    var profits: List<ProfitRecord>? = null
}