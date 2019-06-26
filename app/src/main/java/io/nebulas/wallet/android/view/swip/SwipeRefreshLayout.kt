package io.nebulas.wallet.android.view.swip

import android.content.Context
import android.graphics.Canvas
import android.os.Build.VERSION.SDK_INT
import android.support.v4.view.MotionEventCompat
import android.support.v4.view.ViewCompat
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.animation.*
import android.view.animation.Animation.AnimationListener
import android.widget.AbsListView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import io.nebulas.wallet.android.R


/**
 * Created by Heinoc on 2018/7/4.
 */
/**
 * The SwipeRefreshLayout should be used whenever the user can refresh the
 * contents of a view via a vertical swipe gesture. The activity that
 * instantiates this view should add an OnRefreshListener to be notified
 * whenever the swipe to refresh gesture is completed. The SwipeRefreshLayout
 * will notify the listener each and every time the gesture is completed again;
 * the listener is responsible for correctly determining when to actually
 * initiate a refresh of its content. If the listener determines there should
 * not be a refresh, it must call setRefreshing(false) to cancel any visual
 * indication of a refresh. If an activity wishes to show just the progress
 * animation, it should call setRefreshing(true). To disable the gesture and progress
 * animation, call setEnabled(false) on the view.
 *
 *
 *  This layout should be made the parent of the view that will be refreshed as a
 * result of the gesture and can only support one direct child. This view will
 * also be made the target of the gesture and will be forced to match both the
 * width and the height supplied in this layout. The SwipeRefreshLayout does not
 * provide accessibility events; instead, a menu item must be provided to allow
 * refresh of the content wherever this gesture is used.
 */
