/*
*Copyright 2023 Fazel Heidari
*
*Licensed under the Apache License, Version 2.0 (the "License");
*you may not use this file except in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing, software
*distributed under the License is distributed on an "AS IS" BASIS,
*WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*See the License for the specific language governing permissions and
*limitations under the License.
 */
package com.github.leodan11.selectable_dynamic_table

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.github.leodan11.selectable_dynamic_table.databinding.TableValueRowBinding
import com.github.leodan11.selectable_dynamic_table.databinding.TableViewBinding
import com.github.leodan11.selectable_dynamic_table.listeners.TableSelectStatusChangeListener
import com.github.leodan11.selectable_dynamic_table.model.Cell
import com.github.leodan11.selectable_dynamic_table.sync_scroll.IScrollListener
import com.github.leodan11.selectable_dynamic_table.sync_scroll.ObservableScrollView
import com.github.leodan11.selectable_dynamic_table.utils.Utils.Companion.getHeightOfView
import com.github.leodan11.selectable_dynamic_table.utils.Utils.Companion.getOptimizedWidth
import com.github.leodan11.selectable_dynamic_table.utils.Utils.Companion.getWidthOfView
import com.google.android.material.card.MaterialCardView
import kotlin.math.roundToInt
import androidx.core.view.isNotEmpty


class TableView : ConstraintLayout, IScrollListener, TableSelectStatusChangeListener {

    private var binding = TableViewBinding.inflate(LayoutInflater.from(context), this, false)
    private var headers: MutableList<String> = ArrayList()
    private var tableData: MutableList<List<Cell>> = ArrayList()
    private var maxVisibleItems: Int = DEF_MAX_VISIBLE_ITEMS
    private var selectedRowColor: Int =
        context.customColorResource(androidx.appcompat.R.attr.colorPrimaryDark)
    private var selectedCellColor: Int =
        context.customColorResource(com.google.android.material.R.attr.colorSecondary)
    private var headerTextColor: Int =
        context.customColorResource(com.google.android.material.R.attr.colorOutline)
    private var indexesTextColor: Int =
        context.customColorResource(com.google.android.material.R.attr.colorOutline)
    private var cellsTextColor: Int =
        context.customColorResource(com.google.android.material.R.attr.colorOnSurface)
    private var selectedCellsTextColor: Int =
        context.customColorResource(com.google.android.material.R.attr.colorSurface)
    private var lastSelectedCell: View? = null
    private var lastSelectedRow: TableRow? = null
    private var lastSelectedIndex: TableRow? = null
    private var rowsHeight: Int = LayoutParams.WRAP_CONTENT
    private var headerHeight: Int = LayoutParams.WRAP_CONTENT
    private var hasBorder: Boolean = true
    private var borderColor =
        context.customColorResource(androidx.appcompat.R.attr.colorPrimaryDark)
    private var rowSelectChangeListener: ((isSelected: Boolean, rowData: List<Cell>, post: Int) -> Unit)? =
        null
    private var cellSelectChangeListener: ((isSelected: Boolean, cell: Cell) -> Unit)? = null
    private var selectableRow: Boolean = true
    private var selectableCell: Boolean = true

    init {
        addView(binding.root)
    }

