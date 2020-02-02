package com.example.recordxx

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet

class LLManager : LinearLayoutManager {
    internal var mCanScrollVertically = true

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, orientation: Int, reverseLayout: Boolean) : super(context, orientation, reverseLayout) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {}

    override fun canScrollVertically(): Boolean {
        return mCanScrollVertically
    }

    fun setVerticalScrollable(verticalScroll: Boolean) {
        mCanScrollVertically = verticalScroll
    }
}