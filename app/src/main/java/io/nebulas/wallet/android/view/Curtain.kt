package io.nebulas.wallet.android.view

import android.R
import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.annotation.ColorInt
import android.support.annotation.StringRes
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.RelativeLayout
import android.widget.TextView
import java.util.*
import kotlin.collections.HashMap

class Curtain private constructor(private val activity: Activity) {

    private val tag = javaClass.simpleName

    enum class CurtainLevel(val desc: String, val color: String) {
        NORMAL("用于一般性的正常提示样式", "#FF8F00"),
        SUCCESS("用于成功的提示样式", "#00CB91"),
        ERROR("用于失败的提示样式", "#FF5552")
    }

    companion object {

        public const val DURATION_DO_NOT_AUTO_DISMISS = -1L

        private var lifecycleCallback: Application.ActivityLifecycleCallbacks? = null

        private val queueManager: CurtainQueueManager = CurtainQueueManager()

        fun create(activity: Activity): Curtain {
            if (lifecycleCallback == null) {
                lifecycle(activity.application)
            }
            return Curtain(activity)
        }

        private fun lifecycle(app: Application) {
            lifecycleCallback = object : Application.ActivityLifecycleCallbacks {
                override fun onActivityPaused(activity: Activity?) {
                    queueManager.pause(activity)
                }

                override fun onActivityResumed(activity: Activity?) {
                    queueManager.resume(activity)
                }

                override fun onActivityStarted(activity: Activity?) {
                }

                override fun onActivityDestroyed(activity: Activity?) {
                    queueManager.destroyed(activity)
                }

                override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
                }

                override fun onActivityStopped(activity: Activity?) {
                }

                override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
                }
            }
            app.registerActivityLifecycleCallbacks(lifecycleCallback)
        }
    }

    private class CurtainQueueManager {
        private val queueMap: HashMap<Activity, CurtainQueue> = HashMap()

        fun pause(activity: Activity?) {
            activity?.apply {
                queueMap[this]?.pause()
            }
        }

        fun resume(activity: Activity?) {
            activity?.apply {
                queueMap[this]?.resume()
            }
        }

        fun destroyed(activity: Activity?) {
            activity?.apply {
                queueMap[this]?.destroyed()
            }
        }

        fun enqueue(curtain: Curtain) {
            var queue = queueMap[curtain.activity]
            if (queue == null) {
                queue = CurtainQueue()
                queueMap[curtain.activity] = queue
            }
            queue.enqueue(curtain)
        }

        fun remove(curtain: Curtain) {
            val queue = queueMap[curtain.activity]
            queue?.apply {
                remove(curtain)
            }
        }

        fun itemFinished(finishedItem: Curtain) {
            val queue = queueMap[finishedItem.activity]
            queue?.apply {
                itemFinished()
            }
        }
    }

    private class CurtainQueue : LinkedList<Curtain>() {
        private var focusCurtain: Curtain? = null
        private var isActive = false
        private var isPaused = false
        private val handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message?) {
                msg?.apply {
                    val tipper = obj as Curtain
                    if (tipper.dismiss()) {
                        itemFinished()
                    }
                }
            }
        }

        fun enqueue(curtain: Curtain) {
            add(curtain)
            next()
        }

        fun pause() {
            isPaused = true
        }

        fun resume() {
            isPaused = false
            next()
        }

        fun destroyed() {
            clear()
            handler.removeMessages(this.hashCode())
            focusCurtain?.apply {
                focusCurtain = null
            }
        }

        fun itemFinished() {
            isActive = false
            next()
        }

        fun next() {
            if (isActive || isPaused) {
                return
            }
            focusCurtain = pollFirst()
            focusCurtain?.apply {
                isActive = true
                realShow()
                if (duration != DURATION_DO_NOT_AUTO_DISMISS) {
                    val msg = Message.obtain()
                    msg.what = activity.hashCode()
                    msg.obj = this
                    handler.sendMessageDelayed(msg, duration + animDuration)
                }
            }
        }
    }

    //    private var minHeight: Int = 300 //300px == xxxhdpi 75dp
    private var minHeight: Int = activity.resources.getDimensionPixelSize(android.support.design.R.dimen.abc_action_bar_default_height_material) + getStatusBarHeight()
    private var duration: Long = 2000L
    private var animDuration: Long = 300L
    private var content: String = ""
    private var level: CurtainLevel = CurtainLevel.NORMAL
    private var customView: View? = null
    private var flingToDismiss: Boolean = true
    private var defaultTextSizeSP: Float = 18f
    private var pixelTextSize: Float = sp2px(defaultTextSizeSP)
    private var enterInterpolator: TimeInterpolator? = null
    private var exitInterpolator: TimeInterpolator? = null
    @ColorInt
    private var backgroundColor: Int? = null

    private var contentView: View? = null
    private var dismissed: Boolean = false
    private val horizontalMarginDP: Float = 20f
    private val verticalMarginDP: Float = 15f

    fun withContent(content: String): Curtain {
        this.content = content
        return this
    }

    fun withContentRes(@StringRes resId: Int): Curtain {
        this.content = activity.getString(resId)
        return this
    }

    fun withDuration(duration: Long): Curtain {
        if (this.duration == DURATION_DO_NOT_AUTO_DISMISS) {
            Log.w(tag, "已经设置了不自动消失，此次设置duration将无效")
        }
        this.duration = duration
        return this
    }

    fun withAnimatorDuration(animatorDuration: Long): Curtain {
        this.animDuration = animatorDuration
        return this
    }

    fun withMinHeight(heightPx: Int): Curtain {
        this.minHeight = heightPx
        return this
    }

    fun withLevel(curtainLevel: CurtainLevel): Curtain {
        this.level = curtainLevel
        return this
    }

    fun withBackgroundColor(@ColorInt color: Int): Curtain {
        this.backgroundColor = color
        return this
    }

    fun withPixelTextSize(textSize: Float): Curtain {
        this.pixelTextSize = textSize
        return this
    }

    /**
     * 按照SP设置字号
     */
    fun withScaledPixelTextSize(textSize: Float): Curtain {
        this.pixelTextSize = sp2px(textSize)
        return this
    }

    fun withoutAutoDismiss(): Curtain {
        this.duration = DURATION_DO_NOT_AUTO_DISMISS
        return this
    }

    fun withoutFlingToDismiss(): Curtain {
        this.flingToDismiss = false
        return this
    }

    fun withCustomView(view: View): Curtain {
        this.customView = view
        return this
    }

    fun withEnterInterpolator(enterInterpolator: TimeInterpolator): Curtain {
        this.enterInterpolator = enterInterpolator
        return this
    }

    fun withExitInterpolator(exitInterpolator: TimeInterpolator): Curtain {
        this.exitInterpolator = exitInterpolator
        return this
    }

    fun show() {
        queueManager.enqueue(this)
    }

    fun cancel() {
        queueManager.remove(this)
        if (!dismissed) {
            dismiss()
            queueManager.itemFinished(this)
        }
    }

    private fun realShow() {
        contentView = customView ?: getContentView()
        contentView?.apply {
            setupFlingToDismiss(this)
            val view = activity.findViewById<ViewGroup>(R.id.content)
            var parent: ViewParent? = view
            while (parent != null) {
                if (parent.parent == null || parent.parent !is ViewGroup) {
                    break
                }
                parent = parent.parent
            }
            val parentViewGroup: ViewGroup = parent as ViewGroup
            minimumHeight = minHeight
            parentViewGroup.addView(this, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

            viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    startEnterAnim(height)
                    viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            })

        }
    }

    private fun startEnterAnim(height: Int) {
        val anim = ValueAnimator.ofInt(0, height)
        anim.duration = animDuration
        enterInterpolator?.apply {
            anim.interpolator = this
        }
        anim.addUpdateListener {
            contentView!!.translationY = (it.animatedValue as Int).toFloat() - height
        }
        anim.start()
    }

    private fun dismiss(): Boolean {
        if (dismissed) {
            return false
        }
        contentView?.apply {
            val anim = ValueAnimator.ofInt(height, 0)
            anim.duration = animDuration
            exitInterpolator?.apply {
                anim.interpolator = this
            }
            anim.addUpdateListener {
                contentView!!.translationY = (it.animatedValue as Int).toFloat() - height
            }
            anim.addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(animation: Animator?) {
                    if (parent != null && parent is ViewGroup) {
                        (parent as ViewGroup).removeView(this@apply)
                    }
                }

                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {
                    dismissed = true
                }
            })
            anim.start()
        }
        return true
    }

    private fun getContentView(): View {
        val layout = RelativeLayout(activity)
        layout.setBackgroundColor(backgroundColor ?: Color.parseColor(level.color))
        val text = TextView(activity)
        text.setPadding(0, getStatusBarHeight(), 0, 0)
        text.text = content
        text.gravity = Gravity.CENTER
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, pixelTextSize)
        text.setTextColor(Color.WHITE)
        val lp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.apply {
            topMargin = dp2px(verticalMarginDP).toInt()
            leftMargin = dp2px(horizontalMarginDP).toInt()
            rightMargin = dp2px(horizontalMarginDP).toInt()
            bottomMargin = dp2px(verticalMarginDP).toInt()
            addRule(RelativeLayout.CENTER_IN_PARENT)
        }
        layout.addView(text, lp)
        return layout
    }

    private fun setupFlingToDismiss(view: View) {
        if (!flingToDismiss) {
            return
        }
        val gestureListener: GestureDetector.OnGestureListener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                if (velocityY < 0) {   //向上滑动
                    dismiss()
                    queueManager.itemFinished(this@Curtain)
                }
                return super.onFling(e1, e2, velocityX, velocityY)
            }
        }
        val gestureDetector = GestureDetector(activity, gestureListener)

        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun getStatusBarHeight(): Int {
        var statusBarHeight = 0
        val resourceId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = activity.resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }

    private fun sp2px(sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, activity.resources.displayMetrics)
    }

    private fun dp2px(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, activity.resources.displayMetrics)
    }

}