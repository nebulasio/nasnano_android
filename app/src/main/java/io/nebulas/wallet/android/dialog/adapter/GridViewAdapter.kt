package io.nebulas.wallet.android.dialog.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.image.ImageUtil
import io.nebulas.wallet.android.module.token.model.SupportTokenModel

class GridViewAdapter(private val context: Context, private val tokenList: MutableList<Any>) : BaseAdapter() {

    private var inflater: LayoutInflater? = LayoutInflater.from(context)


    override fun getView(p0: Int, p1: View?, p2: ViewGroup): View {
        var convertView = p1
        var viewHolder: ViewHolder?
        if (convertView == null) {
            convertView = inflater?.inflate(R.layout.item_gridview_dialog, p2, false)

            viewHolder = ViewHolder()
            viewHolder.name = convertView!!.findViewById(R.id.tokenName)
            viewHolder.imageView = convertView.findViewById(R.id.img)
        } else {
            viewHolder = convertView.tag as ViewHolder
        }
        val supportTokenModel:SupportTokenModel = tokenList?.get(p0) as SupportTokenModel
        if (supportTokenModel != null) {
            ImageUtil.load(context!!, viewHolder.imageView!!, supportTokenModel!!.img)

            viewHolder.name!!.text = (supportTokenModel?.tokenName)
        }
        convertView.tag = viewHolder

        return convertView


    }

    override fun getItem(p0: Int): Any? {
        return null
    }

    override fun getItemId(p0: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return tokenList!!.size
    }

    internal inner class ViewHolder {
        var name: TextView? = null
        var imageView: ImageView? = null
    }
}