package io.nebulas.wallet.android.view

import android.graphics.Rect
import android.text.GetChars
import android.text.TextUtils
import android.text.method.TransformationMethod
import android.view.View

/**
 * Created by young on 2018/6/6.
 */
object JustShowFirstTransformationMethod : TransformationMethod {
    override fun onFocusChanged(view: View?, sourceText: CharSequence?, focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        //貌似用不到，参考 ReplacementTransformationMethod //This callback isn't used.
    }

    override fun getTransformation(source: CharSequence?, view: View?): CharSequence {
        if (source==null){
            return source?:""
        }
        return JustShowFirstCharSequence(source)
    }

    private class JustShowFirstCharSequence(private val mSource: CharSequence):CharSequence, GetChars {
        override val length: Int
            get() = mSource.length

        override fun get(index: Int): Char {
            if (index>0){
                return '*'
            }
            return mSource[index]
        }

        override fun subSequence(start: Int, end: Int): CharSequence {
            val c = CharArray(end - start)

            getChars(start, end, c, 0)
            return String(c)
        }

        override fun toString(): String {
            val c = CharArray(length)

            getChars(0, length, c, 0)
            return String(c)
        }

        override fun getChars(start: Int, end: Int, dest: CharArray, off: Int) {
            TextUtils.getChars(mSource, start, end, dest, off)
            val offend = end - start + off

            (off until offend)
                    .filter { it >0 }
                    .forEach { dest[it] = '*' }
        }
    }
}