package io.nebulas.wallet.android.extensions

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.View

/**
 * Created by young on 2018/5/31.
 */

fun animatorSequentially(vararg animations: Animator): AnimatorSet {
    val set = AnimatorSet()
    set.playSequentially(animations.asList())
    return set
}

fun animatorTogether(vararg animations: Animator): AnimatorSet {
    val set = AnimatorSet()
    set.playTogether(animations.asList())
    return set
}

fun <T : Animator> T.duration(duration: Long): T {
    this.duration = duration
    return this
}

fun <T : Animator> T.delay(delay: Long): T {
    this.startDelay = delay
    return this
}

fun <T : Animator> T.withInterpolator(interpolator: TimeInterpolator): T {
    this.interpolator = interpolator
    return this
}

fun <T : Animator> T.doOnEnd(block: (Animator?) -> Unit): T {
    addListener(object : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator?) {}
        override fun onAnimationEnd(animation: Animator?) {
            block(animation)
        }

        override fun onAnimationCancel(animation: Animator?) {}
        override fun onAnimationStart(animation: Animator?) {}
    })
    return this
}

fun <T : Animator> T.doOnRepeat(block: (Animator?) -> Unit): T {
    addListener(object : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator?) {
            block(animation)
        }
        override fun onAnimationEnd(animation: Animator?) {}
        override fun onAnimationCancel(animation: Animator?) {}
        override fun onAnimationStart(animation: Animator?) {}
    })
    return this
}

fun <T : View> T.startValueAnimator(start: Float, end: Float, duration: Long = 300L, delay: Long = 0L, block: T.(Float) -> Unit) {
    val anim = ValueAnimator.ofFloat(start, end).duration(duration).delay(delay)
    anim.addUpdateListener {
        block(it.animatedValue as Float)
    }
    anim.start()
}

fun <T : View> T.withValueAnimator(start: Float,
                                   end: Float,
                                   duration: Long = 300L,
                                   delay: Long = 0L,
                                   block: T.(Float) -> Unit): ValueAnimator {
    val anim = ValueAnimator.ofFloat(start, end).duration(duration).delay(delay)
    anim.addUpdateListener {
        block(it.animatedValue as Float)
    }
    return anim
}