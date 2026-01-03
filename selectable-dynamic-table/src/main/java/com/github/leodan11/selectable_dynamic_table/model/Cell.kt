package com.github.leodan11.selectable_dynamic_table.model


class Cell
constructor(
    private val mId: String,
    var mContent: String,
    val mData: Any,
) : ISortableModel {

    override fun getId(): String {
        return mId
    }

    override fun getContent(): Any {
        return mData
    }

}