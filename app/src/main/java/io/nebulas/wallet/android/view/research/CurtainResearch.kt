package io.nebulas.wallet.android.view.research

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Context
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

class CurtainResearch private constructor() {

    private val tag = javaClass.simpleName

    enum class CurtainLevel(val desc: String, val color: String) {
        NORMAL("用于一般性的正常提示样式", "#FF8F00"),
        SUCCESS("用于成功的提示样式", "#00CB91"),
        ERROR("用于失败的提示样式", "#FF5552")
    }

    private interface IWindowOwner<T> {
        fun getWindow(): Window
        fun getOwner(): T
        fun getContext(): Context
    }

    private inner class ActivityWindowOwner(val activity: Activity) : IWindowOwner<Activity> {
        override fun getOwner(): Activity = activity
        override fun getWindow(): Window = activity.window
        override fun getContext(): Context = activity
    }

    private inner class DialogWindowOwner(val dialog: Dialog) : IWindowOwner<Dialog> {
        override fun getOwner(): Dialog = dialog
        override fun getWindow(): Window = dialog.window
        override fun getContext(): Context = dialog.context
    }

    private lateinit var windowOwner: IWindowOwner<*>

    private constructor(activity: Activity) : this() {
        windowOwner = ActivityWindowOwner(activity)
    }

    private constructor(dialog: Dialog) : this() {
        windowOwner = DialogWindowOwner(dialog)
    }

    companion object {

        public const val DURATION_DO_NOT_AUTO_DISMISS = -1L

        private var lifecycleCallback: Application.ActivityLifecycleCallbacks? = null

        private val queueManager: CurtainQueueManager = CurtainQueueManager()

        private val dialogSet: MutableSet<Dialog> = mutableSetOf()

        fun create(activity: Activity): CurtainResearch {
            if (lifecycleCallback == null) {
                lifecycle(activity.application)
            }
            return CurtainResearch(activity)
        }

        fun create(dialog: Dialog): CurtainResearch {
            if (!dialogSet.contains(dialog)) {
                dialog.setOnDismissListener {
                    queueManager.destroy(it as Dialog)
                    dialogSet.remove(it)
                }
            }
            return CurtainResearch(dialog)
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
        private val queueMapForDialog: HashMap<Dialog, CurtainQueue> = HashMap()

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

        fun destroy(dialog: Dialog) {
            queueMapForDialog[dialog]?.destroyed()
        }

        fun enqueue(CurtainResearch: CurtainResearch) {
            if (CurtainResearch.windowOwner is ActivityWindowOwner) {
                val activity: Activity = CurtainResearch.windowOwner.getOwner() as Activity
                var queue = queueMap[activity]
                if (queue == null) {
                    queue = CurtainQueue()
                    queueMap[activity] = queue
                }
                queue.enqueue(CurtainResearch)
            } else if (CurtainResearch.windowOwner is DialogWindowOwner) {
                val dialog: Dialog = CurtainResearch.windowOwner.getOwner() as Dialog
                var queue = queueMapForDialog[dialog]
                if (queue == null) {
                    queue = CurtainQueue()
                    queueMapForDialog[dialog] = queue
                }
                queue.enqueue(CurtainResearch)
            }
        }

        fun remove(CurtainResearch: CurtainResearch) {
            val q = if (CurtainResearch.windowOwner is ActivityWindowOwner) {
                queueMap
            } else {
                queueMapForDialog
            }
            val queue = q[CurtainResearch.windowOwner.getOwner()]
            queue?.apply {
                remove(CurtainResearch)
            }
        }

        fun itemFinished(finishedItem: CurtainResearch) {
            val q = if (finishedItem.windowOwner is ActivityWindowOwner) {
                queueMap
            } else {
                queueMapForDialog
            }
            val queue = q[finishedItem.windowOwner.getOwner()]
            queue?.apply {
                itemFinished()
            }
        }
    }

    private class CurtainQueue : LinkedList<CurtainResearch>() {
        private var focusCurtain: CurtainResearch? = null
        private var isActive = false
        private var isPaused = false
        private val handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message?) {
                msg?.apply {
                    val tipper = obj as CurtainResearch
                    if(null!= tipper.onDilalogDismiss){
                        tipper.onDilalogDismiss!!.invoke()
                    }
                    if (tipper.dismiss()) {
                        itemFinished()
                    }
                }
            }
        }

