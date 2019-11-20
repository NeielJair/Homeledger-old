package com.njair.homeledger.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.njair.homeledger.R;

public class DialogDrawerView extends LinearLayout {

    public int slideAnimationDuration = 100;
    public boolean visible = false;

    private TextView textViewTitle;
    private ImageView imageViewGrayBar;

    public DialogDrawerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.DialogDrawerView,
                0, 0);

        int innerLayout;
        String okButtonText, cancelButtonText;

        try {
            innerLayout = a.getResourceId(R.styleable.DialogDrawerView_innerLayout, 0);
            okButtonText = a.getString(R.styleable.DialogDrawerView_okButtonText);
            cancelButtonText = a.getString(R.styleable.DialogDrawerView_cancelButtonText);
        } finally {
            a.recycle();
        }

        LayoutInflater inflater = LayoutInflater.from(context);

        //View root = inflate(getContext(), R.layout.view_dialog_drawer,this);
        View root = inflater.inflate(R.layout.view_dialog_drawer, this, true);

        LinearLayout layout = root.findViewById(R.id.linearLayout);

        View inside = inflater.inflate(innerLayout, null, false);
        layout.addView(inside);

        setVisibility(View.GONE);

        post(new Runnable() {
            @Override
            public void run() {
                close();
            }
        });

        ((Button)root.findViewById(R.id.buttonOk)).setText(okButtonText);
        ((Button)root.findViewById(R.id.buttonCancel)).setText(cancelButtonText);
        textViewTitle = root.findViewById(R.id.textViewTitle);
        imageViewGrayBar = root.findViewById(R.id.imageViewGrayBar);

        textViewTitle.setVisibility(View.GONE);
    }

    public void slideUp(){
        if(visible)
            return;

        ObjectAnimator animation = ObjectAnimator.ofFloat(this, "translationY", 0);
        animation.setDuration(slideAnimationDuration);
        animation.start();

        visible = true;
        setVisibility(View.VISIBLE);
    }

    public void slideDown(){
        if(!visible)
                return;

        ObjectAnimator animation = ObjectAnimator.ofFloat(this, "translationY", getHeight());
        animation.setDuration(slideAnimationDuration);
        animation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                visible = false;
                setVisibility(View.GONE);
            }
        });
        animation.start();
    }

    public void close(){
        if(!visible)
            return;

        ObjectAnimator animation = ObjectAnimator.ofFloat(this, "translationY", getHeight());
        animation.setDuration(0);
        animation.start();

        visible = false;
        setVisibility(View.GONE);
    }

    public void setTitle(String title){
        textViewTitle.setVisibility(View.VISIBLE);
        textViewTitle.setText(title);
        imageViewGrayBar.setVisibility(View.GONE);
    }

    public void enableDismiss(){

    }
}
