package io.nebulas.wallet.android.base

import android.content.Context
import android.databinding.DataBindingUtil
import android.databinding.ObservableArrayList
import android.databinding.ObservableList
import android.databinding.ViewDataBinding
import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * Created by Heinoc on 2018/2/22.
 */
abstract class BaseBindingAdapter<M, B : ViewDataBinding>(protected var context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var items: ObservableArrayList<M>
        protected set
    protected var itemsChangeCallback: ListChangedCallback

    protected var onItemClickListener: OnItemClickListener? = null

    init {
        this.items = ObservableArrayList()
        this.itemsChangeCallback = ListChangedCallback()
    }

    fun changeDataSource(list: MutableList<M>) {
        this.items.removeOnListChangedCallback(itemsChangeCallback)
        this.items.clear()
        this.items.addAll(list)
        this.notifyDataSetChanged()

        this.items.addOnListChangedCallback(itemsChangeCallback)
    }

    fun setOnClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    override fun getItemCount(): Int {
        return this.items.size
    }

    override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = DataBindingUtil.inflate<B>(LayoutInflater.from(this.context), this.getLayoutResId(viewType), parent, false)
        return getViewHolder(binding.root, viewType)
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = DataBindingUtil.getBinding<B>(holder.itemView)
        this.onBindItem(binding, this.items[position])
        binding?.executePendingBindings()//解决recyclerview item会闪烁的问题


        if (onItemClickListener != null) {

            holder.itemView.setOnClickListener {
                val pos = holder.layoutPosition
                if (pos >= 0)
                    onItemClickListener?.onItemClick(holder.itemView, pos)
            }
            holder.itemView.setOnLongClickListener {
                val pos = holder.layoutPosition
                if (pos >= 0)
                    onItemClickListener?.onItemLongClick(holder.itemView, pos)
                return@setOnLongClickListener true
            }
        }


    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.items.addOnListChangedCallback(itemsChangeCallback)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.items.removeOnListChangedCallback(itemsChangeCallback)
    }

    //region 处理数据集变化
    protected fun onChanged(newItems: ObservableArrayList<M>) {
        resetItems(newItems)
        notifyDataSetChanged()
    }

    protected fun onItemRangeChanged(newItems: ObservableArrayList<M>, positionStart: Int, itemCount: Int) {
        resetItems(newItems)
        notifyItemRangeChanged(positionStart, itemCount)
    }

    protected fun onItemRangeInserted(newItems: ObservableArrayList<M>, positionStart: Int, itemCount: Int) {
        resetItems(newItems)
        notifyItemRangeInserted(positionStart, itemCount)
    }

    protected fun onItemRangeMoved(newItems: ObservableArrayList<M>) {
        resetItems(newItems)
        notifyDataSetChanged()
    }

    protected fun onItemRangeRemoved(newItems: ObservableArrayList<M>, positionStart: Int, itemCount: Int) {
        resetItems(newItems)
        notifyItemRangeRemoved(positionStart, itemCount)
    }

    public fun resetItems(newItems: ObservableArrayList<M>) {
        this.items = newItems
    }
    //endregion

    /**
     * 如果需要自定义ViewHolder，可以重写该方法，返回自定义的（需继承BaseBindingViewHolder）ViewHolder
     */
    open protected fun getViewHolder(itemView: View, viewType: Int): BaseBindingViewHolder {
        return BaseBindingViewHolder(itemView)
    }

    @LayoutRes
    protected abstract fun getLayoutResId(viewType: Int): Int

    protected abstract fun onBindItem(binding: B?, item: M)

    inner class ListChangedCallback : ObservableList.OnListChangedCallback<ObservableArrayList<M>>() {
        override fun onChanged(newItems: ObservableArrayList<M>) {
            this@BaseBindingAdapter.onChanged(newItems)
        }

        override fun onItemRangeChanged(newItems: ObservableArrayList<M>, i: Int, i1: Int) {
            this@BaseBindingAdapter.onItemRangeChanged(newItems, i, i1)
        }

        override fun onItemRangeInserted(newItems: ObservableArrayList<M>, i: Int, i1: Int) {
            this@BaseBindingAdapter.onItemRangeInserted(newItems, i, i1)
        }

        override fun onItemRangeMoved(newItems: ObservableArrayList<M>, i: Int, i1: Int, i2: Int) {
            this@BaseBindingAdapter.onItemRangeMoved(newItems)
        }

        override fun onItemRangeRemoved(sender: ObservableArrayList<M>, positionStart: Int, itemCount: Int) {
            this@BaseBindingAdapter.onItemRangeRemoved(sender, positionStart, itemCount)
        }
    }


    /**
     * OnItemClickLitener
     */
    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
        fun onItemLongClick(view: View, position: Int)
    }


}
