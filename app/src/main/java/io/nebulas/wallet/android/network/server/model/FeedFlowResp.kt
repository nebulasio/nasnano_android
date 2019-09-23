package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.module.transaction.model.Transaction

/**
 * Created by Heinoc on 2018/5/8.
 */
class FeedFlowResp : BaseEntity() {
    /**
     * 通知
     */
    var noticeList: MutableList<HomeFeedItem>? = null
    /**
     * feed流列表
     */
    var flowList: MutableList<HomeFeedItem>? = null
    /**
     * tx交易信息
     */
    var txList: MutableList<Transaction>? = null

    /**
     * 钱包配置项
     */
    var configurations: Map<String, String>? = null
}

class HomeFeedItem : BaseEntity() {
    var id: Int = 0
    /**
     * 创建时间
     */
    var createdAt: Long = 0
    /**
     * 各模块颜色配置。预留字段...
     */
    var color: String = ""
    /**
     * feed流类型。1-通知；2-广告；3-交易记录
     */
    var feedType: Int = 1
    /**
     * 图片地址
     */
    var imageUrl: String = ""
    /**
     * 描述
     */
    var description: String = ""
    /**
     * 超链接名
     */
    var hrefName: String = ""
    /**
     * 超链接地址
     */
    var href: String = ""
    /**
     * 优先级大小，1-10。1-最大，10-最小
     */
    var priority: Int = 1
    /**
     * 标题
     */
    var title: String = ""
}