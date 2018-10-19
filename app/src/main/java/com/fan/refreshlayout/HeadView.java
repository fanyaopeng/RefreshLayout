package com.fan.refreshlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.LinearLayout;

/**
 * Created by huisoucw on 2018/10/19.
 */

public class HeadView extends LinearLayout {
    private float mMaxTranslate;
    private int mDuration = 300;

    public HeadView(Context context) {
        this(context, null);
    }

    public HeadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutParams params = new LayoutParams(-2, -2);
        params.leftMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, context.getResources().getDisplayMetrics());
        mMaxTranslate = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, context.getResources().getDisplayMetrics());
        CircleView red = new CircleView(context);
        CircleView yellow = new CircleView(context);
        yellow.setColor(Color.YELLOW);
        CircleView green = new CircleView(context);
        green.setColor(Color.GREEN);
        addView(red, params);
        addView(yellow, params);
        addView(green, params);
        setAnim();
    }

    private void setAnim() {
        animFirstViewToBottom();
    }


    private boolean isSecondStart;
    private boolean isThirdStart;

    private void animFirstViewToBottom() {
        CircleView view = (CircleView) getChildAt(0);
        view.animate().translationY(mMaxTranslate).setDuration(mDuration).setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (animation.getAnimatedFraction() >= 0.5f) {
                    if (!isSecondStart) {
                        Log.e("main", "start Second");
                        animSecondViewToBottom();
                        isSecondStart = true;
                    }
                }
                if (animation.getAnimatedFraction() == 1.0f) {
                    Log.e("main", "onAnimationUpdate");
                    animFirstViewToTop();
                    animation.cancel();
                }
            }
        }).start();
    }

    private void animFirstViewToTop() {
        CircleView view = (CircleView) getChildAt(0);
        view.animate().translationY(0).setDuration(mDuration).start();
    }

    private void animSecondViewToBottom() {
        CircleView view = (CircleView) getChildAt(1);
        view.animate().translationY(mMaxTranslate).setDuration(mDuration).setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (animation.getAnimatedFraction() >= 0.5f) {
                    if (!isThirdStart) {
                        Log.e("main", "start third");
                        animThirdViewToBottom();
                        isThirdStart = true;
                    }
                }
                if (animation.getAnimatedFraction() == 1.0f) {
                    animSecondViewToTop();
                }
            }
        }).start();
    }

    private void animSecondViewToTop() {
        CircleView view = (CircleView) getChildAt(1);
        view.animate().translationY(0).setDuration(mDuration).start();
    }

    private void animThirdViewToBottom() {
        CircleView view = (CircleView) getChildAt(2);
        view.animate().translationY(mMaxTranslate).setDuration(mDuration).setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (animation.getAnimatedFraction() == 1.0f) {
                    animThirdViewToTop();
                }
            }
        }).start();
    }

    private void animThirdViewToTop() {
        CircleView view = (CircleView) getChildAt(2);
        view.animate().translationY(0).setDuration(mDuration).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
//                        isSecondStart = false;
//                        isThirdStart = false;
//                        animFirstViewToBottom();
                        Log.e("main", "onAnimationEnd");
                    }
                }, 500);
            }
        }).start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), (int) (getMeasuredHeight() + mMaxTranslate));
    }
}