    constructor(context: Context) : super(context) {
        initIndicators(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initIndicators(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initIndicators(context, attrs, defStyleAttr, 0)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initIndicators(context, attrs, defStyleAttr, defStyleRes)
    }

    /**
     * attrs are attributes used to build the layout parameters
     */
    private fun initIndicators(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {

        /**
         * Get TypedArray holding the attribute values in set that are listed in attrs.
         * Default style specified by defStyleAttr and defStyleRes
         * defStyleAttr contains a reference to a style resource that supplies defaults values for attributes
         * defStyleRes is resource identifier of a style resource that supplies default values for the attributes,
         * used only if defStyleAttr is 0 or can not be found in the theme. Can be 0 to not look for defaults.
         */
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.TableView, defStyleAttr, defStyleRes)

        try {
            // Layout
            rowsHeight = typedArray.getDimensionPixelSize(
                R.styleable.TableView_row_height,
                LayoutParams.MATCH_PARENT
            )
            headerHeight = typedArray.getDimensionPixelSize(
                R.styleable.TableView_header_height,
                LayoutParams.WRAP_CONTENT
            )

            // Border
            hasBorder = typedArray.getBoolean(R.styleable.TableView_show_border, true)
            borderColor = typedArray.getColor(R.styleable.TableView_border_color, borderColor)

            // Header
            val indexHeaderText =
                typedArray.getString(R.styleable.TableView_index_header_text)
            headerTextColor =
                typedArray.getColor(R.styleable.TableView_header_text_color, headerTextColor)

            // Index column
            indexesTextColor =
                typedArray.getColor(R.styleable.TableView_index_text_color, indexesTextColor)

            // Cells
            cellsTextColor =
                typedArray.getColor(R.styleable.TableView_cell_text_color, cellsTextColor)
            selectedCellColor = typedArray.getColor(
                R.styleable.TableView_cell_selected_background_color,
                selectedCellColor
            )
            selectedRowColor = typedArray.getColor(
                R.styleable.TableView_row_selected_background_color,
                selectedRowColor
            )
            selectedCellsTextColor = typedArray.getColor(
                R.styleable.TableView_selected_text_color,
                selectedCellsTextColor
            )

            binding.indexesHeaderTitle.apply {
                indexHeaderText?.let {
                    text = it
                }
                setTextColor(headerTextColor)
            }

        } finally {
            typedArray.recycle()
        }

        setScrollListeners()
    }

    /**
     * Called to do add listeners to indexes and data table ObservableScrollView to sync
     * they scroll together.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setScrollListeners() {

        binding.dataTableScroll.setScrollViewListener(this)
        binding.indexesScroll.setScrollViewListener(this)

    }

    /**
     * Called after indexes or data table scrolled, and gives us its new position so we can scroll
     * the other one to it's position.
     */
    override fun onScrollChanged(
        scrollView: ObservableScrollView?,
        x: Int,
        y: Int,
        oldX: Int,
        oldY: Int
    ) {
        if (scrollView == binding.dataTableScroll) {
            binding.indexesScroll.scrollTo(x, y)
        } else if (scrollView == binding.indexesScroll) {
            binding.dataTableScroll.scrollTo(x, y)
        }
    }

    /**
     * Called to do add data provided by user to the table.
     */
    private fun loadData() {

        val rows = tableData.size

        for (i in 0 until rows) {

            // add table row
            val tr = TableRow(context)

            //set number of row as id to table row
            tr.id = i

            val trParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            tr.layoutParams = trParams

            //add each row's cell
            val row = tableData[i]
            for (element in row) {
                addCell(tr, element, i)
            }

            //add row to the table
            binding.tableData.addView(tr, trParams)

            //load each row's corresponding index to indexes table
            loadIndex(i, getHeightOfView(tr))


        }

        loadHeaders()
    }

    /**
     * Called to do add rows corresponding index to indexes table.
     */
    private fun addCell(tr: TableRow, cell: Cell, index: Int) {

        val cellView: View
        val cellItem = TableValueRowBinding.inflate(LayoutInflater.from(context))

        //apply style to data table cell
        cellItem.apply {
            tableCellContent.text = cell.mContent
            tableCellContent.setTextColor(cellsTextColor)

            if (!hasBorder) {
                root.strokeColor =
                    ContextCompat.getColor(context, android.R.color.transparent)
            } else {
                if (borderColor != context.customColorResource(androidx.appcompat.R.attr.colorPrimaryDark)) {
                    root.strokeColor = borderColor
                }
            }

            cellView = root
        }

        //set index as id to cell
        cellView.id = index

        //add cell to table row
        tr.addView(cellView)

        if (rowsHeight != LayoutParams.MATCH_PARENT)
            cellView.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                rowsHeight
            )

        //set click listener type based on user given options
        if (selectableRow && !selectableCell)
            cellView.setOnClickListener {
                tableRowSelected(tr)
            }
        else if (selectableCell)
            cellView.setOnClickListener {
                tableCellSelected(cellView, cell)
            }

        //enable long click listener if user enabled both selectable row and cell
        if (selectableRow)
            cellView.setOnLongClickListener {
                tableRowSelected(tr)
                true
            }
    }

    /**
     * Called to do add rows corresponding index to indexes table.
     *
     * @param index is the index number,
     * @param height is the height of the corresponding data table row
     */
    @SuppressLint("SetTextI18n")
    private fun loadIndex(index: Int, height: Int) {

        // index column
        val indexRow = TableValueRowBinding.inflate(LayoutInflater.from(context))

        val trParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )

        //apply style to indexes table cell
        indexRow.apply {
            root.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                height
            )
            tableCellContent.text = (index + 1).toString()
            tableCellContent.setTextColor(indexesTextColor)

            if (!hasBorder) {
                root.strokeColor = ContextCompat.getColor(context, android.R.color.transparent)
            } else {
                if (borderColor != context.customColorResource(androidx.appcompat.R.attr.colorPrimaryDark)) {
                    root.strokeColor = borderColor
                }
            }
        }

