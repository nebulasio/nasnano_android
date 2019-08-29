package io.nebulas.wallet.android.common

/**
 * Created by Heinoc on 2018/2/6.
 */

object URLConstants {

    /**
     * 用户隐私协议
     */
    const val PRIVACY_URL = "https://nano.nebulas.io/wap/privacy_policy_en.html"

    /**
     * 用户隐私协议和服务协议链接
     */
    const val PRIVACY_TERMS_URL = "https://nano.nebulas.io/wap/agreement.html"
    /**
     * 服务协议
     */
    const val TERMS_URL_EN = "https://nano.nebulas.io/wap/terms_of_service_en.html"
    /**
     * test-server url host
     */
    const val URL_HOST = "http://52.53.225.118:8090/"
//    /**
//     * server url host
//     */
//    const val URL_HOST = "https://walletapi.nebulas.io/"

    /**
     * 获取应用版本信息
     */
    const val VERSION = "api/v1/config/version"

    /**
     * 首页feed流
     */
    const val FEED_FLOW = "api/v1/feedflow/active"

    /**
     * 获取balance
     */
    const val BALANCE = "api/v1/account/balance"

    /**
     * 获取币列表
     */
    const val CURRENCY = "api/v1/currency"

    /**
     * 获取币rpc节点
     */
    const val CURRENCY_COIN = "api/v1/currency/coin"

    /**
     * currency price
     */
    const val CURRENCY_PRICE = "api/v1/currency/price"

    /**
     * 获取transactioin交易记录
     */
    const val TX = "api/v1/tx"
    /**
     * gas price
     */
    const val GAS_PRICE = "api/v1/tx/gasprice"

    const val EXPLORER_TX_DETAIL_PREFIX = "https://explorer.nebulas.io/#/tx/"
    const val EXPLORER_TX_DETAIL_PREFIX_TEST_NET = "https://explorer.nebulas.io/#/testnet/tx/"
    const val TX_WALLET = "api/v1/tx/wallet"


    /**
     * 首页换币入口
     */
    const val SWAP_ENTRANCE = "api/v2/transfer/config"

    /**
     * 换币详情
     */
    const val SWAP_TRANSFER_DETAIL = "api/v2/account/transfer/hash"

    /**
     * 换币列表
     */
    const val SWAP_TRANSFER_LIST = "api/v2/account/transfer/list"

    /**
     * 绑定换币地址
     */
    const val BIND_SWAP_ADDRESS = "api/v2/account/transfer/address"

    /**
     * 客户端发起交易后，上传交易记录（hash等交易信息）
     */
    const val SWAP_HASH_UPLOAD = "api/v2/transfer/upload"

    /**
     * 更新设备信息
     */
    const val URL_UPDATE_DEVICE_INFO = "api/v1/device/update"
    /**
     * 推送通知开关（目前只处理交易推送）
     */
    const val URL_NOTIFICATION_SWITCH = "api/v1/device/switch"
    /**
     * 获取staking相关合约地址
     */
    const val URL_GET_STAKING_CONTRACTS = "api/v1/nax/contract"
    /**
     * 获取NAX汇总信息
     */
    const val URL_GET_STAKING_SUMMARY = "api/v1/nax/summary"
    /**
     * 获取单钱包NAX收益信息
     */
    const val URL_GET_NAX_PROFITS = "api/v1/nax/profit/list"

    /******************************** NAS ********************************/
    /**
     * Nebulas rpc host
     */
    const val NAS_URL_HOST = "http://35.154.108.11:8685/v1/user/"
    /**
     * eth get balance
     */
    const val NAS_GET_BALANCE = "accountstate"

    /**
     * Submit the signed transaction
     */
    const val NAS_SEND_RAW_TRANSACTION = "rawtransaction"

    /**
     * get the state of the neb.
     */
    const val NAS_NEB_STATE = "nebstate"

    /**
     * Return the state of the account. Balance and nonce of the given address will be returned.
     */
    const val NAS_ACCOUNT_STATE = "accountstate"

    /**
     * Return current gasPrice.
     */
    const val NAS_GET_GAS_PRICE = "getGasPrice"

    /**
     * Call contract.
     */
    const val NAS_CALL = "call"

    /**
     * Return the estimate gas of transaction.
     */
    const val NAS_ESTIMATE_GAS = "estimateGas"

    /**
     * Return the transaction receipt
     */
    const val NAS_GET_TRANSACTION_RECEIPT = "getTransactionReceipt"


    /******************************** NAS ********************************/



    /******************************** ETH ********************************/
    /**
     * ethereum rpc host
     */
    const val ETH_URL_HOST = "https://api.myetherapi.com/eth/"
    /**
     * eth get balance
     */
    const val ETH_GET_BALANCE = "eth_getBalance"
    /**
     * eth 智能合约调用
     */
    const val ETH_CALL = "eth_call"
    /**
     * eth estimateGas
     */
    const val ETH_ESTIMATE_GAS = "eth_estimateGas"
    /**
     * eth eth_getTransactionCount 获取eth nonce
     */
    const val ETH_GET_TRANSACTION_COUNT = "eth_getTransactionCount"
    /**
     * eth send raw transaction
     */
    const val ETH_SEND_RAW_TRANSACTION = "eth_sendRawTransaction"
    /**
     * eth eth_getTransactionReceipt
     */
    const val ETH_GET_TRANSACTION_RECEIPT = "eth_getTransactionReceipt"
    /**
     * eth block number
     */
    const val ETH_BLOCK_NUMBER = "eth_blockNumber"


    /******************************** ETH ********************************/
}