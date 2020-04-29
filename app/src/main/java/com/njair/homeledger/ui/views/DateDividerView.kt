package com.njair.homeledger.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.njair.homeledger.R
import com.njair.homeledger.extensions.bindView

class DateDividerView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val tVDateName: TextView by bindView(R.id.textViewDateName)
    private val iVDivider: ImageView by bindView(R.id.imageViewDivider)
    var text: CharSequence
        get() = tVDateName.text
        set(value) { tVDateName.text = value }
    

    init {
        View.inflate(getContext(), R.layout.layout_date_divider, this)
    }
}