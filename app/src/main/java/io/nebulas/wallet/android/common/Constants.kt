package io.nebulas.wallet.android.common

import io.nebulas.wallet.android.BuildConfig

/**
 * Created by Heinoc on 2018/3/1.
 */
object Constants {

    //    val PLATFORMS = listOf(Walletcore.NAS, Walletcore.ETH)
    val PLATFORMS: List<String> = BuildConfig.PLATFORMS.split(";")

    val TOKEN_WHITE_LIST = listOf("nax", "nat", "nebulas", "nas", "atp")

    /**
     * cny
     * usd
     */
    var CURRENCY_SYMBOL_NAME = "cny"
    var CURRENCY_SYMBOL = "￥"
    /**
     * 系统语言
     */
    var LANGUAGE = "en"

    /**
     * 最多可创建的钱包数量
     */
    const val MAX_WALLET_COUNTS = 50

    /**
     * token scale,控制token的小数点后显示多少位小数
     */
    const val TOKEN_SCALE = 18

    const val TOKEN_FORMAT_ETH = "###,##0.0000##############"
    const val TOKEN_FORMAT = "###,##0.0000##############"

    const val PRICE_FORMAT = "###,##0.00"

    const val GAS_PRICE_FORMAT = "###,##0.00########"

    /**
     * NAS default mnemonic path
     */
    const val NAS_MNEMONIC_PATH = "m/44'/2718'/0'/0/0"
    /**
     * ETH default mnemonic path
     */
    const val ETH_MNEMONIC_PATH = "m/44'/60'/0'/0/0"

    /**
     * Keystore alias
     */
    const val KEYSTORE_ALIAS = "nas_keystore"

    const val UUID_FILE_PATH = "Android/data/"
    const val UUID_FILE_NAME = ".uid"


    /******************************** NAS ********************************/
    /**
     * NAS 主链id
     */
    const val NAS_CHAIN_ID = BuildConfig.NAS_CHAIN_ID

    /******************************** NAS ********************************/

    /******************************** ETH ********************************/
    /**
     * ETH 主链id
     */
    const val ETH_CHAIN_ID = BuildConfig.ETH_CHAIN_ID
    const val ETH_JSON_RPC_VERSION = "2.0"

    /******************************** ETH ********************************/


    /**
     * 网络请求缓存路径
     */
    const val NETWORK_CACHE_DIR = "networkCache"

    /**
     * 二维码存储路径
     */
    const val QRCODE_CACHE_DIR = "addressQRCode"
    const val KEYSTORE_QRCODE_CACHE_DIR = "keystoreQRCode"
    const val TX_QRCODE_CACHE_DIR = "txQRCode"

    /**
     * 通过DApp唤起应用时传递过来的交易信息
     */
    const val KEY_DAPP_TRANSFER_JSON = "keyDAppTransferJson"


    const val PRIVACY_TERMS_VERSION = "1.1"


    //输入密码页面展示
    const val KAInputPassword = "Input_Password"

    // 所有添加钱包按钮点击时
    const val kAAddWalletBtnClick = "Add_Wallet_Btn_Click"

    // 点击创建钱包
    const val kACreateWalletBtnClick = "Create_Wallet_Btn_Click"
    // 使用简单密码
    const val kACreateWalletUseSimplePwd = "Create_Wallet_Use_Simple_Pwd"
    // 使用复杂密码
    const val kACreateWalletUseComplexPwd = "Create_Wallet_Use_Complex_Pwd"
    // 创建钱包成功
    const val kACreateWalletSuccess = "WalletCreated"

    // 点击导入钱包
    const val kAImportWalletBtnClick = "Wallet_Import_Methods_Showed"
    // 点击助记词导入
    const val kAImportWalletMnemonicClick = "Wallet_Import_By_Mnemonic"
    // 点击Keystore导入
    const val kAImportWalletKeystoreClick = "Wallet_Import_By_Keystore"
    // 点击私钥导入
    const val kAImportWalletPrivateKeyClick = "Wallet_Import_By_PK"