        fun enqueue(CurtainResearch: CurtainResearch) {
            add(CurtainResearch)
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
                handler.post({ realShow() })
                if (duration != DURATION_DO_NOT_AUTO_DISMISS) {
                    val msg = Message.obtain()
                    msg.what = windowOwner.getOwner()!!.hashCode()
                    msg.obj = this
                    handler.sendMessageDelayed(msg, duration + animDuration)
                }
            }
        }
    }

    //    private var minHeight: Int = 300 //300px == xxxhdpi 75dp
    private var minHeight: Int = 0
    private var duration: Long = 2000L
    private var animDuration: Long = 300L
    private var content: String = ""
    private var level: CurtainLevel = CurtainLevel.NORMAL
    private var customView: View? = null
    private var flingToDismiss: Boolean = true
    private var defaultTextSizeSP: Float = 18f
    private var pixelTextSize: Float = -1f
    private var enterInterpolator: TimeInterpolator? = null
    private var exitInterpolator: TimeInterpolator? = null
    private var onDilalogDismiss :  (() -> Unit)? = null
    @ColorInt
    private var backgroundColor: Int? = null

    private var contentView: View? = null
    private var dismissed: Boolean = false
    private val horizontalMarginDP: Float = 20f
    private val verticalMarginDP: Float = 15f

    fun withContent(content: String): CurtainResearch {
        this.content = content
        return this
    }

    fun withContentRes(@StringRes resId: Int): CurtainResearch {
        this.content = windowOwner.getContext().getString(resId)
        return this
    }

    fun withDuration(duration: Long): CurtainResearch {
        if (this.duration == DURATION_DO_NOT_AUTO_DISMISS) {
            Log.w(tag, "已经设置了不自动消失，此次设置duration将无效")
        }
        this.duration = duration
        return this
    }

    fun withAnimatorDuration(animatorDuration: Long): CurtainResearch {
        this.animDuration = animatorDuration
        return this
    }

    fun withMinHeight(heightPx: Int): CurtainResearch {
        this.minHeight = heightPx
        return this
    }

    fun withLevel(curtainLevel: CurtainLevel): CurtainResearch {
        this.level = curtainLevel
        return this
    }

    fun withBackgroundColor(@ColorInt color: Int): CurtainResearch {
        this.backgroundColor = color
        return this
    }

    fun withPixelTextSize(textSize: Float): CurtainResearch {
        this.pixelTextSize = textSize
        return this
    }

    /**
     * 按照SP设置字号
     */
    fun withScaledPixelTextSize(textSize: Float): CurtainResearch {
        this.pixelTextSize = sp2px(textSize)
        return this
    }

    fun withoutAutoDismiss(): CurtainResearch {
        this.duration = DURATION_DO_NOT_AUTO_DISMISS
        return this
    }

    fun withoutFlingToDismiss(): CurtainResearch {
        this.flingToDismiss = false
        return this
    }

    fun withCustomView(view: View): CurtainResearch {
        this.customView = view
        return this
    }

    fun withEnterInterpolator(enterInterpolator: TimeInterpolator): CurtainResearch {
        this.enterInterpolator = enterInterpolator
        return this
    }

    fun withExitInterpolator(exitInterpolator: TimeInterpolator): CurtainResearch {
        this.exitInterpolator = exitInterpolator
        return this
    }

    fun withDismissBlock(onDilalogDismiss: (() -> Unit)?):CurtainResearch{
        this.onDilalogDismiss = onDilalogDismiss
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
        minHeight = windowOwner.getContext().resources.getDimensionPixelSize(android.support.design.R.dimen.abc_action_bar_default_height_material) + getStatusBarHeight()
        contentView = customView ?: getContentView()
        contentView?.apply {
            setupFlingToDismiss(this)
//            val view = activity.findViewById<ViewGroup>(R.id.content)
//            var parent: ViewParent? = view
//            while (parent != null) {
//                if (parent.parent == null || parent.parent !is ViewGroup) {
//                    break
//                }
//                parent = parent.parent
//            }
//            val parentViewGroup: ViewGroup = parent as ViewGroup
            minimumHeight = minHeight
//            parentViewGroup.addView(this, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

            windowOwner.getWindow().addContentView(this, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

//            val filed = windowOwner.getWindow()::class.java.getDeclaredField("mDecor").apply {
//                isAccessible = true
//            }
//            val decorView: ViewGroup = filed.get(windowOwner.getWindow()) as ViewGroup
//            decorView.addView(this, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

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
        val layout = RelativeLayout(windowOwner.getContext())
        layout.setBackgroundColor(backgroundColor ?: Color.parseColor(level.color))
        val text = TextView(windowOwner.getContext())
        text.setPadding(0, getStatusBarHeight(), 0, 0)
        text.text = content
        text.gravity = Gravity.CENTER
        if (pixelTextSize == -1f) {
            pixelTextSize = sp2px(defaultTextSizeSP)
        }
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
                    queueManager.itemFinished(this@CurtainResearch)
                }
                return super.onFling(e1, e2, velocityX, velocityY)
            }
        }
        val gestureDetector = GestureDetector(windowOwner.getContext(), gestureListener)

        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun getStatusBarHeight(): Int {
        var statusBarHeight = 0
        val resourceId = windowOwner.getContext().resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = windowOwner.getContext().resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }

    private fun sp2px(sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, windowOwner.getContext().resources.displayMetrics)
    }

    private fun dp2px(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, windowOwner.getContext().resources.displayMetrics)
    }

}