        //enable selectable row based on user config
        if (selectableRow)
            indexRow.root.setOnClickListener {
                val dataRow = binding.tableData.getChildAt(index) as TableRow
                tableRowSelected(dataRow)
            }

        // add indexes table row
        val trIndex = TableRow(context)
        trIndex.addView(indexRow.root)
        binding.tableIndexes.addView(trIndex, trParams)
    }

    /**
     * Called to do add header titles to header table.
     */
    private fun loadHeaders() {

        // add table row
        val trHeaders = TableRow(context)
        val trParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        trHeaders.layoutParams = trParams


        for (i in headers.indices) {
            val headerCell = TableValueRowBinding.inflate(LayoutInflater.from(context))

            //apply style to indexes table cell
            headerCell.apply {
                if (!hasBorder) {
                    root.strokeColor =
                        ContextCompat.getColor(context, android.R.color.transparent)
                } else {
                    if (borderColor != context.customColorResource(androidx.appcompat.R.attr.colorPrimaryDark)) {
                        root.strokeColor = borderColor
                    }
                }

                tableCellContent.text = headers[i]
                tableCellContent.setTextColor(headerTextColor)
                root.layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    headerHeight
                )
            }

            trHeaders.addView(headerCell.root)
        }
        binding.tableHeaders.addView(trHeaders, trParams)
    }

    /**
     * Called to do the proper action after a row select or unselected.
     *
     * @param cellView is the table cell that is select or unselected.
     */
    private fun tableCellSelected(cellView: View, cellData: Cell) {

        //if there is a selected row, unselect it and let listeners know
        if (lastSelectedRow != null) {

            rowSelectChangeListener?.invoke(
                false,
                tableData[(lastSelectedRow as TableRow).id],
                (lastSelectedRow as TableRow).id
            )

            lastSelectedRow?.let { changeRowStyle(lastSelectedRow!!, false) }

            lastSelectedIndex
                ?.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        }

        if (cellView.isSelected) {

            lastSelectedCell = null
            changeCellStyle(cellView, isCellSelected = false, isRowSelected = false)
            cellSelectChangeListener?.invoke(false, cellData)

        } else {

            //unselect last selected cell
            lastSelectedCell?.let {
                changeCellStyle(lastSelectedCell!!, isCellSelected = false, isRowSelected = false)
            }

            lastSelectedCell = cellView
            changeCellStyle(cellView, isCellSelected = true, isRowSelected = false)
            cellSelectChangeListener?.invoke(true, cellData)

        }

    }

    /**
     * Called to do the proper action after a row select or unselected.
     * @param tr is the table row that is select or unselected.
     */
    private fun tableRowSelected(tr: TableRow) {

        //if there is a selected cell, unselect it and let listeners know
        if (lastSelectedCell != null) {
            lastSelectedCell?.let {
                changeCellStyle(
                    lastSelectedCell!!,
                    isCellSelected = false,
                    isRowSelected = false
                )
            }
            lastSelectedCell = null
        }

        if (tr.isSelected) {
            rowSelectChangeListener?.invoke(false, tableData[tr.id], tr.id)
            changeRowStyle(lastSelectedRow!!, false)
        } else {

            //unselect selected row
            lastSelectedRow?.let { changeRowStyle(lastSelectedRow!!, false) }

            changeRowStyle(tr, true)

            //get row data to put in listener
            val rowData = tableData[tr.id]
            rowSelectChangeListener?.invoke(true, rowData, tr.id)


        }

    }

    /**
     * Called to change table row's status between selected/unselected and do the
     * proper changes after a row select or unselected.
     *
     * @param tr is the table row that it's state should change,
     * @param isSelected is the status of the row,
     */
    private fun changeRowStyle(tr: TableRow, isSelected: Boolean) {

        if (isSelected) {

            //change selected row style
            tr.apply {
                tr.isSelected = true
                setBackgroundColor(selectedRowColor)
            }

            //change corresponding index style
            binding.tableIndexes.getChildAt(tr.id).apply {
                (findViewById<TextView>(R.id.table_cell_content)!!).apply {
                    setTextColor(selectedCellsTextColor)
                    setTypeface(null, Typeface.BOLD)
                }
                setBackgroundColor(selectedRowColor)
            }

            //change selected row cell's style
            for (i in 0..tr.childCount) {
                val cell = tr.getChildAt(i)

                if (cell != null)
                    changeCellStyle(cell, isCellSelected = true, isRowSelected = true)
            }

            lastSelectedRow = tr
        } else {

            //change selected row style
            tr.apply {
                this.isSelected = false
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
            }

            //change corresponding index style
            binding.tableIndexes.getChildAt(tr.id).apply {
                (findViewById<TextView>(R.id.table_cell_content)).apply {
                    setTextColor(indexesTextColor)
                    setTypeface(null, Typeface.NORMAL)
                }
                setBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        android.R.color.transparent
                    )
                )
            }

            //change unselected row cell's style
            for (i in 0..tr.childCount) {
                val cell = tr.getChildAt(i)

                if (cell != null)
                    changeCellStyle(cell, isCellSelected = false, isRowSelected = false)
            }

            lastSelectedRow = null
        }


    }

    /**
     * Called to change table cell's status between selected/unselected and do the
     * proper changes after a cell select or unselected.
     *
     * @param cellView is the view that it's state should change,
     * @param isCellSelected is the status of the cell,
     * @param isRowSelected is the status of the cell view's row
     */
    private fun changeCellStyle(cellView: View, isCellSelected: Boolean, isRowSelected: Boolean) {

        if (isCellSelected) {

            (cellView.findViewById<TextView>(R.id.table_cell_content)!!).apply {
                setTextColor(selectedCellsTextColor)
                setTypeface(null, Typeface.BOLD)
            }

            //prevent cell background color change when row selected
            if (!isRowSelected)
                cellView.setBackgroundColor(selectedCellColor)
            cellView.isSelected = true


        } else {
            (cellView.findViewById<TextView>(R.id.table_cell_content)).apply {
                setTextColor(cellsTextColor)
                setTypeface(null, Typeface.NORMAL)
            }

            cellView.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    android.R.color.transparent
                )
            )

            cellView.isSelected = false

        }

    }

    /**
     * Called to equalize the width of table headers with the data table or index table cells.
     */
    private fun optimizeDataAndHeaderTable() {

        //get headers row
        val dataRow = binding.tableData.getChildAt(binding.tableData.childCount - 1) as TableRow

        //get indexes table header(AppCompatTextView)
        binding.indexesHeaderTitle.height = getHeightOfView(binding.tableHeaders)

        Handler(Looper.getMainLooper()).postDelayed({

            try {
                if (headers.isNotEmpty())


                    for (i in 0 until dataRow.childCount) {

                        val headerItem =
                            (binding.tableHeaders.getChildAt(0) as TableRow).getChildAt(i) as MaterialCardView
                        val dataItem =
                            (binding.tableData.getChildAt(0) as TableRow).getChildAt(i) as MaterialCardView

                        //compare each header cell to its corresponding data table cell items and equalize
                        // width based on optimized width
                        if (headerItem.width != dataItem.width) {
                            dataItem.post {
                                dataItem.layoutParams = TableRow.LayoutParams(
                                    getOptimizedWidth(headerItem.width, dataItem.width),
                                    dataItem.measuredHeight
                                )
                            }

                            headerItem.post {
                                headerItem.layoutParams = TableRow.LayoutParams(
                                    getOptimizedWidth(headerItem.width, dataItem.width),
                                    headerItem.measuredHeight
                                )
                            }
                        }

                    }
            } catch (e: Exception) {
                Log.d("SDT", e.stackTraceToString())
            }


        }, 100)

    }

    /**
     * Called to equalize the height of table based on @param maxVisibleItems:
     * check's the height of table and if its visible items more than maxVisibleItems, change
     * the table height.
     */
    private fun optimizeTableHeight() {

        if (tableData.size > maxVisibleItems && binding.tableData.isNotEmpty()) {
            val dataRowHeight = getHeightOfView(binding.tableData.getChildAt(0))
            val headerHeight = getHeightOfView(binding.tableHeaders)
            val tableHeight: Int =
                (headerHeight + (dataRowHeight * (maxVisibleItems - 0.5))).roundToInt()

            binding.root.layoutParams = TableRow.LayoutParams(
                binding.root.measuredWidth,
                tableHeight
            )
        }

    }

