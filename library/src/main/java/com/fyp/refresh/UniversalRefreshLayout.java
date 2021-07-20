package com.fyp.refresh;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;

import android.util.AttributeSet;
import android.util.Log;
import android.view.InflateException;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

/**
 * 一个可定制的刷新控件  支持NestedScrolling 控件 等的惯性滑动
 */
public class UniversalRefreshLayout extends ViewGroup {
    private View mChild;
    private FooterView mFooter;
    private HeaderView mHeader;
    private final int mMaxOffset;
    private boolean isLoading;
    private boolean isRefreshing;
    private float mLastY;
    private float mDownY;
    private OnLoadMoreListener mLoadMoreListener;
    private OnRefreshListener mRefreshListener;
    private OnChildScrollListener mChildScrollListener;
    private boolean isEnabledLoadMore;
    private boolean mPendingRefresh;
    private boolean mPendingLoadMore;
    private final int mTouchSlop;
    private final NestedScrollingParentHelper mParentHelper;
    private boolean isAutoLoadMore;
    private final Drawable mFootAnim;
    private final Drawable mHeadAnim;
    private static final String TAG = "UniversalRefreshLayout";

    public UniversalRefreshLayout(Context context) {
        this(context, null);
    }

    public UniversalRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.UniversalRefreshLayoutStyle);
    }

    public UniversalRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.UniversalRefreshLayout, defStyleAttr, R.style.UniversalRefreshLayout);
        mMaxOffset = a.getDimensionPixelOffset(R.styleable.UniversalRefreshLayout_max_scroll_distance, 100);
        mHeadAnim = a.getDrawable(R.styleable.UniversalRefreshLayout_head_anim_frame);
        mFootAnim = a.getDrawable(R.styleable.UniversalRefreshLayout_foot_anim_frame);
        isAutoLoadMore = a.getBoolean(R.styleable.UniversalRefreshLayout_auto_load_more, true);
        a.recycle();

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mParentHelper = new NestedScrollingParentHelper(this);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        for (int i = 0; i < getChildCount(); i++) {
            measureChild(getChildAt(i), widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        top = -mHeader.getMeasuredHeight();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.layout(left, top, right, top + child.getMeasuredHeight());
            top += child.getMeasuredHeight();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 1) {
            throw new InflateException("只能有一个child");
        }
        mChild = getChildAt(0);
        createHeadView();
        createFootView();
    }

    private void createFootView() {
        mFooter = new FooterView(getContext(), mFootAnim);
        addView(mFooter);
    }

    private void createHeadView() {
        mHeader = new HeaderView(getContext(), mHeadAnim);
        addView(mHeader, 0);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            reset();
        }
    }

    public void setAutoLoadMore(boolean autoLoadMore) {
        isAutoLoadMore = autoLoadMore;
    }

    private void reset() {
        smoothScrollTo(0);
        isLoading = false;
        isRefreshing = false;
        mPendingLoadMore = false;
        mPendingRefresh = false;
        setFootAnim(false);
        setHeadAnim(false);
    }

    public boolean isRefreshing() {
        return isRefreshing;
    }

    public boolean isLoading() {
        return isLoading;
    }

    private void setHeadAnim(boolean start) {
        mHeader.setHeadAnim(start);
    }

    private void setFootAnim(boolean start) {
        mFooter.setFootAnim(start);
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        if (listener != null)
            isEnabledLoadMore = true;
        mLoadMoreListener = listener;
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        mRefreshListener = listener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (isLoading || isRefreshing || !isEnabled()) {
            return super.dispatchTouchEvent(ev);
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = ev.getRawY();
                mLastY = mDownY;
                break;
            case MotionEvent.ACTION_MOVE:
                int offset = getScrollY();
                float curY = ev.getRawY();
                float dy = mLastY - curY;
                mLastY = curY;
                if (Math.abs(curY - mDownY) < mTouchSlop) {
                    return super.dispatchTouchEvent(ev);
                }
                float pending = offset + dy;
                boolean intercept = false;
                if (dy < 0) {
                    if (!canChildScrollUp()) {
                        mPendingRefresh = true;
                        intercept = true;
                    }
                    if (mPendingLoadMore) {
                        intercept = true;
                        if (pending < 0) {
                            scrollTo(0, 0);
                            return super.dispatchTouchEvent(ev);
                        }
                    }
                }
                if (dy > 0 && isEnabledLoadMore) {
                    if (!canChildScrollDown()) {
                        mPendingLoadMore = true;
                        intercept = true;
                    }
                    if (mPendingRefresh) {
                        intercept = true;
                        if (pending > 0) {
                            scrollTo(0, 0);
                            return super.dispatchTouchEvent(ev);
                        }
                    }
                }
                if (intercept) {
                    scrollBy(0, (int) dy);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                scrollEnd();
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void scrollEnd() {
        if (isLoading || isRefreshing || !isEnabled()) {
            return;
        }
        int offset = getScrollY();
        if (offset < 0) {
            if (Math.abs(offset) >= mHeader.getHeight()) {
                isRefreshing = true;
                smoothScrollTo(-mHeader.getHeight());
                setHeadAnim(true);
                if (mRefreshListener != null) mRefreshListener.onRefresh();
            } else {
                smoothScrollTo(0);
            }
        }
        if (offset > 0) {
            if (offset > mFooter.getHeight()) {
                smoothScrollTo(mFooter.getHeight());
            }
            if (offset > mFooter.getHeight() || isAutoLoadMore) {
                isLoading = true;
                setFootAnim(true);
                if (mLoadMoreListener != null) mLoadMoreListener.onLoadMore();
            } else {
                smoothScrollTo(0);
            }
        }
        mPendingRefresh = false;
        mPendingLoadMore = false;
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public interface OnRefreshListener {
        void onRefresh();
    }

    public void loadComplete() {
        loadFinish();
        isEnabledLoadMore = false;
    }

    /**
     * 禁止后 重新可用
     */
    public void loadMoreEnabled() {
        isEnabledLoadMore = true;
    }

    public void loadFinish() {
        post(new Runnable() {
            @Override
            public void run() {
                smoothScrollTo(0);
                isLoading = false;
                mPendingLoadMore = false;
                setFootAnim(false);
            }
        });
    }

    public void setRefreshing(boolean refreshing) {
        isRefreshing = refreshing;
        mPendingRefresh = refreshing;
        if (refreshing) {
            post(new Runnable() {
                @Override
                public void run() {
                    setHeadAnim(true);
                    smoothScrollTo(-mHeader.getHeight());
                }
            });
        } else {
            setHeadAnim(false);
            smoothScrollTo(0);
        }
    }

    private ValueAnimator resetAnimator;

    public void smoothScrollTo(int to) {
        if (resetAnimator != null && resetAnimator.isRunning()) {
            resetAnimator.cancel();
        }
        resetAnimator = ValueAnimator.ofInt(getScrollY(), to);
        resetAnimator.setDuration(400);
        resetAnimator.addUpdateListener(resetAnimatorUpdateListener);
        resetAnimator.start();
    }

    private ValueAnimator.AnimatorUpdateListener resetAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int cur = (int) animation.getAnimatedValue();
            scrollTo(0, cur);
        }
    };

    private boolean canChildScrollDown() {
        if (mChildScrollListener != null) return mChildScrollListener.canChildScroll(1);
        return mChild.canScrollVertically(1);
    }

    private boolean canChildScrollUp() {
        if (mChildScrollListener != null) return mChildScrollListener.canChildScroll(-1);
        return mChild.canScrollVertically(-1);
    }

    public interface OnChildScrollListener {
        boolean canChildScroll(int direction);
    }


    public void setOnChildScrollListener(OnChildScrollListener listener) {
        mChildScrollListener = listener;
    }

    @Override
    public int getNestedScrollAxes() {
        return mParentHelper.getNestedScrollAxes();
    }

//    @Override
//    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
//        return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0 && !isLoading && !isRefreshing;
//    }
//
//    @Override
//    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {
//        mParentHelper.onNestedScrollAccepted(child, target, axes, type);
//    }
//
//    @Override
//    public void onStopNestedScroll(@NonNull View target, int type) {
//        mParentHelper.onStopNestedScroll(target, type);
//    }
//
//    @Override
//    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
//        onNestedScrollInternal(dyUnconsumed, type, consumed);
//    }
//
//    @Override
//    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
//        onNestedScrollInternal(dyUnconsumed, type, null);
//    }
//
//    private void onNestedScrollInternal(int dyUnconsumed, int type, @Nullable int[] consumed) {
//        scrollBy(0, dyUnconsumed);
//        if (consumed != null) {
//            consumed[1] = dyUnconsumed;
//        }
//    }
//
//    @Override
//    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
//        int scrollY = getScrollY();
//        int targetY = scrollY + dy;
//        if (dy > 0) {
//            if (scrollY < 0) {
//                if (targetY > 0) {
//                    targetY = 0;
//                }
//                scrollBy(0, targetY - scrollY);
//                consumed[1] = dy;
//            }
//        }
//        if (dy < 0) {
//            if (scrollY > 0) {
//                if (targetY < 0) {
//                    targetY = 0;
//                }
//                scrollBy(0, targetY - scrollY);
//                consumed[1] = dy;
//            }
//        }
//    }

    @Override
    public void scrollTo(int x, int y) {
        if (y < 0) {
            if (Math.abs(y) > mMaxOffset) {
                y = -mMaxOffset;
            }
        }
        if (y > 0) {
            if (y > mMaxOffset) {
                y = mMaxOffset;
            }
        }
        super.scrollTo(x, y);
    }
}
