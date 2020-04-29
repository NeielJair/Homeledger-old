package com.njair.homeledger.ui.views

import android.animation.ObjectAnimator
import android.content.Context
import android.content.DialogInterface
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.addListener
import androidx.core.widget.NestedScrollView
import com.njair.homeledger.GroupMain
import com.njair.homeledger.R
import com.njair.homeledger.extensions.bindView
import com.njair.homeledger.extensions.dpToPx

class DialogDrawer(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs), DialogInterface {
    private val inflater: LayoutInflater by lazy { LayoutInflater.from(context) }
    
    private var onSlideListener: OnSlideListener?      = null
    private var refreshMain: RefreshMethod?            = null
    private var refreshSecondary: RefreshMethod?       = null
    private var onDisplayMain: OnDisplayListener?      = null
    private var onDisplaySecondary: OnDisplayListener? = null
    
    private var slideAnimationDuration = 100
    
    var visible = false
        private set
    
    private var animState: Int        = ANIM_CLOSED
    private var isCancelable          = true
    private var refreshOnSlideUp      = true
    private var lockHeight            = true
    private var defaultMinHeight: Int = 0
    var totalMaxHeight: Int = 0
        set(value) {
            field = value
            
            if (height > totalMaxHeight)
                forceHeight(totalMaxHeight)
        }
    
    private val textViewTitleMain: TextView      by bindView(R.id.textViewTitleMain)
    private val textViewTitleDivider: TextView   by bindView(R.id.textViewTitleDivider)
    private val textViewTitleSecondary: TextView by bindView(R.id.textViewTitleSecondary)
    private val imageViewGrayBar: ImageView      by bindView(R.id.imageViewGrayBar)
    
    var mainView: View? = null
        private set
    var secondaryView: View? = null
        private set
    
    private val contentList: MutableList<Content> = mutableListOf()
    private var contentIndex = -1
    private val currentContent
        get() = contentList[contentIndex]
    
    
    private val topLayout: ConstraintLayout  by bindView(R.id.topLayout)
    private val scrollView: NestedScrollView by bindView(R.id.scrollView)
    private val innerLayout: LinearLayout    by bindView(R.id.linearLayout)
    
    private val buttonPositive: Button   by bindView(R.id.buttonPositive)
    private val buttonNeutral: Button    by bindView(R.id.buttonNeutral)
    private val buttonNegative: Button   by bindView(R.id.buttonNegative)
    private val buttonDismiss: ImageView by bindView(R.id.imageViewDismiss)
    
    var parentActivity: GroupMain? = null
    
    private val paint = Paint().apply { color = Color.WHITE }
    
    override fun cancel() = close()
    override fun dismiss() = close()

    fun slideUp() {
        if (visible)
            return
        
        val animation = ObjectAnimator.ofFloat(this, "translationY", 0f)
        animation.duration = slideAnimationDuration.toLong()
        animation.addListener ( onEnd = { animState = ANIM_OPEN } )
        animation.start()
        visible = true
        visibility = View.VISIBLE
        
        if (refreshOnSlideUp)
            refresh()
        
        onSlideListener?.onSlideUp()
        
        onDisplay()
    
        forceHeight(defaultMinHeight)
    }

    fun slideDown() {
        if (!visible)
            return
        
        val animation = ObjectAnimator.ofFloat(this, "translationY", height.toFloat())
        animation.duration = slideAnimationDuration.toLong()
        animation.addListener(
            onEnd = {
                visible = false
                visibility = View.GONE
                onSlideListener?.onClose()
            }
        )
        animation.start()
        onSlideListener?.onSlideDown()
    }

    fun close() {
        if (!visible)
            return
        
        val animation = ObjectAnimator.ofFloat(this, "translationY", height.toFloat())
        animation.duration = 0
        animation.start()
        visible = false
        visibility = View.GONE
        animState = ANIM_CLOSED
        onSlideListener?.onClose()
    }

    private fun refresh() {
        contentList.forEach { it.onRefresh (this, innerLayout) }
        
        /*refreshMain?.refresh(this, innerLayout)
        refreshSecondary?.refresh(this, innerLayout)*/
    }
    
    private fun onDisplay() {
        if (contentIndex != -1)
            currentContent.onDisplay(currentContent.view)
    }

    fun fullScroll(direction: Int) = scrollView.fullScroll(direction)
    
    fun addContent(vararg contents: Content) {
        contents.forEach {
            contentList.add(it)
            innerLayout.addView(it.view)
    
            if (contentIndex == -1)
                contentIndex = 0
            else{
                it.view.visibility = View.GONE
                it.view.isEnabled  = false
            }
        }
        
        update()
    }
    
