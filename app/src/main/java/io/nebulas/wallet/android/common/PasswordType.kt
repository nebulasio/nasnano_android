package io.nebulas.wallet.android.common

import android.support.annotation.IntDef

const val PASSWORD_TYPE_SIMPLE = 0
const val PASSWORD_TYPE_COMPLEX = 1

@IntDef(PASSWORD_TYPE_SIMPLE, PASSWORD_TYPE_COMPLEX)
@Retention(AnnotationRetention.SOURCE)
annotation class PasswordType