    // 导入钱包成功
    const val kAImportWalletSuccess = "Wallet_Import_Success"

    // 点击首页转账按钮
    const val kAHomeTransferClick = "Home_Send"
    // 显示转账界面
    const val kATransferVCShowed = "Transfer_Showed"
    // 点击币详情转账按钮
    const val kACoinDetailTransferClick = "Transfer_Click_Coin_Detail"
    // 点击通过地址转账
    const val kATransferMethodAddress = "Transfer_By_Address"
    // 点击通过二维码转账
    const val kATransferMethodQRCode = "Transfer_By_QR_Code"
    // 转账页点击修改备注(已舍弃，2.0+新版备注不用再跳转)
    const val kATransferMemoClick = "Transfer_Change_Remarks"
    // 转账页点击修改Gas
    const val kATransferGasClick = "Transfer_Change_Gas"

    // 转账确认页点击确认
    const val kATransferConfirmViewConfirmClick = "Transfer_Confirm_Transfer"
    // 转账成功
    const val kATransferSuccess = "Transfer_Success"
    // 转账失败
    const val kATransferFailed = "Transfer_Failed"

    // 点击首页收币
    const val kAHomeReceiveClick = "Home_Receive"
    // 收币页面点击复制地址
    const val kAReceiveCopyClick = "Receive_Copy_Click"
    // 收币页面点击保存图片
    const val kAReceiveSaveClick = "Receive_Save_Click"
    // 收币页面点击分享
    const val kAReceiveShareClick = "Receive_Share_Click"

    // 点击首页备份
    const val kAHomeBackupClick = "Home_Urgent_For_Backups"
    // 点击助记词备份
    const val kABackupMnemonicClick = "Backup_Mnemonic_Click"
    // 助记词备份成功
    const val kABackupMnemonicSuccess = "Backup_Mnemonic_Success"
    // 点击Keystore备份
    const val kABackupKeystoreClick = "Backup_Keystore_Click"
    // 点击私钥备份
    const val kABackupPrivateKeyClick = "Backup_PrivateKey_Click"


    // 点击首页顶部广告
    const val kAHomeTopAdClick = "Home_Top_Ad_Click"
    // 点击首页消息流
    const val kAHomeFeedClick = "Home_Feed_Click"


    // 进入App
    const val kAUserSignIn = "Sign_In"

    // 添加钱包成功（无论是创建还是导入）
    const val kAAddWalletSuccess = "Add_Wallet_Success"

    // 通过助记词导入钱包成功
    const val kAImportWalletSuccessByMnemonic = "Wallet_Import_Success_By_Mnemonic"
    // 通过私钥导入钱包成功
    const val kAImportWalletSuccessByPK = "Wallet_Import_Success_By_PK"
    // 通过Keystore导入钱包成功
    const val kAImportWalletSuccessByKeystore = "Import_Success_By_Keystore"


    // 转账页点击确认
    const val kATransferConfirmClick = "Transfer_Tap_Transfer"

    // 首页眼睛关闭
    const val kAHomeHiddenDetail = "Home_Hidden_Detail"
    // 首页眼睛打开
    const val kAHomeShowDetail = "Home_Show_Detail"
    // 点击Dapps标签
    const val kADiscoverShowed = "Discover_Showed"

    //支持币种说明弹窗展示
    const val receiveSupporttokensShow = "Receive_Supporttokens_Show"

    //转账页面，点击右上角“扫码”
    const val sendScanClick = "Send_Scan_Click"

