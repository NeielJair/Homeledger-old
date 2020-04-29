package com.njair.homeledger.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import androidx.annotation.StyleableRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.njair.homeledger.R
import com.njair.homeledger.extensions.dpToPx

class ToggleView : AppCompatTextView {
    var selectedEntry = ENTRY_FIRST
        private set
    
    var onToggle: ((Boolean) -> Unit)? = null
    
    private var entryFirst = "First"
    private var entrySecond = "Second"
    private var onClickListener: OnClickListener? = null

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        if (!isInEditMode){
            readAttrs(context, attrs)
            init(context)
        } else
            initEditMode()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        if (!isInEditMode){
            readAttrs(context, attrs)
            init(context)
        } else
            initEditMode()
    }

    constructor(context: Context) : super(context) {
        if (!isInEditMode){
            init(context)
        } else
            initEditMode()
    }

    private fun readAttrs(context: Context, attrs: AttributeSet) {
        val set = intArrayOf(
            R.attr.entry_first,
            R.attr.entry_second,
            android.R.attr.textSize,
            android.R.attr.textColor
        )
        
        @StyleableRes
        var i = 0
        val defColor = resources.getColor(android.R.color.primary_text_light)
        val defMargin: Int = 8.dpToPx()
        val a = context.obtainStyledAttributes(attrs, set)
        
        if (a.hasValue(i))
            entryFirst = a.getString(i)!!
        
        if (a.hasValue(++i))
            entrySecond = a.getString(i)!!
        
        val textSize = a.getDimensionPixelSize(++i, 16)
        val color = a.getColor(++i, defColor)
        a.recycle()
    
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize.toFloat())
        compoundDrawablePadding = defMargin
        gravity = Gravity.CENTER_VERTICAL or Gravity.START
    }

    @SuppressLint("SetTextI18n")
    private fun init(context: Context) {
        isFocusable = true
        isClickable = true
        text = entryFirst
        
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_swap_horiz_black_24dp)
        
        drawable?.setTint(resources.getColor(android.R.color.tertiary_text_dark))
        
        setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
        
        super.setOnClickListener { v ->
            onClickListener?.onClick(v)
            toggle()
        }
    }
    
    @SuppressLint("SetTextI18n")
    private fun initEditMode() {
        text = "Toggle View"
    }

    private fun toggle() {
        selectedEntry = !selectedEntry
        text = if (selectedEntry == ENTRY_FIRST) entryFirst else entrySecond
        onToggle?.invoke(selectedEntry)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        onClickListener = l
    }

    fun setSelection(selection: Boolean) {
        if (selection != selectedEntry)
            toggle()
    }

    companion object {
        var ENTRY_FIRST = true
        var ENTRY_SECOND = false
    }
}