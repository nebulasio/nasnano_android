package io.nebulas.wallet.android.network.nas.model

import io.nebulas.wallet.android.base.BaseEntity


/*
// Request
curl -i -H 'Content-Type: application/json' -X POST http://localhost:8685/v1/user/getTransactionReceipt -d '{"hash":"cda54445ffccf4ea17f043e86e54be11b002053f9edbe30ae1fbc0437c2b6a73"}'

// Result
{
    "result":{
        "hash":"cda54445ffccf4ea17f043e86e54be11b002053f9edbe30ae1fbc0437c2b6a73",
        "chainId":100,
        "from":"n1Z6SbjLuAEXfhX1UJvXT6BB5osWYxVg3F3",
        "to":"n1PxKRaJ5jZHXwTfgM9WqkZJJVXBxRcggEE",
        "value":"10000000000000000000",
        "nonce":"53",
        "timestamp":"1521964742",
        "type":"binary",
        "data":null,
        "gas_price":"1000000",
        "gas_limit":"20000",
        "contract_address":"",
        "status":1,
        "gas_used":"20000",
        "execute_error":"",
        "execute_result":"\"\""
    }
}
*/
class NasTransactionReceipt: BaseEntity() {

    var hash:String = ""
    /**
     *  Transaction status, 0 failed, 1 success, 2 pending.
     */
    var status:Int = 2
}