    /**
     * 换币流程打点
     */
    //首页代币换币提示点击
    const val Exchange_Entrance_Click = "Exchange_Entrance_Click"
    //换币说明页面点击开始
    const val Exchange_Start_Click = "Exchange_Start_Click"
    //换币说明页面点击“不需要换币”
    const val Exchange_NoNeed_Click = "Exchange_NoNeed_Click"
    //免责页面点击“已经阅读同意”
    const val Exchange_Agreement_Click = "Exchange_Agreement_Click"
    //在设置新地址前对原地址点击“备份”
    const val Exchange_OldBackup_Click = "Exchange_OldBackup_Click"
    //在设置新地址前对原地址备份成功（备份成功提示页面展现）
    const val Exchange_OldBackup_Success = "Exchange_OldBackup_Success"
    //设置地址页面展现
    const val Exchange_SetNewAddress_Show = "Exchange_SetNewAddress_Show"
    //设置成功页面展现
    const val Exchange_AddressSuccess_Show = "Exchange_AddressSuccess_Show"
    //设置成功后进行备份成功（备份成功提示页面展现）
    const val Exchange_NewBackup_Success = "Exchange_NewBackup_Success"
    //转入代币页面展现数据
    const val Exchange_ImportTokens_Show = "Exchange_ImportTokens_Show"
    //转入代币点击复制地址
    const val Exchange_CopyAddress_Click = "Exchange_CopyAddress_Click"
    //转入代币点击保存二维码
    const val Exchange_QRcode_Click = "Exchange_QRcode_Click"
    //“确定换币”按钮点击
    const val Exchange_Confirm_Click = "Exchange_Confirm_Click"


    //新钱包创建“立即备份”按钮点击
    const val Backup_NewUser_Click = "Backup_NewUser_Click"
    //新钱包创建备份成功（备份成功提示页面展现）
    const val Backup_NewUser_Success = "Backup_NewUser_Success"
    //首页收币点击后出现的提醒弹窗展现
    const val Backup_HomeReceive_Show = "Backup_HomeReceive_Show"
    //在首页收币弹窗中点击“备份”
    const val Backup_HomeReceive_Click = "Backup_HomeReceive_Click"
    //通过收币弹窗备份成功（备份成功提示页面展现）
    const val Backup_HomeReceive_Success = "Backup_HomeReceive_Success"
    //钱包详情页点击收币或转账的弹窗中点击“备份”
    const val Backup_WalletDetail_Click = "Backup_WalletDetail_Click"
    //通过钱包详情页点击收币或转账的弹窗中备份成功（备份成功提示页面展现）
    const val Backup_WalletDetail_Success = "Backup_WalletDetail_Success"
    //币种详情页点击收币或转账的弹窗中点击“备份”
    const val Backup_TokenDetail_Click = "Backup_TokenDetail_Click"
    //通过币种详情页点击收币或转账的弹窗中备份成功（备份成功提示页面展现）
    const val Backup_TokenDetail_Success = "Backup_TokenDetail_Success"

    //首页置顶广告位点击
    const val Home_TopAds_Click_ID = "Home_TopAds_Click_ID"
    //首页下方信息流点击
    const val Home_News_Click_ID = "Home_News_Click_ID"

    /**
     * 首页特殊空投展示
     */
    const val Home_ATPAds_Show = "Home_ATPAds_Show"

    /**
     * 首页特殊空投的点击
     */
    const val Home_ATPAds_Click = "Home_ATPAds_Click"

    /**
     * 交易记录特殊空投的展现
     */
    const val TxRecord_ATPAds_Show = "TxRecord_ATPAds_Show"

    /**
     * 交易记录特殊空投的点击
     */
    const val TxRecord_ATPAds_Click = "TxRecord_ATPAds_Click"

    /**
     * 钱包gas费用确认页面的展现(ATP)
     */
    const val ATPAds_Gas_Show = "ATPAds_Gas_Show"

    /**
     * 余额不足按钮的展现
     */
    const val ATPAds_NoBlance_Show = "ATPAds_NoBlance_Show"

    /**
     * 密码确认页面的展现
     */
    const val ATPAds_Password_Show = "ATPAds_Password_Show"

    /**
     * 密码输入成功
     */
    const val ATPAds_Password_Click = "ATPAds_Password_Click"

    val voteContracts = listOf("n1pADU7jnrvpPzcWusGkaizZoWgUywMRGMY")

}