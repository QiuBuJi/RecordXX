package com.example.recordxx.activity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.Scroller
import android.widget.TextView
import com.example.recordxx.MainActivity.Companion.llm
import com.example.recordxx.R
import kotlin.math.abs

class EditableItem : ConstraintLayout {
    private var scroller: Scroller
    private var oldX = 0
    private var oldY = 0
    private var offsetX = 0
    private var offsetY = 0
    private var downX = 0
    private var downY = 0
    private lateinit var tvContent: TextView
    private lateinit var tvDelete: TextView
    private var scrollerDuration = 300
    private var lockMove = false
    private var mCanDelete = false
    /**可以拉出删除按钮的区域宽度*/
    private val mRightWidth = 180// 80
    private var mOnce = true
    private val paint = Paint()

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        paint.color = Color.argb(60, 0, 0, 0)
        scroller = Scroller(context)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        tvContent = findViewById(R.id.listItem_textView_content)
        tvDelete = findViewById(R.id.listItem_textView_delete)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x.toInt()
        val y = event.y.toInt()
        val disX = abs(downX - x)
        val disY = abs(downY - y)

        if (lockMove) {
            mTemp?.run {
                if (event.action == MotionEvent.ACTION_UP) setShowDelete(false)
                return true
            }
        }

        when (event.action) {
            //************************************************************ACTION_DOWN
            MotionEvent.ACTION_DOWN -> {
                oldX = x
                oldY = y
                downX = x
                downY = y
                mOnce = true
                mCanDelete = x > width - mRightWidth

                //按下后，如果正在删除，则取消删除
                mTemp?.run {
                    setShowDelete(false)
                    return true
                }
            }
            //************************************************************ACTION_UP
            MotionEvent.ACTION_UP   -> {
                if (!mCanDelete || disX < 4) return super.onTouchEvent(event)
                //if (downX == x) return super.onTouchEvent(event);
                setShowDelete(offsetX)
                return true
            }
            //************************************************************ACTION_MOVE
            MotionEvent.ACTION_MOVE -> {
                offsetX = oldX - x
                offsetY = oldY - y
                if (!mCanDelete) return super.onTouchEvent(event)

                //如果横向移动，就不能纵向移动
                if (mOnce) {
                    if (disX != disY) {
                        mOnce = false
                        val verticalScroll: Boolean = disX < disY
                        llm.setVerticalScrollable(verticalScroll)
                    }
                }
                val curX = scrollX + offsetX

                //限制，不能再向左滑了
                if (curX < tvDelete.width && disX > 20) scrollTo(curX, 0)
                oldX = x
                oldY = y
                return true
            }
        }
//        return super.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    /**设置 “删除按钮” 的显示与否*/
    internal fun setShowDelete(showDelete: Boolean) = setShowDelete(if (showDelete) 1 else -1)

    internal fun setShowDelete(offset: Int) {
        var temp: EditableItem? = null
        val scrollX = scrollX
        val distance: Int

        //手指左滑********************
        if (offset >= 0) {
            val width = tvDelete.width
            if (scrollX >= 0) {
                if (scrollX > width) distance = -(scrollX - width)
                else {
                    distance = width - scrollX
                    temp = this
                }
            } else distance = width - scrollX
        } else distance = if (scrollX >= 0) -scrollX else abs(scrollX)

        scroller.startScroll(scrollX, 0, distance, 0, scrollerDuration)
        invalidate()

        if (temp != null) {
            isPressed = true
            llm.setVerticalScrollable(false)//不能上下移动
            lockMove = false
        } else {
            isPressed = false
            llm.setVerticalScrollable(true)//可以上下移动
            lockMove = true
        }
        mTemp = temp
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, 0)
            invalidate()
        }
    }

    public override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val width = width.toFloat()
        val height = height.toFloat()
        val startX = width - mRightWidth

        //画辅助线
        c.drawLine(startX, 0f, startX, height, paint)
        c.drawLine(startX, 0f, width, 0f, paint)
        c.drawLine(startX, height, width, height, paint)
    }

    companion object {
        internal var mTemp: EditableItem? = null

        fun fadeDelete() {
            mTemp?.run { setShowDelete(false) }
        }
    }
}