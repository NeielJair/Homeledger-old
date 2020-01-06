package com.njair.homeledger.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.njair.homeledger.R;

public class UserMemberView extends ConstraintLayout {
    public UserMemberView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(getContext(), R.layout.view_user_member,this);
    }
}