    private fun update() {
        when {
            contentList.size == 0 -> {
                textViewTitleMain.visibility = View.GONE
            }
            contentList.size == 1 -> {
                textViewTitleMain.visibility = View.VISIBLE
                textViewTitleMain.text = contentList[0].title
            }
            contentList.size == 2 -> {
                textViewTitleMain.visibility = View.VISIBLE
                textViewTitleMain.text = contentList[0].title
                
                textViewTitleDivider.visibility = View.VISIBLE
                textViewTitleSecondary.visibility = View.VISIBLE
                textViewTitleSecondary.text = contentList[1].title
                
                if (contentIndex == 0) {
                    textViewTitleMain.setTextColor(Color.BLACK)
                    textViewTitleSecondary.setTextColor(Color.GRAY)
    
                    textViewTitleMain.setTypeface(null, Typeface.BOLD)
                    textViewTitleSecondary.setTypeface(null, Typeface.NORMAL)
                } else {
                    textViewTitleMain.setTextColor(Color.GRAY)
                    textViewTitleSecondary.setTextColor(Color.BLACK)
    
                    textViewTitleMain.setTypeface(null, Typeface.NORMAL)
                    textViewTitleSecondary.setTypeface(null, Typeface.BOLD)
                }
            }
            contentList.size > 2 -> 1/0
        }
    
        currentContent.buttonPositive?.let {
            buttonPositive.visibility = View.VISIBLE
            buttonPositive.text = it.first
        }
        currentContent.buttonNeutral?.let {
            buttonNeutral.visibility = View.VISIBLE
            buttonNeutral.text = it.first
        }
        currentContent.buttonNegative?.let {
            buttonNegative.visibility = View.VISIBLE
            buttonNegative.text = it.first
        }
    }
    
    fun setMainContent(context: Context?, layout: Int): DialogDrawer {
        val inflater = LayoutInflater.from(context)
        mainView = inflater.inflate(layout, null, false)
        innerLayout.addView(mainView)
        return this
    }

    fun setSecondaryContent(layout: Int, secondaryTitle: String?): DialogDrawer {
        textViewTitleSecondary.text = secondaryTitle
        textViewTitleMain.setTypeface(null, Typeface.BOLD)
        textViewTitleMain.visibility = View.VISIBLE
        textViewTitleDivider.visibility = View.VISIBLE
        textViewTitleSecondary.visibility = View.VISIBLE
        secondaryView = inflater.inflate(layout, null, false)
        innerLayout.addView(secondaryView)
        secondaryView!!.visibility = View.GONE
        return this
    }

    private fun switchContent(which: Int) {
        if (contentList.size <= 1)
            return
        
        require(which < contentList.size)
        
        currentContent.view.visibility = View.GONE
        currentContent.view.isEnabled  = false
        
        contentIndex = which
        currentContent.view.visibility = View.VISIBLE
        currentContent.view.isEnabled  = true
    
        update()
        onDisplay()
    }

    fun setRefreshMethod(which: Boolean = MAIN, refreshMethod: RefreshMethod): DialogDrawer {
        if (which == MAIN)
            refreshMain = refreshMethod
        else
            refreshSecondary = refreshMethod
        
        refresh()
        return this
    }

    /*fun setRefreshOnSlideUp(refreshOnSlideUp: Boolean): DialogDrawerView {
        this.refreshOnSlideUp = refreshOnSlideUp
        return this
    }*/

    fun setOnDisplayListener(which: Boolean = MAIN, onDisplayListener: OnDisplayListener): DialogDrawer {
        if (which == MAIN)
            onDisplayMain = onDisplayListener
        else
            onDisplaySecondary = onDisplayListener
        return this
    }

    fun setTitle(title: String): DialogDrawer {
        textViewTitleMain.visibility = View.VISIBLE
        textViewTitleMain.text = title
        return this
    }

    /*fun setCancelable(isCancelable: Boolean): DialogDrawerView {
        this.isCancelable = isCancelable
        buttonDismiss.visibility = if (isCancelable) View.VISIBLE else View.GONE
        return this
    }*/

    @Throws(IllegalArgumentException::class)
    fun setButton(whichButton: Int, text: CharSequence, closesAfterClick: Boolean = true, listener: OnClickListener? = null): DialogDrawer {
        val button: Button =
            when (whichButton) {
                DialogInterface.BUTTON_POSITIVE -> buttonPositive
                DialogInterface.BUTTON_NEUTRAL  -> buttonNeutral
                DialogInterface.BUTTON_NEGATIVE -> buttonNegative
                else                            -> throw IllegalArgumentException("illegal button id: $whichButton")
            }
        
        button.text = text
        button.visibility = View.VISIBLE
        button.setOnClickListener { view ->
            if (isCancelable && closesAfterClick)
                slideDown()
            
            listener?.onClick(view)
        }
        return this
    }
    
    @Throws(IllegalArgumentException::class)
    fun setButtonText(whichButton: Int, text: String): DialogDrawer {
        when (whichButton) {
            DialogInterface.BUTTON_POSITIVE -> buttonPositive.text = text
            DialogInterface.BUTTON_NEUTRAL  -> buttonNeutral.text  = text
            DialogInterface.BUTTON_NEGATIVE -> buttonNegative.text = text
            else                            -> throw IllegalArgumentException("illegal button id: $whichButton")
        }
        return this
    }

