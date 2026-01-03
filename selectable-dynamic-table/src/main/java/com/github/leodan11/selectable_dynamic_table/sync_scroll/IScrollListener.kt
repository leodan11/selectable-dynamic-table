package com.github.leodan11.selectable_dynamic_table.sync_scroll


interface IScrollListener {
    fun onScrollChanged(scrollView: ObservableScrollView?, x: Int, y: Int, oldX: Int, oldY: Int)
}