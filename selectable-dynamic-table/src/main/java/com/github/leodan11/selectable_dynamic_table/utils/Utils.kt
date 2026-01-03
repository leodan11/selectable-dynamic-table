package com.github.leodan11.selectable_dynamic_table.utils

import android.view.View


class Utils {

    companion object {

        fun getHeightOfView(contentView: View): Int {
            contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            return contentView.measuredHeight
        }

        fun getWidthOfView(contentView: View): Int {
            contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            return contentView.measuredWidth
        }

        fun getOptimizedWidth(headerWidth: Int, dataWidth: Int): Int {
            return when (headerWidth.coerceAtLeast(dataWidth)) {
                in 0..200 -> 200
                in 200..400 -> 400
                in 400..600 -> 600
                in 600..800 -> 800
                in 800..1000 -> 1000
                else -> headerWidth.coerceAtLeast(dataWidth)
            }
        }

    }
}