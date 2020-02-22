package com.wvt.codeeditor.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.Scroller
import androidx.core.content.ContextCompat
import com.wvt.codeeditor.R
import java.util.*
import kotlin.math.abs

open class ScrollableView(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(
        context,
        attrs,
        defStyleAttr
    ) {
    //水平边界
    private var mHorizontalBound: Int = 0
    //垂直边界
    private var mVerticalBound: Int = 0
    //滑动辅助类
    private var mVelocityTracker: VelocityTracker? = null
    private val mScroller = Scroller(context)
    //最小拖动像素数
    private val mTouchSlop = ViewConfiguration.get(context).scaledPagingTouchSlop
    //最大和最小加速度
    private val mMaximumVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity
    private val mMinimumVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private var mLastX: Int = 0
    private var mLastY: Int = 0
    //滑动条相关
    //滑动条参数
    var scrollBarWidth: Float
    var scrollBarColor: Int
    var scrollBarRadius: Float
    var verticalScrollBarMarginRight: Float
    var verticalScrollBarMarginTop: Float
    var verticalScrollBarMarginBottom: Float
    var horizontalScrollBarMarginBottom: Float
    var horizontalScrollBarMarginRight: Float
    var horizontalScrollBarMarginLeft: Float

    //按动滑动条时的误差值
    val mScrollBarTouchingError: Float

    //滑动条自动隐藏相关
    var isScrollBarAutoFade: Boolean
    var scrollBarAutoFadeTime: Int
    private var mScrollBarFadeTimeCounter: Int
    private val mScrollBarAutoFadeDelay: Int = 200
    private var mScrollBarAutoFadeTimer = Timer()
    //滑动条工具
    private val mScrollBarPaint = Paint()
    private val mVerticalScrollBarRectF = RectF()
    private val mHorizontalScrollBarRectF = RectF()

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ScrollableView)
        scrollBarColor = typedArray.getColor(
            R.styleable.ScrollableView_scrollBarColor,
            ContextCompat.getColor(context, R.color.default_scrollbar_color)
        )
        scrollBarWidth = typedArray.getDimension(
            R.styleable.ScrollableView_scrollBarSize,
            context.resources.getDimension(R.dimen.default_scrollbar_width)
        )
        scrollBarRadius = typedArray.getDimension(
            R.styleable.ScrollableView_scrollBarRadius,
            context.resources.getDimension(R.dimen.default_scrollbar_radius)
        )
        verticalScrollBarMarginRight = typedArray.getDimension(
            R.styleable.ScrollableView_verticalScrollBarMarginRight,
            context.resources.getDimension(R.dimen.default_vertical_scrollbar_margin_right)
        )
        verticalScrollBarMarginTop = typedArray.getDimension(
            R.styleable.ScrollableView_verticalScrollBarMarginTop,
            context.resources.getDimension(R.dimen.default_vertical_scrollbar_margin_top)
        )
        verticalScrollBarMarginBottom = typedArray.getDimension(
            R.styleable.ScrollableView_verticalScrollBarMarginBottom,
            context.resources.getDimension(R.dimen.default_vertical_scrollbar_margin_bottom)
        )
        horizontalScrollBarMarginBottom = typedArray.getDimension(
            R.styleable.ScrollableView_horizontalScrollBarMarginBottom,
            context.resources.getDimension(R.dimen.default_horizontal_scrollbar_margin_bottom)
        )
        horizontalScrollBarMarginRight = typedArray.getDimension(
            R.styleable.ScrollableView_horizontalScrollBarMarginRight,
            context.resources.getDimension(R.dimen.default_horizontal_scrollbar_margin_right)
        )
        horizontalScrollBarMarginLeft = typedArray.getDimension(
            R.styleable.ScrollableView_horizontalScrollBarMarginLeft,
            context.resources.getDimension(R.dimen.default_horizontal_scrollbar_margin_left)
        )
        isScrollBarAutoFade = typedArray.getBoolean(
            R.styleable.ScrollableView_scrollBarAutoFade,
            context.resources.getBoolean(R.bool.default_scrollbar_auto_fade)
        )
        scrollBarAutoFadeTime = typedArray.getInteger(
            R.styleable.ScrollableView_scrollBarAutoFade,
            context.resources.getInteger(R.integer.default_scrollbar_auto_fade_time)
        )
        typedArray.recycle()
        mScrollBarTouchingError =
            context.resources.getDimension(R.dimen.default_scrollbar_touch_error)
        mScrollBarPaint.color = scrollBarColor
        mScrollBarFadeTimeCounter = scrollBarAutoFadeTime / mScrollBarAutoFadeDelay
    }

    //处理滑动事件
    private var isScrolling: Boolean = false
    private var isHoldingVerticalScrollBar: Boolean = false
    private var isHoldingHorizontalScrollBar: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mScroller.forceFinished(true)
                mLastX = event.x.toInt()
                mLastY = event.y.toInt()
                if (mVelocityTracker == null)
                    mVelocityTracker = VelocityTracker.obtain()
                mVelocityTracker?.addMovement(event)
                if (!isScrollBarFade()) {
                    //判断是否按下滑动条
                    val rectF = RectF(
                        event.x + scrollX - mScrollBarTouchingError,
                        event.y + scrollY - mScrollBarTouchingError,
                        event.x + scrollX + mScrollBarTouchingError,
                        event.y + scrollY + mScrollBarTouchingError
                    )
                    if (RectF.intersects(mVerticalScrollBarRectF, rectF))
                        isHoldingVerticalScrollBar = true
                    else if (RectF.intersects(mHorizontalScrollBarRectF, rectF))
                        isHoldingHorizontalScrollBar = true

                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = mLastX - event.x.toInt()
                val dy = mLastY - event.y.toInt()
                if (!isScrollBarFade()) {
                    if (isHoldingVerticalScrollBar) {
                        scrollByInBound(
                            0,
                            ((mVerticalBound - height) * (-dy) / (height - paddingTop - paddingBottom - verticalScrollBarMarginTop - verticalScrollBarMarginBottom - mVerticalScrollBarRectF.height())).toInt()
                        )
                        mLastX = event.x.toInt()
                        mLastY = event.y.toInt()
                    } else if (isHoldingHorizontalScrollBar) {
                        scrollByInBound(
                            ((mHorizontalBound - width) * (-dx) / (width - paddingRight - paddingLeft - horizontalScrollBarMarginLeft - horizontalScrollBarMarginRight - mHorizontalScrollBarRectF.width())).toInt(),
                            0
                        )
                        mLastX = event.x.toInt()
                        mLastY = event.y.toInt()
                    }
                }
                if (isScrolling) {
                    mScrollBarFadeTimeCounter = 0
                    scrollByInBound(dx, dy)
                    mVelocityTracker?.addMovement(event)
                    mLastX = event.x.toInt()
                    mLastY = event.y.toInt()
                } else if (abs(dx) > mTouchSlop || abs(dy) > mTouchSlop) {
                    isScrolling = true
                    mLastX = event.x.toInt()
                    mLastY = event.y.toInt()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (isHoldingVerticalScrollBar)
                    isHoldingVerticalScrollBar = false
                if (isHoldingHorizontalScrollBar)
                    isHoldingHorizontalScrollBar = false
                if (isScrolling) {
                    mScroller.abortAnimation()
                    mVelocityTracker?.apply {
                        computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
                        mScroller.fling(
                            scrollX, scrollY,
                            if (abs(xVelocity) > mMinimumVelocity) -xVelocity.toInt() else 0,
                            if (abs(yVelocity) > mMinimumVelocity) -yVelocity.toInt() else 0,
                            0, mHorizontalBound - width, 0, mVerticalBound - height
                        )
                    }
                    isScrolling = false
                }
                mVelocityTracker?.recycle()
                mVelocityTracker = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun computeScroll() {
        super.computeScroll()
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.currX, mScroller.currY)
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mScrollBarAutoFadeTimer.schedule(
            mScrollBarAutoFadeTask,
            0L,
            mScrollBarAutoFadeDelay.toLong()
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mScrollBarAutoFadeTimer.cancel()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        //绘制滑动条
        if (!isScrollBarFade()) {
            updateScrollBarRectF()
            if (mVerticalBound > height) {
                canvas?.drawRoundRect(
                    mVerticalScrollBarRectF,
                    scrollBarRadius,
                    scrollBarRadius,
                    mScrollBarPaint
                )
            }
            if (mHorizontalBound > width) {
                canvas?.drawRoundRect(
                    mHorizontalScrollBarRectF,
                    scrollBarRadius,
                    scrollBarRadius,
                    mScrollBarPaint
                )
            }
        }
    }

    //更新滑动条边界
    private fun updateScrollBarRectF() {
        mVerticalScrollBarRectF.set(
            width - paddingRight - scrollBarWidth - verticalScrollBarMarginRight + scrollX,
            (height - paddingTop - paddingBottom - verticalScrollBarMarginBottom - verticalScrollBarMarginTop) * scrollY / mVerticalBound + scrollY + verticalScrollBarMarginTop,
            width - paddingRight + scrollX - verticalScrollBarMarginRight,
            (height - paddingTop - paddingBottom - verticalScrollBarMarginBottom - verticalScrollBarMarginTop) * (scrollY + height) / mVerticalBound + scrollY + verticalScrollBarMarginTop
        )
        mHorizontalScrollBarRectF.set(
            (width - paddingLeft - paddingRight - horizontalScrollBarMarginLeft - horizontalScrollBarMarginRight) * scrollX / mHorizontalBound + scrollX + horizontalScrollBarMarginLeft,
            height - paddingTop - paddingBottom - scrollBarWidth + scrollY - horizontalScrollBarMarginBottom,
            (width - paddingLeft - paddingRight - horizontalScrollBarMarginLeft - horizontalScrollBarMarginRight) * (scrollX + width) / mHorizontalBound + scrollX + horizontalScrollBarMarginLeft,
            height - paddingTop - paddingBottom + scrollY - horizontalScrollBarMarginBottom
        )
    }

    //不会滑过边界的scrollBy
    fun scrollByInBound(x: Int, y: Int) {
        var dx = x
        var dy = y
        if (mHorizontalBound > width) {
            //右边界
            if (scrollX + x >= mHorizontalBound - width)
                dx = mHorizontalBound - width - scrollX
            //左边界
            else if (scrollX + x <= 0)
                dx = -scrollX
        } else dx = 0
        if (mVerticalBound > height) {
            //下边界
            if (scrollY + y >= mVerticalBound - height)
                dy = mVerticalBound - height - scrollY
            //上边界
            else if (scrollY + y <= 0)
                dy = -scrollY
        } else dy = 0
        scrollBy(dx, dy)
    }

    //自动显示滑动条
    override fun scrollTo(x: Int, y: Int) {
        super.scrollTo(x, y)
        mScrollBarFadeTimeCounter = 0
    }

    //在监听器中调用此方法以设置滑动边界
    fun setScrollBound(widthRealSpec: Int, heightRealSpec: Int) {
        this.mHorizontalBound = widthRealSpec
        this.mVerticalBound = heightRealSpec
    }

    //滑动条自动隐藏相关
    private val mScrollBarAutoFadeTask = object : TimerTask() {
        override fun run() {
            with(scrollBarAutoFadeTime / mScrollBarAutoFadeDelay) {
                if (mScrollBarFadeTimeCounter < this && !isHoldingHorizontalScrollBar && !isHoldingVerticalScrollBar) {
                    mScrollBarFadeTimeCounter++
                    if (mScrollBarFadeTimeCounter >= this)
                        invalidate()
                }
            }
        }
    }

    //判断滑动条是否隐藏

    private fun isScrollBarFade(): Boolean {
        if (!isScrollBarAutoFade)
            return false
        return mScrollBarFadeTimeCounter >= (scrollBarAutoFadeTime / mScrollBarAutoFadeDelay)
    }
}