class SwipeRefreshLayout
/**
 * Constructor that is called when inflating SwipeRefreshLayout from XML.
 * @param context
 * @param attrs
 */
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {

    private var mProgressBar: SwipeProgressBar? = SwipeProgressBar(this) //the thing that shows progress is going
    private var mTarget: View? = null //the content that gets pulled down
    private var mOriginalOffsetTop: Int = 0
    private var mListener: OnRefreshListener? = null
    private var mPullListener: OnPullListener? = null
    private var mFrom: Int = 0
    /**
     * @return Whether the SwipeRefreshWidget is actively showing refresh
     * progress.
     */
    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    var isRefreshing = false
        set(refreshing) {
            if (isRefreshing != refreshing) {
                ensureTarget()
                mCurrPercentage = 0f
                field = refreshing
                if (isRefreshing) {
//                    mProgressBar!!.start()
                } else {
                    setTargetOffsetTopAndBottom(-mHeadHeight)
//                    mProgressBar!!.stop()

                }
            }
        }
    private val mTouchSlop: Int
    private var mDistanceToTriggerSync = -1f
    private var mMediumAnimationDuration: Int = resources.getInteger(android.R.integer.config_mediumAnimTime)
    private var mFromPercentage = 0f
    private var mCurrPercentage = 0f
    private val mProgressBarHeight: Int
    private var mCurrentTargetOffsetTop: Int = 0

    private var mInitialMotionY: Float = 0.toFloat()
    private var mLastMotionY: Float = 0.toFloat()
    private var mIsBeingDragged: Boolean = false
    private var mActivePointerId = INVALID_POINTER

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.
    private var mReturningToStart: Boolean = false
    private var mCanStartRefresh: Boolean = false
    private var mDecelerateInterpolator: DecelerateInterpolator = DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR)
    private val mAccelerateInterpolator: AccelerateInterpolator

    private var mIsDefaultHeadView = false

    private var mHeadView: View? = null
    private var mRefreshImageview: ImageView? = null
    private var mRefreshTitle: TextView? = null
    private var mRefreshSubTitle: TextView? = null
    private var mRefreshProgress: ProgressBar? = null
    private var mRotateAnimation: Animation? = null
    private var mIsPulling: Boolean = false
    private var mCanRefreshing: Boolean = false
    private var mHeadHeight: Int = 0
    private var mIsShowColorProgressBar = true
    //    private boolean mIsBootom = true;

    private val mAnimateToStartPosition = object : Animation() {
        public override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            var targetTop = 0
            if (mFrom != mOriginalOffsetTop) {
                targetTop = mFrom + ((mOriginalOffsetTop - mFrom) * interpolatedTime).toInt()
            }
            var offset = targetTop - mTarget!!.top
            val currentTop = mTarget!!.top
            if (offset + currentTop < 0) {
                offset = 0 - currentTop
            }
            if (isRefreshing) {
                mTarget!!.top = mHeadHeight
                mHeadView!!.top = 0
                mCurrentTargetOffsetTop = mTarget!!.top
            } else {
                setTargetOffsetTopAndBottom(offset)
            }
        }
    }

    private val mShrinkTrigger = object : Animation() {
        public override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            val percent = mFromPercentage + (0 - mFromPercentage) * interpolatedTime
//            mProgressBar!!.setTriggerPercentage(percent)
        }
    }

    private val mReturnToStartPositionListener = object : BaseAnimationListener() {
        override fun onAnimationEnd(animation: Animation) {
            // Once the target content has returned to its start position, reset
            // the target offset to 0
            mCurrentTargetOffsetTop = 0
        }
    }

    private val mShrinkAnimationListener = object : BaseAnimationListener() {
        override fun onAnimationEnd(animation: Animation) {
            mCurrPercentage = 0f
        }
    }

    private val mReturnToStartPosition = Runnable {
        mReturningToStart = true
        animateOffsetToStartPosition(mCurrentTargetOffsetTop + paddingTop,
                mReturnToStartPositionListener)
    }

    // Cancel the refresh gesture and animate everything back to its original state.
    private val mCancel = Runnable {
        mReturningToStart = true
        // Timeout fired since the user last moved their finger; animate the
        // trigger to 0 and put the target back at its original position
        if (mProgressBar != null) {
            mFromPercentage = mCurrPercentage
            mShrinkTrigger.duration = mMediumAnimationDuration.toLong()
            mShrinkTrigger.setAnimationListener(mShrinkAnimationListener)
            mShrinkTrigger.reset()
            mShrinkTrigger.interpolator = mDecelerateInterpolator
            startAnimation(mShrinkTrigger)
        }
        animateOffsetToStartPosition(mCurrentTargetOffsetTop + paddingTop,
                mReturnToStartPositionListener)
    }

    init {

        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

        mMediumAnimationDuration = resources.getInteger(
                android.R.integer.config_mediumAnimTime)

        setWillNotDraw(false)
        mProgressBar = SwipeProgressBar(this)
        val metrics = resources.displayMetrics
        mProgressBarHeight = (metrics.density * PROGRESS_BAR_HEIGHT).toInt()
        mDecelerateInterpolator = DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR)
        mAccelerateInterpolator = AccelerateInterpolator(ACCELERATE_INTERPOLATION_FACTOR)

        val a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS)
        isEnabled = a.getBoolean(0, true)
        a.recycle()
    }

    private fun addDefaultHeadView(context: Context) {
        mHeadView = LayoutInflater.from(context).inflate(R.layout.pull_to_refresh_header_vertical, null)
        mRefreshImageview = mHeadView!!.findViewById(R.id.pull_to_refresh_image) as ImageView
        mRefreshTitle = mHeadView!!.findViewById(R.id.pull_to_refresh_text) as TextView
        mRefreshSubTitle = mHeadView!!.findViewById(R.id.pull_to_refresh_sub_text) as TextView
        mRefreshProgress = mHeadView!!.findViewById(R.id.pull_to_refresh_progress) as ProgressBar
        addView(mHeadView, 0)

        mRotateAnimation = RotateAnimation(0f, -180f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f)
        mRotateAnimation!!.duration = 150
        mRotateAnimation!!.fillAfter = true

        mIsDefaultHeadView = true
    }

    fun setRefreshSubText(text: String) {
        if (mRefreshSubTitle != null && !TextUtils.isEmpty(text)) {
            mRefreshSubTitle!!.text = text
        }
    }


    fun setHeadView(headView: View?) {
        if (headView != null) {
            mHeadView = headView
            if (childCount == 2) {
                removeViewAt(0)
            }
            addView(mHeadView, 0)
            requestLayout()
        }
    }


    fun hideColorProgressBar() {
        mIsShowColorProgressBar = false
    }


    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        removeCallbacks(mCancel)
        removeCallbacks(mReturnToStartPosition)
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(mReturnToStartPosition)
        removeCallbacks(mCancel)
    }

    private fun animateOffsetToStartPosition(from: Int, listener: AnimationListener) {
        mFrom = from
        mAnimateToStartPosition.reset()
        mAnimateToStartPosition.duration = mMediumAnimationDuration.toLong()
        mAnimateToStartPosition.setAnimationListener(listener)
        mAnimateToStartPosition.interpolator = mDecelerateInterpolator
        mTarget!!.startAnimation(mAnimateToStartPosition)
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    fun setOnRefreshListener(listener: OnRefreshListener) {
        mListener = listener
    }

    fun setOnPullListener(listener: OnPullListener) {
        mPullListener = listener
    }


    private fun setTriggerPercentage(percent: Float) {
        if (percent == 0f) {
            // No-op. A null trigger means it's uninitialized, and setting it to zero-percent
            // means we're trying to reset state, so there's nothing to reset in this case.
            mCurrPercentage = 0f
            return
        }
        mCurrPercentage = percent
//        mProgressBar!!.setTriggerPercentage(percent)
    }


    @Deprecated("Use {@link #setColorSchemeResources(int, int, int, int)}")
    fun setColorScheme(colorRes1: Int, colorRes2: Int, colorRes3: Int, colorRes4: Int) {
        setColorSchemeResources(colorRes1, colorRes2, colorRes3, colorRes4)
    }

    /**
     * Set the four colors used in the progress animation from color resources.
     * The first color will also be the color of the bar that grows in response
     * to a user swipe gesture.
     */
    fun setColorSchemeResources(colorRes1: Int, colorRes2: Int, colorRes3: Int,
                                colorRes4: Int) {
        val res = resources
        setColorSchemeColors(res.getColor(colorRes1), res.getColor(colorRes2),
                res.getColor(colorRes3), res.getColor(colorRes4))
    }

    /**
     * Set the four colors used in the progress animation. The first color will
     * also be the color of the bar that grows in response to a user swipe
     * gesture.
     */
    fun setColorSchemeColors(color1: Int, color2: Int, color3: Int, color4: Int) {
        ensureTarget()
//        mProgressBar!!.setColorScheme(color1, color2, color3, color4)
    }

    private fun ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid out yet.
        if (childCount == 1) {
            addDefaultHeadView(context)
        }
        if (mTarget == null) {
            if (childCount > 2 && !isInEditMode) {
                throw IllegalStateException(
                        "SwipeRefreshLayout can host max two direct child")
            }
            mTarget = getChildAt(1)
            mOriginalOffsetTop = mTarget!!.top + paddingTop
        }

        if (mHeadView == null) {
            mHeadView = getChildAt(0)
        }
        if (mDistanceToTriggerSync == -1f) {
            if (parent != null && (parent as View).height > 0) {
                val metrics = resources.displayMetrics
                //                mDistanceToTriggerSync = (int) Math.min(
                //                        ((View) getParent()) .getHeight() * MAX_SWIPE_DISTANCE_FACTOR,
                //                                REFRESH_TRIGGER_DISTANCE * metrics.density);
                mDistanceToTriggerSync = mHeadHeight.toFloat()
            }
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
//        if (mIsShowColorProgressBar) {
//            mProgressBar!!.draw(canvas)
//        }

    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = measuredWidth
        val height = measuredHeight
//        mProgressBar!!.setBounds(0, 0, width, mProgressBarHeight)
        if (childCount == 0) {
            return
        }


        val child = getChildAt(1)
        val childLeft = paddingLeft
        val childTop = mCurrentTargetOffsetTop + paddingTop
        val childWidth = width - paddingLeft - paddingRight
        val childHeight = height - paddingTop - paddingBottom
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)
        mHeadView!!.layout(childLeft, childTop - mHeadView!!.measuredHeight, childLeft + childWidth, childTop)
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (childCount == 1) {
            addDefaultHeadView(context)
        }

        if (childCount > 2 && !isInEditMode) {
            throw IllegalStateException("SwipeRefreshLayout can host max two direct child")
        }

        if (childCount > 1) {
            //            getChildAt(0).measure( MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
            //                    MeasureSpec.EXACTLY),
            //                    MeasureSpec.makeMeasureSpec(100,MeasureSpec.EXACTLY));
            if (mHeadView == null) {
                mHeadView = getChildAt(0)
            }
            measureChild(mHeadView, widthMeasureSpec, heightMeasureSpec)
            mHeadHeight = mHeadView!!.measuredHeight
            getChildAt(1).measure(
                    View.MeasureSpec.makeMeasureSpec(
                            measuredWidth - paddingLeft - paddingRight,
                            View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(
                            measuredHeight - paddingTop - paddingBottom,
                            View.MeasureSpec.EXACTLY))
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    fun canChildScrollUp(): Boolean {
        return if (SDK_INT < 14) {
            if (mTarget is AbsListView) {
                val absListView = mTarget as AbsListView?
                absListView!!.childCount > 0 && (absListView.firstVisiblePosition > 0 || absListView.getChildAt(0)
                        .top < absListView.paddingTop)
            } else {
                mTarget!!.scrollY > 0
            }
        } else {
            ViewCompat.canScrollVertically(mTarget!!, -1)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        ensureTarget()

        val action = MotionEventCompat.getActionMasked(ev)

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false
        }

        if (!isEnabled || mReturningToStart || canChildScrollUp()) {
            // Fail fast if we're not in a state where a swipe is possible
            return false
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mInitialMotionY = ev.y
                mLastMotionY = mInitialMotionY
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0)
                mIsBeingDragged = false
                mCurrPercentage = 0f
                mIsPulling = false
                mCanRefreshing = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.")
                    return false
                }

                val pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId)
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.")
                    return false
                }

                val y = MotionEventCompat.getY(ev, pointerIndex)
                val yDiff = y - mInitialMotionY
                if (yDiff > mTouchSlop) {
                    mLastMotionY = y
                    mIsBeingDragged = true
                }
            }

            MotionEventCompat.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mIsBeingDragged = false
                mCurrPercentage = 0f
                mActivePointerId = INVALID_POINTER
            }
        }

        return mIsBeingDragged
    }

    override fun requestDisallowInterceptTouchEvent(b: Boolean) {
        // Nope.
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val action = MotionEventCompat.getActionMasked(ev)

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false
        }

        if (!isEnabled || mReturningToStart || canChildScrollUp()) {
            // Fail fast if we're not in a state where a swipe is possible
            return false
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mInitialMotionY = ev.y
                mLastMotionY = mInitialMotionY
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0)
                mIsBeingDragged = false
                mCurrPercentage = 0f
                mIsPulling = false
                mCanRefreshing = false
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId)
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.")
                    return false
                }

                val y = MotionEventCompat.getY(ev, pointerIndex)
                val yDiff = y - mInitialMotionY

                if (!mIsBeingDragged && yDiff > mTouchSlop) {
                    mIsBeingDragged = true
                }

                if (mIsBeingDragged) {
                    // User velocity passed min velocity; trigger a refresh
                    mCanStartRefresh = yDiff / 2 > mDistanceToTriggerSync
                    //                    else {
                    // Just track the user's movement
                    setTriggerPercentage(
                            mAccelerateInterpolator.getInterpolation(
                                    if (yDiff / mDistanceToTriggerSync > 1) 1F else yDiff / mDistanceToTriggerSync))
                    updateContentOffsetTop((yDiff / 2).toInt())
                    if (mLastMotionY > y && mTarget!!.top == paddingTop) {
                        // If the user puts the view back at the top, we
                        // don't need to. This shouldn't be considered
                        // cancelling the gesture as the user can restart from the top.
                        removeCallbacks(mCancel)
                    }
                    //                        else {
                    //                            updatePositionTimeout();
                    //                        }
                    //                    }
                    mLastMotionY = y
                }
            }

            MotionEventCompat.ACTION_POINTER_DOWN -> {
                val index = MotionEventCompat.getActionIndex(ev)
                mLastMotionY = MotionEventCompat.getY(ev, index)
                mActivePointerId = MotionEventCompat.getPointerId(ev, index)
            }

            MotionEventCompat.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (mCanStartRefresh) {
                    startRefresh()
                } else {
                    removeCallbacks(mCancel)
                    post(mCancel)
                }

                mIsBeingDragged = false
                mCurrPercentage = 0f
                mActivePointerId = INVALID_POINTER
                return false
            }
        }

        return true
    }

    private fun startRefresh() {
        isRefreshing = true
        mReturnToStartPosition.run()
        setTargetOffsetTopAndBottom(-mHeadHeight)


        removeCallbacks(mCancel)
//        setDefaultHeadViewRefreshing()

        if (isRefreshing) {
            if (mListener != null) {
                mListener!!.onRefresh()
            }

            if (mPullListener != null) {
                mPullListener!!.onRefreshing(mHeadView!!)
            }
        }


    }

    private fun setDefaultHeadViewRefreshing() {
        if (mIsDefaultHeadView) {
            mRefreshImageview!!.clearAnimation()
            mRefreshImageview!!.visibility = View.GONE
            mRefreshProgress!!.visibility = View.VISIBLE
            mRefreshTitle!!.text = resources.getString(R.string.refreshing)
        }
    }

    private fun updateContentOffsetTop(targetTop: Int) {
        var targetTop = targetTop
        val currentTop = mTarget!!.top
        //        if (targetTop > mDistanceToTriggerSync) {
        //            targetTop = (int) mDistanceToTriggerSync;
        //        } else
        if (targetTop < 0) {
            targetTop = 0
        }

        if (mIsDefaultHeadView) {
            mRefreshImageview!!.visibility = View.VISIBLE
            mRefreshProgress!!.visibility = View.GONE
        }
        if (currentTop > mDistanceToTriggerSync) {

            if (!mCanRefreshing && !isRefreshing) {
                mIsPulling = false
                if (mPullListener != null) {
                    mPullListener!!.onCanRefreshing(mHeadView!!)
                }
                if (mIsDefaultHeadView) {
                    mRefreshImageview!!.startAnimation(mRotateAnimation)
                    mRefreshTitle!!.text = resources.getString(R.string.loosenToRefresh)
                }

            }
            mCanRefreshing = true

        } else {

            if (!mIsPulling && !isRefreshing) {
                mCanRefreshing = false
                if (mPullListener != null) {
                    mPullListener!!.onPulling(mHeadView!!)
                }
                if (mIsDefaultHeadView) {
                    mRefreshImageview!!.clearAnimation()
                    mRefreshTitle!!.text = resources.getString(R.string.pullToRefresh)
                }
            }
            mIsPulling = true
        }
        setTargetOffsetTopAndBottom(targetTop - currentTop)

    }

    private fun setTargetOffsetTopAndBottom(offset: Int) {
        mTarget!!.offsetTopAndBottom(offset)
        mHeadView!!.offsetTopAndBottom(offset)
        mCurrentTargetOffsetTop = mTarget!!.top
    }

    private fun updatePositionTimeout() {
        removeCallbacks(mCancel)
        postDelayed(mCancel, RETURN_TO_ORIGINAL_POSITION_TIMEOUT)
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = MotionEventCompat.getActionIndex(ev)
        val pointerId = MotionEventCompat.getPointerId(ev, pointerIndex)
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mLastMotionY = MotionEventCompat.getY(ev, newPointerIndex)
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex)
        }
    }

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    interface OnRefreshListener {
        fun onRefresh()
    }

    /**
     * Simple AnimationListener to avoid having to implement unneeded methods in
     * AnimationListeners.
     */
    private open inner class BaseAnimationListener : AnimationListener {
        override fun onAnimationStart(animation: Animation) {}

        override fun onAnimationEnd(animation: Animation) {}

        override fun onAnimationRepeat(animation: Animation) {}
    }

    companion object {
        private val LOG_TAG = SwipeRefreshLayout::class.java.simpleName

        private val RETURN_TO_ORIGINAL_POSITION_TIMEOUT: Long = 300
        private val ACCELERATE_INTERPOLATION_FACTOR = 1.5f
        private val DECELERATE_INTERPOLATION_FACTOR = 2f
        private val PROGRESS_BAR_HEIGHT = 4f
        private val MAX_SWIPE_DISTANCE_FACTOR = .6f
        private val REFRESH_TRIGGER_DISTANCE = 120
        private val INVALID_POINTER = -1
        private val LAYOUT_ATTRS = intArrayOf(android.R.attr.enabled)
    }
}
/**
 * Simple constructor to use when creating a SwipeRefreshLayout from code.
 * @param context
 */