    @Throws(IllegalArgumentException::class)
    fun setButtonEnabled(whichButton: Int, enabled: Boolean): DialogDrawer {
        when (whichButton) {
            DialogInterface.BUTTON_POSITIVE -> buttonPositive.isEnabled = enabled
            DialogInterface.BUTTON_NEUTRAL  -> buttonNeutral.isEnabled  = enabled
            DialogInterface.BUTTON_NEGATIVE -> buttonNegative.isEnabled = enabled
            else                            -> throw IllegalArgumentException("illegal button id: $whichButton")
        }
        return this
    }

    @Throws(IllegalArgumentException::class)
    fun setButtonVisibility(whichButton: Int, visibility: Int): DialogDrawer {
        when (whichButton) {
            DialogInterface.BUTTON_POSITIVE -> buttonPositive.visibility = visibility
            DialogInterface.BUTTON_NEUTRAL  -> buttonNeutral.visibility  = visibility
            DialogInterface.BUTTON_NEGATIVE -> buttonNegative.visibility = visibility
            else                            -> throw IllegalArgumentException("illegal button id: $whichButton")
        }
        return this
    }

    fun setOnSlideListener(onSlideListener: OnSlideListener): DialogDrawer {
        this.onSlideListener = onSlideListener
        return this
    }
    
    fun forceHeight(newHeight: Int) {
        this.minHeight = newHeight
        this.maxHeight = newHeight
        this.layoutParams.height = newHeight
        this.requestLayout()
    }

    interface OnSlideListener {
        fun onSlideUp()
        fun onSlideDown()
        fun onClose()
    }

    interface OnDisplayListener {
        fun onDisplay(currentView: View?)
    }

    interface RefreshMethod {
        fun refresh(view: DialogDrawer?, layout: LinearLayout?)
    }

    companion object {
        const val MAIN = true
        const val SECONDARY = false
        
        const val ANIM_CLOSED  = 0
        const val ANIM_SLIDING = 1
        const val ANIM_OPEN    = 2
    }

    init {
        View.inflate(context, R.layout.view_dialog_drawer, this)
    
        setWillNotDraw(false)
        
        viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                scrollView.setPadding(0, 0, 0, topLayout.height)
                
                if (lockHeight) {
                    maxHeight = height
                    minHeight = height
                }
    
                defaultMinHeight = height
                if (parentActivity != null)
                    totalMaxHeight = parentActivity!!.fragmentHeight
                
                close()
                visibility = View.GONE
    
                //@SuppressLint("ClickableViewAccessibility")
                imageViewGrayBar.setOnTouchListener { v, event ->
                    val y = event.y.toInt()
        
                    when (event.action) {
                        MotionEvent.ACTION_MOVE -> {
                            val shift = v.y.toInt() - y
                            val topY = height + shift + 8.dpToPx()
                            
                            when {
                                topY <= defaultMinHeight*3/5 -> if (animState != ANIM_SLIDING) slideDown()
                                topY <= defaultMinHeight     -> forceHeight(defaultMinHeight)
                                topY >= totalMaxHeight       -> forceHeight(totalMaxHeight)
                                else                         -> forceHeight(height + shift)
                            }
                        }
                    }
        
                    false
                }
            }
        })
        
        buttonPositive.visibility = View.GONE
        buttonPositive.setOnClickListener {
            currentContent.buttonPositive?.second?.invoke(buttonPositive)
        }
        
        buttonNeutral.visibility = View.GONE
        buttonNeutral.setOnClickListener {
            currentContent.buttonNeutral?.second?.invoke(buttonNeutral)
        }
        
        buttonNegative.visibility = View.GONE
        buttonNegative.setOnClickListener {
            currentContent.buttonNegative?.second?.invoke(buttonNegative)
        }
        
        buttonDismiss.setOnClickListener { slideDown() }
        
        textViewTitleMain.visibility = View.GONE
        textViewTitleDivider.visibility = View.GONE
        textViewTitleSecondary.visibility = View.GONE
        
        textViewTitleMain.setOnClickListener { switchContent(0) }
        textViewTitleSecondary.setOnClickListener { switchContent(1) }
    }
    
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    
        val sVBottomY = scrollView.y + scrollView.height
        
        canvas ?: return
        
        if (sVBottomY < height)
            canvas.drawRect(0f, sVBottomY, width.toFloat(), height.toFloat(), paint)
    }
    
    inner class Content(val layout: Int,
                        val title: String,
                        var onRefresh: (DialogDrawer?, LinearLayout?) -> Unit = { _, _ -> },
                        var onDisplay: (View) -> Unit = {},
                        var buttonPositive: Pair<CharSequence, ((View) -> Unit)?>? = null,
                        var buttonNeutral:  Pair<CharSequence, ((View) -> Unit)?>? = null,
                        var buttonNegative: Pair<CharSequence, ((View) -> Unit)?>? = null) {
        val view: View = LayoutInflater.from(context).inflate(layout, null)
    }
}