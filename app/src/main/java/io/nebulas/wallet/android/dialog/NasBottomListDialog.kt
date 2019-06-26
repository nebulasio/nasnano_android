package io.nebulas.wallet.android.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.main.dialog_nas_bottom_list.*
import org.jetbrains.anko.find

class NasBottomListDialog<T, TW : NasBottomListDialog.ItemWrapper<T>>(context: Context,
                                                                      val title: String,
                                                                      dataSource: MutableList<TW> = mutableListOf(),
                                                                      initSelectedItem: TW? = null,
                                                                      val withSelectedStatusIcon: Boolean = true) : Dialog(context, R.style.AppDialog) {

    interface ItemWrapper<out T> {
        fun getDisplayText(): String
        fun getOriginObject(): T
        fun isShow(): Boolean
    }

    interface OnItemSelectedListener<out T, in TW : NasBottomListDialog.ItemWrapper<T>> {
        fun onItemSelected(itemWrapper: TW)
    }

    interface OnCloseListener {
        fun onClosed()
    }

    var selectedItem: TW? = initSelectedItem
        set(value) {
            field = value
            if (isShowing && !field!!.isShow()) {
                adapter.notifyDataSetChanged()
            }
        }
    var dataSourceSetter: MutableList<TW> = dataSource
        set(value) {
            field = value
            if (isShowing) {
                adapter.notifyDataSetChanged()
            }
        }
    var onItemSelectedListener: OnItemSelectedListener<T, TW>? = null
    var onCloseListener: OnCloseListener? = null
    private val adapter = LAdapter()
    private val screenHeight: Int = Util.screenHeight(context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_nas_bottom_list)
        setCanceledOnTouchOutside(false)
        tvTitle.text = title
        ibClose.setOnClickListener {
            dismiss()
            onCloseListener?.onClosed()
        }
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            selectedItem = adapter.getItem(position)
            if(selectedItem!!.isShow()){
                return@setOnItemClickListener
            }else {
                listView.postDelayed({
                    dismiss()
                    selectedItem?.apply {

                        onItemSelectedListener?.onItemSelected(this)
                    }
                }, 100)
            }
        }
    }


    fun showWithSelectedItem(item: TW?) {
        selectedItem = item
        if (!isShowing) {
            show()
        }
    }

    private fun setMaxHeight() {
        val maxHeight = screenHeight * 0.4f
        val attr = window.attributes
        attr.height = maxHeight.toInt()
        window.attributes = attr
    }

    private val lis = ViewTreeObserver.OnPreDrawListener {
        val height = rootView.measuredHeight
        if (height > screenHeight * 0.4) {
            setMaxHeight()
        }
        true
    }

    override fun show() {
        if (context != null && context is Activity) {
            val activity = context as Activity
            if (activity.isFinishing || activity.isDestroyed) {
                return
            }
        }
        super.show()
        rootView.viewTreeObserver.addOnPreDrawListener(lis)
        val attr = window.attributes
        attr.gravity = Gravity.BOTTOM
        attr.width = WindowManager.LayoutParams.MATCH_PARENT
        window.decorView.setPadding(0, 0, 0, 0)
        window.attributes = attr
    }

    override fun dismiss() {
        super.dismiss()
        rootView.viewTreeObserver.removeOnPreDrawListener(lis)
    }

    inner class LAdapter : BaseAdapter() {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View?
            if (convertView != null) {
                view = convertView
            } else {
                view = LayoutInflater.from(context).inflate(R.layout.item_nas_bottom_list, parent, false)
                val holder = VH(view!!.find(R.id.tvContent), view.find(R.id.ivCheckedStatus), view.find(R.id.backupDes))
                view.tag = holder
            }
            val holder: VH = view.tag as VH
            val item = getItem(position)
            holder.tvContent.text = item.getDisplayText()
            holder.ivCheckedStatus.visibility = if (withSelectedStatusIcon && selectedItem == item) {
                View.VISIBLE
            } else {
                View.GONE
            }
            holder.backupDes.visibility = if (item.isShow()) {
                holder.tvContent.alpha = 0.4f
                View.VISIBLE
            } else {
                holder.tvContent.alpha = 1f
                View.GONE
            }
            return view
        }

        override fun getItem(position: Int): TW = dataSourceSetter[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getCount(): Int = dataSourceSetter.size

    }

    class VH(val tvContent: TextView, val ivCheckedStatus: ImageView, val backupDes: TextView)
}