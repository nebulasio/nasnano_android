package io.nebulas.wallet.android.util

import android.text.Editable
import android.text.TextWatcher


class TextChange(private val onAfterChange: () -> Unit,private val onBefroreChange:()->Unit,private val onTextChange:()->Unit):TextWatcher {


    override fun afterTextChanged(s: Editable?) {
        onTextChange()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        onBefroreChange()
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        onAfterChange()
    }

}