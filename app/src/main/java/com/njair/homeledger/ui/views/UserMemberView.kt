package com.njair.homeledger.ui.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.njair.homeledger.R
import com.njair.homeledger.service.Member
import com.njair.homeledger.service.OnTouchImageViewListener
import com.njair.homeledger.extensions.bindView

class UserMemberView(context: Context?, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    var name: String = resources.getString(R.string.member_name)
    var email: String? = null
    var color = Color.RED
    
    private val iVIcon: ImageView     by bindView(R.id.imageViewMemberIcon)
    private val tVName: TextView      by bindView(R.id.textViewMemberName)
    private val iVSettings: ImageView by bindView(R.id.imageViewSettings)
    private val iVLogOut: ImageView   by bindView(R.id.imageViewLogOut)
    
    val defaultColor: Int
        get() = iVLogOut.imageTintList!!.defaultColor
    
    var settingsListener: OnTouchImageViewListener? = null
        set(value) {
            field = value
            settingsListener?.setColor(color)
            iVSettings.setOnTouchListener(value)
        }
    
    var logOutListener: OnTouchImageViewListener? = null
        set(value) {
            field = value
            logOutListener!!.setColor(color)
            iVLogOut.setOnTouchListener(value)
        }
    
    fun setMember(member: Member) {
        name = member.name
        email = member.user.email
        color = member.color
        
        tVName.text = name
        iVIcon.setColorFilter(color)
        iVSettings.setColorFilter(color)
        iVLogOut.setColorFilter(color)
        
        if (member.type == Member.USER)
            iVIcon.setImageResource(R.drawable.ic_face_white_32dp)
        
        settingsListener?.setColor(color)
        logOutListener?.setColor(color)
    }

    init {
        View.inflate(context, R.layout.view_user_member, this)
    }
}