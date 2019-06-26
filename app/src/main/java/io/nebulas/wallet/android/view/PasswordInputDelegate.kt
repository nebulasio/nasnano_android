package io.nebulas.wallet.android.view

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import java.util.*

class PasswordInputDelegate private constructor(private val context: Context) {

    private val tag = javaClass.simpleName!!

    interface DelegateCallback {
        fun onDelegateTextChanged(displayDelegate: TextView, value: String)
    }

    interface DelegateIndexCallback {
        /**
         * @param value value.length is (1 OR 0); 0:delete 1:input
         */
        fun onDelegateIndexTextChanged(index: Int, value: String)
    }

    private val passwordView: EditText = EditText(context)
    private lateinit var rootView: ViewGroup
    private lateinit var owner: Any
    private var displayViewArray: ArrayList<TextView> = ArrayList()
    var callback: DelegateCallback? = null
    var indexCallback: DelegateIndexCallback? = null
    private val handler: Handler = Handler(Looper.getMainLooper())


    constructor(activity: Activity) : this(activity as Context) {
        owner = activity
        rootView = activity.findViewById(android.R.id.content)
        doAddView()
    }

    constructor(dialog: Dialog) : this(dialog.context) {
        owner = dialog
        rootView = dialog.findViewById(android.R.id.content)
        doAddView()
    }

    constructor(fragment: Fragment) : this(fragment.requireActivity()) {
        owner = fragment
        val v = fragment.view
        if (v == null) {
            throw IllegalArgumentException("The fragment do not have a view.")
        } else {
            if (v is ViewGroup) {
                rootView = v
                doAddView()
            } else {
                throw IllegalArgumentException("Root view of the fragment is not a ViewGroup")
            }
        }
    }

    private val watcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            s?.apply {
                val k = when {
                    before == 1 -> ""
                    count == 1 -> this[start].toString()
                    else -> ""
                }
                if (indexCallback != null) {
                    indexCallback?.onDelegateIndexTextChanged(start, k)
                } else {
                    showWithDelegate(start, k)
                }
            }
        }
    }

    private val onClick = View.OnClickListener { requestFocus() }

    init {
        passwordView.apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            transformationMethod = PasswordTransformationMethod.getInstance()
            filters = arrayOf(InputFilter.LengthFilter(6))
            isCursorVisible = false
            isFocusable = true
            setBackgroundColor(Color.TRANSPARENT)
            isFocusableInTouchMode = true
            addTextChangedListener(watcher)
        }
    }

    private fun doAddView() {
        when (owner) {
            is Activity -> (owner as Activity).window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            is Fragment -> (owner as Fragment).requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            is Dialog -> (owner as Dialog).window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        val param = ViewGroup.MarginLayoutParams(1, 1)
        param.topMargin = 200
        rootView.addView(passwordView, param)
    }

    fun setFocusable(isFocusable:Boolean) {
        passwordView.isEnabled = isFocusable
    }

    fun setDisplayDelegate(vararg textViews: TextView) {
        displayViewArray.clear()
        displayViewArray.addAll(textViews)
        textViews.forEach {
            it.isClickable = true
            it.setOnClickListener(onClick)
        }
    }

    fun addDisplayDelegate(textView: TextView) {
        displayViewArray.add(textView)
        textView.isClickable = true
        textView.setOnClickListener(onClick)
    }

    fun clearPassword() {
        passwordView.removeTextChangedListener(watcher)
        passwordView.setText("")
        val size = displayViewArray.size
        (0 until size).forEach {
            showWithDelegate(it, "")
        }
        passwordView.addTextChangedListener(watcher)
    }

    fun requestFocus() {
        passwordView.requestFocus()
        val im: InputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.showSoftInput(passwordView, InputMethodManager.SHOW_IMPLICIT)
    }

    fun removeFocus() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(passwordView.windowToken, 0) //强制隐藏键盘
        passwordView.clearFocus()
        rootView.requestFocus()
    }

    fun getPassword(): String {
        return passwordView.text.toString()
    }

    private fun showWithDelegate(index: Int, c: CharSequence) {
        if (index >= displayViewArray.size) {
            return
        }
        val target = displayViewArray[index].apply {
            text = c
            callback?.onDelegateTextChanged(this, c.toString())
        }
        if (c.isNotEmpty()) {
            val preIndex = index - 1
            if (preIndex >= 0) {
                val preContent = displayViewArray[preIndex].text
                displayViewArray[preIndex].text = passwordView.transformationMethod.getTransformation(preContent, passwordView)
            }
            handler.postDelayed({
                if (target.text.isNotEmpty()) {
                    target.text = passwordView.transformationMethod.getTransformation(c, passwordView)
                }
            }, 800)
        }
    }

}