package com.njair.homeledger.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.njair.homeledger.R;

public class TransactionView extends LinearLayout {
    public TransactionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(getContext(), R.layout.view_transaction,this);
    }
}
