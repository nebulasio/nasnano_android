# Nas Nano客户端数据库表结构

* Wallet【】
* Address【】
* Coin【】
* SupportToken【】
* Transaction【】

## Wallet(tableName = "wallet")

> 钱包表

| 类成员变量 | 数据库字段 | 类型      | 描述       |
| ---------- | ---------- | --------- | ---------- |
| id         | id         | Long      | 自增长主键 |
| walletName | walletName | String    | 钱包名称   |
| mnemonic   | mnemonic   | ByteArray | 加密助记词 |
|            |            |           |            |

## Address(tableName = "address")

> 

| 类成员变量 | 数据库字段 | 类型      | 描述                                                  |
| ---------- | ---------- | --------- | ----------------------------------------------------- |
| id         | id         | Long      | 自增长主键                                            |
| walletId   | wallet_id  | Long      | 外键：钱包ID                                          |
| address    | address    | String    | 地址                                                  |
| keyJson    | keyJson    | ByteArray | 加密keystore（私钥加密之后），可以通过这个算出address |
| platform   | platform   | String    | 所属公链                                              |

## Coin(tableName = "token")

> 钱包资产

| 类成员变量      | 数据库字段      | 类型    | 描述                                   |
| --------------- | --------------- | ------- | -------------------------------------- |
| id              | id              | Long    | 自增长主键                             |
| walletId        | wallet_id       | Long    | 外键：钱包ID                           |
| addressId       | address_id      | Long    | 外键：address                          |
| address         | address         | String? | address，默认“”                        |
| tokenId         | token_id        | String? | 后端返回的support_token的id            |
| symbol          | symbol          | String  | 数字货币符号                           |
| name            | name            | String  | 数字货币名称                           |
| platform        | platform        | String  | 区块链类型（属于ETH/NAS）              |
| contractAddress | contractAddress | String  | 智能合约地址                           |
| logo            | logo            | String  | logo                                   |
| tokenDecimals   | tokenDecimals   | String  | 币种的小数位，默认值：“18”             |
| quotation       | quotation       | String  | 数字货币行情，默认："+0%"              |
| balance         | balance         | String  | 数字货币数量                           |
| balanceValue    | balanceValue    | String  | 数字货币兑法币总价值，默认："0.00"     |
| displayed       | displayed       | Int     | 列表展示排序                           |
| type            | type            | Int     | 是否为核心币 1：是，0：否，默认0       |
| mark            | mark            | String  | 类型标识，CORE：核心币  NORMAL：常用币 |
| isShow          | isShow          | Boolean | 是否展示该token，默认true              |

## SupportToken(tableName = "support_token")

> 当前支持的币种（字典）

| 类成员变量      | 数据库字段      | 类型    | 描述                                           |
| --------------- | --------------- | ------- | ---------------------------------------------- |
| id              | id              | String  | 主键                                           |
| symbol          | symbol          | String  | 数字货币符号                                   |
| name            | name            | String  | 数字货币名称                                   |
| platform        | platform        | String  | 区块链类型（属于ETH/NAS）                      |
| contractAddress | contractAddress | String  | 智能合约地址                                   |
| logo            | logo            | String  | logo                                           |
| tokenDecimals   | tokenDecimals   | String  | 币种的小数位，默认值：“18”                     |
| quotation       | quotation       | String? | 数字货币行情，默认："+0%"                      |
| isSelected      | isSelected      | Boolean | 是否被选中，默认false                          |
| displayed       | displayed       | Int     | 列表展示排序                                   |
| type            | type            | Int     | 是否为核心币 1：是，0：否，默认0               |
| mark            | mark            | String? | 类型标识，CORE：核心币  NORMAL：常用币，默认“” |
| markName        | markName        | String? | 类型名称，默认“”                               |

## Transaction(tableName = "tx")

| 类成员变量      | 数据库字段      | 类型    | 描述                                                 |
| --------------- | --------------- | ------- | ---------------------------------------------------- |
| id              | id              | Long    | 自增长主键                                           |
| account         | account         | String? | 本地发生交易的钱包地址，默认null                     |
| amount          | amount          | String? | 交易数量，默认null                                   |
| blockHeight     | blockHeight     | String? | 交易产生是的区块高度，默认“”                         |
| blockTimestamp  | blockTimestamp  | Long?   | 发送交易的打包时间，默认null                         |
| confirmed       | confirmed       | Boolean | 是否已确认，默认false                                |
| confirmedCnt    | confirmedCnt    | Int     | 已确认节点数，默认0                                  |
| currencyId      | currencyId      | String? | 货币ID，默认null                                     |
| hash            | hash            | String? | tx hash 交易的Hash值                                 |
| libTimestamp    | libTimestamp    | Long?   | tx到达lib时间，默认null                              |
| maxConfirmCnt   | maxConfirmCnt   | Int     | 节点最大确认数，默认0                                |
| receiver        | receiver        | String? | 交易接收方，默认null                                 |
| sendTimestamp   | sendTimestamp   | Long?   | tx发送时间，默认null                                 |
| sender          | sender          | String? | 交易发送方，默认null                                 |
| status          | status          | String? | 交易状态，默认null                                   |
| txFee           | txFee           | String? | 矿工费，默认null                                     |
| name            | name            | String? | 对方姓名，默认“”                                     |
| coinSymbol      | coinSymbol      | String? | 发生交易的货币符号，默认null，并重写了get方法        |
| platform        | platform        | String  | 区块链类型（属于ETH/NAS），默认“”                    |
| tokenDecimals   | tokenDecimals   | String  | 币种的小数位，默认“18”                               |
| contractAddress | contractAddress | String  | 智能合约地址，默认“”                                 |
| nonce           | nonce           | String  | 当前账户正在交易的笔数，默认“”                       |
| txData          | txData          | String  | 附加数据（交易的时候想把某些数据记录到链上），默认“” |
| gasPrice        | gasPrice        | String  | gas price，默认“”                                    |
| gasLimit        | gasLimit        | String  | gas limit，默认“”                                    |
| remark          | remark          | String  | 交易备注，默认“”                                     |

