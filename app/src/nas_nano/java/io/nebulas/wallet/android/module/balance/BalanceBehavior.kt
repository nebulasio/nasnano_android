package io.nebulas.wallet.android.module.balance

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.Log
import android.view.View


/**
 * Created by Heinoc on 2018/4/13.
 */
class BalanceBehavior(context: Context, attr: AttributeSet) : CoordinatorLayout.Behavior<View>(context, attr) {

    /**
     * 监听哪个类型View的滑动
     */
    override fun layoutDependsOn(parent: CoordinatorLayout?, child: View?, dependency: View?): Boolean {
        return dependency is RecyclerView
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout?, child: View?, dependency: View?): Boolean {
        val offset = dependency!!.getTop() - child!!.getTop()
        ViewCompat.offsetTopAndBottom(child, offset)
        return true
    }


    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, directTargetChild: View, target: View, axes: Int, type: Int): Boolean {
        /**
         * 当滑动为垂直方向时，返回true
         */
        return axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
    }

    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) {

        val scrollY = target.scrollY
        child.scrollY = scrollY

        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)
    }


}