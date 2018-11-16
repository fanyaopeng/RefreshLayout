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
import android.view.View;
import android.widget.LinearLayout;

/**
 * Created by huisoucw on 2018/10/19.
 */

public class HeadView extends LinearLayout {
    private float mMaxTranslate;
    private int mDuration = 2500;
    private ValueAnimator mFirstAnimToBottom;
    private ValueAnimator mFirstAnimToTop;

    private ValueAnimator mSecondAnimToBottom;
    private ValueAnimator mSecondAnimToTop;

    private ValueAnimator mThirdAnimToBottom;
    private ValueAnimator mThirdAnimToTop;

    private View first, second, third;

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

        first = red;
        second = yellow;
        third = green;
        setAnim();
    }

    private void setAnim() {
        mFirstAnimToBottom = ValueAnimator.ofFloat(0, mMaxTranslate).setDuration(mDuration);
        mFirstAnimToTop = ValueAnimator.ofFloat(mMaxTranslate, 0).setDuration(mDuration);
        mFirstAnimToBottom.addUpdateListener(mFirstListener);
        mFirstAnimToTop.addUpdateListener(mFirstListener);

        mSecondAnimToBottom = ValueAnimator.ofFloat(0, mMaxTranslate).setDuration(mDuration);
        mSecondAnimToTop = ValueAnimator.ofFloat(mMaxTranslate, 0).setDuration(mDuration);
        mSecondAnimToBottom.addUpdateListener(mSecondListener);
        mSecondAnimToTop.addUpdateListener(mSecondListener);


        mThirdAnimToBottom = ValueAnimator.ofFloat(0, mMaxTranslate).setDuration(mDuration);
        mThirdAnimToTop = ValueAnimator.ofFloat(mMaxTranslate, 0).setDuration(mDuration);
        mThirdAnimToBottom.addUpdateListener(mThirdListener);
        mThirdAnimToTop.addUpdateListener(mThirdListener);

        animFirstViewToBottom();
    }

    private ValueAnimator.AnimatorUpdateListener mFirstListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            first.setTranslationY((Float) animation.getAnimatedValue());
            if (first.getTranslationY() >= mMaxTranslate / 2 && getChildAt(1).getTranslationY() == 0) {
                animSecondViewToBottom();
            }
            if (animation.getAnimatedFraction() == 1.0f && first.getTranslationY() == mMaxTranslate) {
                animFirstViewToTop();
            }
        }
    };
    private ValueAnimator.AnimatorUpdateListener mSecondListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            second.setTranslationY((Float) animation.getAnimatedValue());
            if (second.getTranslationY() >= mMaxTranslate / 2 && getChildAt(2).getTranslationY() == 0) {
                animThirdViewToBottom();
            }
            if (animation.getAnimatedFraction() == 1.0f && second.getTranslationY() == mMaxTranslate) {
                animSecondViewToTop();
            }
        }
    };

    private ValueAnimator.AnimatorUpdateListener mThirdListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            third.setTranslationY((Float) animation.getAnimatedValue());
            if (animation.getAnimatedFraction() == 1.0f) {
                if (third.getTranslationY() == mMaxTranslate) {
                    animThirdViewToTop();
                }
                if (third.getTranslationY() == 0) {
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            animFirstViewToBottom();
                        }
                    }, 500);
                }
            }
        }
    };

    private void animFirstViewToBottom() {
        mFirstAnimToBottom.start();
    }

    private void animFirstViewToTop() {
        mFirstAnimToTop.start();
    }


    private void animSecondViewToBottom() {
        mSecondAnimToBottom.start();
    }

    private void animSecondViewToTop() {
        mSecondAnimToTop.start();
    }

    private void animThirdViewToBottom() {
        mThirdAnimToBottom.start();
    }


    private void animThirdViewToTop() {
        mThirdAnimToTop.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), (int) (getMeasuredHeight() + mMaxTranslate));
    }
}
