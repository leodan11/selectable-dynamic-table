package com.github.leodan11.selectable_dynamic_table.sync_scroll

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView


class ObservableScrollView : NestedScrollView {
    private var listener: IScrollListener? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    fun setScrollViewListener(listener: IScrollListener?) {
        this.listener = listener
    }

    override fun onScrollChanged(x: Int, y: Int, oldx: Int, oldy: Int) {
        super.onScrollChanged(x, y, oldx, oldy)
        if (listener != null) {
            listener!!.onScrollChanged(this, x, y, oldx, oldy)
        }
    }
}