package com.example.recordxx.activity

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent

class RView : RecyclerView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    //屏蔽多点触控
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean { //屏蔽多点触控
        when (ev.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_POINTER_DOWN -> return false
        }
        return super.dispatchTouchEvent(ev)
    }
}