package io.nebulas.wallet.android.view

import android.text.method.ReplacementTransformationMethod

/**
 * Created by young on 2018/6/6.
 */
object HiddenTransformationMethod : ReplacementTransformationMethod() {
    private val allChars = " ;:\"{},_'@!#$%^&*()~|\\[]/<>`-+=.0123456789zxcvbnmasdfghjklqwertyuiopZXCVBNMASDFGHJKLQWERTYUIOP"
    override fun getOriginal(): CharArray {
        return allChars.toCharArray()
    }

    override fun getReplacement(): CharArray {
        val org = original
        val replace = CharArray(org.size)
        (0 until replace.size).forEach {
            replace[it] = '*'
        }
        return replace
    }
}