//------------------------------------------- data methods  --------------------------------------\\
    /**
     * Called to set headers and data that will be going to show in the table.
     *
     * @param headers is the list of header titles of table,
     * @param data is a list of list of Cell models which should be shown in the data table.
     */
    fun setData(headers: List<String>?, data: List<List<Cell>>) {

        if (tableData.isNotEmpty())
            clearTable()

        if (!headers.isNullOrEmpty())
            this.headers = headers as MutableList<String>
        else
            hideHeader()

        tableData = data as MutableList<List<Cell>>

    }

    /**
     * Called to fill the provided data into the table
     * notice that you should call this function after setData.
     * Also if you using customizing methods, call them before calling this function.
     */
    fun fillToTable() {
        loadData()

        if (maxVisibleItems != DEF_MAX_VISIBLE_ITEMS)
            optimizeTableHeight()

        Handler(Looper.getMainLooper()).postDelayed({

            optimizeDataAndHeaderTable()

            binding.cardLyt.visibility = VISIBLE
            binding.dataTableScroll.smoothScrollTo(0, 0)

        }, 300)

    }

    /**
     * Called to clear table data and views
     */
    fun clearTable() {
        tableData.clear()
        headers = mutableListOf()
        binding.tableData.removeAllViewsInLayout()
        binding.tableHeaders.removeAllViewsInLayout()
        binding.tableIndexes.removeAllViewsInLayout()
        binding.cardLyt.visibility = GONE

    }
//------------------------------------------------------------------------------------------------\\

//------------------------------------------- set Listeners  -------------------------------------\\

    override fun setOnCellSelectChangeListener(cellSelectChangeListener: (isSelected: Boolean, cell: Cell) -> Unit) {
        this.cellSelectChangeListener = cellSelectChangeListener
    }

    override fun setOnRowSelectChangeListener(rowSelectChangeListener: (isSelected: Boolean, rowData: List<Cell>, pos: Int) -> Unit) {
        this.rowSelectChangeListener = rowSelectChangeListener
    }

//------------------------------------------------------------------------------------------------\\

//---------------------------------------- customizing methods  ----------------------------------\\

    /**
     * Hides the header of this table.
     */
    fun hideHeader() {
        val indexWidth = getWidthOfView(binding.indexesHeaderTitleCard)
        binding.tableHeaders.visibility = GONE
        binding.indexesHeaderTitleCard.visibility = GONE

        binding.indexesScroll.layoutParams = LayoutParams(
            indexWidth,
            LayoutParams.WRAP_CONTENT
        )
    }

    /**
     * Hides the indexes of this table.
     */
    fun hideIndexes() {
        binding.indexesParent.visibility = GONE
    }

    /**
     * Hides this table data columns based on given column indexes.
     *
     * @param columnIndexes List of column indexes to hide.
     */
    fun hideColumns(columnIndexes: List<Int>) {

        Handler(Looper.getMainLooper()).postDelayed({
            for (index in columnIndexes) {
                Log.d("////-----", index.toString())
                if ((index - 1) < binding.tableData.childCount &&
                    (index - 1) < binding.tableIndexes.childCount
                ) {
                    val dataRow = binding.tableData.getChildAt(index - 1) as TableRow
                    val indexRow = binding.tableIndexes.getChildAt(index - 1) as TableRow
                    dataRow.visibility = GONE
                    indexRow.visibility = GONE
                }

            }
        }, 100)

    }

    /**
     * Sets the radius to this table.
     *
     * @param radius Float number of the radius.
     */
    fun setRadius(radius: Float) {
        binding.cardLyt.radius = radius
    }

    /**
     * Sets whether the card has border.
     * Call this before fillToTable() function.
     *
     * @param hasBorder Whether the card has border
     *
     */
    fun setHasBorder(hasBorder: Boolean) {
        this.hasBorder = hasBorder
    }

    /**
     * Sets the border color to this table.
     * Call this before fillToTable() function.
     *
     * @param color Color resource to use for border color.
     *
     */
    fun setBorderColorRes(@ColorRes color: Int) {
        setBorderColor(ContextCompat.getColor(context, color))
    }

    /**
     * Sets the border color to this table.
     * Call this before fillToTable() function.
     *
     * @param color Color resource to use for border color.
     *
     */
    fun setBorderColor(@ColorInt color: Int) {
        borderColor = color
        binding.cardLyt.strokeColor = color
        binding.indexesHeaderTitleCard.strokeColor = color
    }

    /**
     * Sets the header background color to this table.
     *
     * @param color Color resource to use as background color.
     */
    fun setHeaderBackgroundColorRes(@ColorRes color: Int) {
        setHeaderBackgroundColor(ContextCompat.getColor(context, color))
    }

    /**
     * Sets the header background color to this table.
     *
     * @param color Color resource to use as background color.
     */
    fun setHeaderBackgroundColor(@ColorInt color: Int) {
        binding.indexesHeaderTitle.setBackgroundColor(color)
        binding.tableHeaders.setBackgroundColor(color)
    }

    /**
     * Sets the header cell's text color to this table.
     * Call this before fillToTable() function.
     *
     * @param color Color resource to use for text color.
     */
    fun setHeaderTextColorRes(@ColorRes color: Int) {
        setHeaderTextColor(ContextCompat.getColor(context, color))
    }

    /**
     * Sets the header cell's text color to this table.
     * Call this before fillToTable() function.
     *
     * @param color Color resource to use for text color.
     */
    fun setHeaderTextColor(@ColorInt color: Int) {
        binding.indexesHeaderTitle.setTextColor(color)
        headerTextColor = color
    }

    /**
     * Sets the indexes background color.
     *
     * @param color Color resource to use as background color.
     */
    fun setIndexesBackgroundColorRes(@ColorRes color: Int) {
        setIndexesBackgroundColor(ContextCompat.getColor(context, color))
    }

    /**
     * Sets the indexes background color.
     *
     * @param color Color resource to use as background color.
     */
    fun setIndexesBackgroundColor(@ColorInt color: Int) {
        binding.tableIndexes.setBackgroundColor(color)
    }

    /**
     * Sets the index cell's text color to this table.
     * Call this before fillToTable() function.
     *
     * @param color Color resource to use for text color.
     */
    fun setIndexesTextColorRes(@ColorRes color: Int) {
        setIndexesTextColor(ContextCompat.getColor(context, color))
    }

    /**
     * Sets the index cell's text color to this table.
     * Call this before fillToTable() function.
     *
     * @param color Color resource to use for text color.
     */
    fun setIndexesTextColor(@ColorInt color: Int) {
        indexesTextColor = color
    }

    /**
     * Sets the data's background color.
     *
     * @param color Color resource to use as background color.
     */
    fun setDataTableBackgroundColorRes(@ColorRes color: Int) {
        setDataTableBackgroundColor(ContextCompat.getColor(context, color))
    }

    /**
     * Sets the data's background color.
     *
     * @param color Color resource to use as background color.
     */
    fun setDataTableBackgroundColor(@ColorInt color: Int) {
        binding.tableData.setBackgroundColor(color)
    }

    /**
     * Sets the data cell's text color to this table.
     * Call this before fillToTable() function.
     *
     * @param color Color resource to use for text color.
     */
    fun setDataCellsTextColorRes(@ColorRes color: Int) {
        setDataCellsTextColor(ContextCompat.getColor(context, color))
    }

    /**
     * Sets the data cell's text color to this table.
     * Call this before fillToTable() function.
     *
     * @param color Color resource to use for text color.
     */
    fun setDataCellsTextColor(@ColorInt color: Int) {
        cellsTextColor = color
    }

    /**
     * Sets the selected data cell's text color to this table.
     *
     * @param color Color resource to use for text color.
     */
    fun setSelectedCellsTextColorRes(@ColorRes color: Int) {
        setSelectedCellsTextColor(ContextCompat.getColor(context, color))
    }

    /**
     * Sets the selected data cell's text color to this table.
     *
     * @param color Color resource to use for text color.
     */
    fun setSelectedCellsTextColor(@ColorInt color: Int) {
        selectedCellsTextColor = color
    }

    /**
     * Sets the selected row's background color to this table.
     *
     * @param color Color resource to use as background color.
     */
    fun setSelectedRowColorRes(@ColorRes color: Int) {
        setSelectedRowColor(ContextCompat.getColor(context, color))
    }

    /**
     * Sets the selected row's background color to this table.
     *
     * @param color Color resource to use as background color.
     */
    fun setSelectedRowColor(@ColorInt color: Int) {
        selectedRowColor = color
    }

    /**
     * Sets the selected cell's background color to this table.
     *
     * @param color Color resource to use as background color.
     */
    fun setSelectedCellColorRes(@ColorRes color: Int) {
        setSelectedCellColor(ContextCompat.getColor(context, color))
    }

    /**
     * Sets the selected cell's background color to this table.
     *
     * @param color Color resource to use as background color.
     */
    fun setSelectedCellColor(@ColorInt color: Int) {
        selectedCellColor = color
    }

    /**
     * Sets the indexes general title to this table.
     *
     * @param indexesTitle String resource to use as indexes title.
     */
    fun setIndexesTitle(@StringRes indexesTitle: Int) {
        setIndexesTitle(ContextCompat.getString(context, indexesTitle))
    }

    /**
     * Sets the indexes general title to this table.
     *
     * @param indexesTitle String resource to use as indexes title.
     */
    fun setIndexesTitle(indexesTitle: String) {
        binding.indexesHeaderTitle.text = indexesTitle
    }

    /**
     * Sets the indexes width to this table.
     * Call this before fillToTable() function.
     *
     * @param width is the desired width to set to the indexes.
     */
    fun setIndexesTitleWidth(width: Int) {
        binding.indexesHeaderTitle.width = width
    }

    /**
     * Sets the headers height to this table.
     * Call this after fillToTable() function.
     *
     * @param height is the desired height to set to the headers.
     *
     */
    fun setHeaderHeight(height: Int) {
        val headerRow = binding.tableHeaders.getChildAt(0) as TableRow


        for (i in 0 until headerRow.childCount) {
            val indexItem = headerRow.getChildAt(i) as MaterialCardView
            indexItem.layoutParams = TableRow.LayoutParams(
                indexItem.measuredWidth,
                height
            )
        }

        binding.indexesHeaderTitleCard.layoutParams = TableRow.LayoutParams(
            binding.indexesHeaderTitleCard.measuredWidth,
            height
        )
    }

    /**
     * Sets the rows height to this table.
     * Call this before fillToTable() function.
     *
     * @param height is the desired height to set to the rows.
     *
     */
    fun setRowsHeight(height: Int) {
        rowsHeight = height
    }

    /**
     * Sets the max visible items to show in the this table, if the table rows was more than given
     * number then table would be scrollable.
     *
     * @param maxVisibleItems is the desired number of max visible rows.
     */
    fun setMaxVisibleItems(maxVisibleItems: Int) {
        this.maxVisibleItems = maxVisibleItems
        optimizeTableHeight()
    }

    /**
     * Sets the selectable status for the cells and the rows to this table.
     * Call this before fillToTable() function.
     *
     * @param selectableRow is the determine that the rows are selectable or not,
     * @param selectableCell is the determine that the cells are selectable or not.
     */
    fun setSelectingStatus(selectableRow: Boolean, selectableCell: Boolean) {
        this.selectableRow = selectableRow
        this.selectableCell = selectableCell
    }

    private fun Context.customColorResource(@AttrRes idAttrRes: Int, fallbackColor: Int = 0): Int {
        val typedValue = TypedValue()
        val resolved = this.theme.resolveAttribute(idAttrRes, typedValue, true)
        return if (resolved) typedValue.data else fallbackColor
    }

    companion object {
        const val DEF_MAX_VISIBLE_ITEMS = -1
    }

//------------------------------------------------------------------------------------------------\\
}

