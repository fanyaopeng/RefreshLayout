package com.fyp.refresh;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.view.NestedScrollingChild2;
import android.support.v4.view.NestedScrollingParent2;
import android.support.v4.view.NestedScrollingParentHelper;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

/**
 * 一个可定制的刷新控件  支持NestedScrolling 控件 等的惯性滑动
 */
public class UniversalRefreshLayout extends ViewGroup implements NestedScrollingParent2 {
    private View mChild;
    private FooterView mFooter;
    private HeaderView mHeader;
    private int mMaxOffset;
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
    private int mTouchSlop;
    private NestedScrollingParentHelper mNestedScrollingParentHelper;
    private boolean isNestedScrollingChild;
    private boolean isNestedScrollingChildFling;
    private float mNestedScrollingChildFlingVelocity;
    //大于这个速率我们才认为他是在fling
    private final int mSlopFlingVelocity = 3000;
    private boolean isAutoLoadMore;
    private Drawable mFootAnim;
    private Drawable mHeadAnim;
    //手势标记
    private int motionMask = 0;
    //停止scrolling标记
    private int onNestedScrollingStopMask = 1;
    //是否正在惯性返回下拉刷新或者加载更多
    private boolean isResettingInertia;

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

        createHeadView();
        createFootView();
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
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
        if (changed) {
            ensureTarget();
        }
        int headTop = -mHeader.getMeasuredHeight();
        mHeader.layout(left, headTop, right, headTop + mHeader.getMeasuredHeight());
        int mChildHeight = getMeasuredHeight();
        int childTop = mHeader.getBottom();
        mChild.layout(left, childTop, right, mChildHeight);
        int footTop = mChild.getBottom();
        mFooter.layout(left, footTop, right, footTop + mFooter.getMeasuredHeight());
    }

    private void ensureTarget() {
        if (getChildCount() > 3) {
            throw new InflateException("can only have one child");
        }
        mHeader = (HeaderView) getChildAt(0);
        mFooter = (FooterView) getChildAt(1);
        mChild = getChildAt(2);
    }

    private void createFootView() {
        FooterView footerView = new FooterView(getContext(), mFootAnim);
        addView(footerView);
    }

    private void createHeadView() {
        HeaderView headerView = new HeaderView(getContext(), mHeadAnim);
        addView(headerView);
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

    @SuppressLint("NewApi")
    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
        isNestedScrollingChild = target instanceof NestedScrollingChild2 && target.isNestedScrollingEnabled();
        return isNestedScrollingChild;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes, type);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        //这个方法调用三次 分别在down up 和fling结束 分别处理
        mNestedScrollingParentHelper.onStopNestedScroll(target, type);
        onNestedScrollingStopMask <<= 1;
        //Log.e("main", "onNestedScrollingStopMask " + onNestedScrollingStopMask);
        if (!isNestedScrollingChildFling) {
            if ((onNestedScrollingStopMask & 4) == 4) {
                scrollEnd();
            }
        } else {
            if ((onNestedScrollingStopMask & 8) == 8) {
                //这里只处理比max小的情况 比max 大 的情况已经被 onNestedScroll 处理
                if (Math.abs(getScrollY()) < mMaxOffset || mNestedScrollingChildFlingVelocity != 0) {
                    scrollEnd();
                }
            }
        }
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        isNestedScrollingChildFling = consumed;
        mNestedScrollingChildFlingVelocity = Math.abs(velocityY);
        return super.onNestedFling(target, velocityX, velocityY, consumed);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        if (isLoading || isRefreshing || !isEnabled()) {
            return;
        }
        //刷新正在惯性回滚
        if (isNestedScrollingChildFling && isResettingInertia) {
            return;
        }
        int offset = Math.abs(getScrollY());
        if (isNestedScrollingChildFling) {
            if (offset >= mMaxOffset) {
                scrollEnd();
                return;
            }
        }
        boolean intercept = false;
        if (dyUnconsumed < 0 && !canChildScrollUp()) {
            intercept = true;
        }
        if (dyUnconsumed > 0 && !canChildScrollDown() && isEnabledLoadMore) {
            intercept = true;
        }
        if (intercept) {
            scrollBy(0, getTargetOffset(dyUnconsumed));
        }
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        if (isLoading || isRefreshing || !isEnabled()) {
            return;
        }
        int offset = getScrollY();
        if (dy > 0) {
            if (offset < 0) {
                if (offset + dy > 0) {
                    dy = -offset;
                }
                scrollBy(0, dy);
                consumed[1] = dy;
            }
        }
        if (dy < 0) {
            if (offset > 0) {
                if (offset + dy < 0) {
                    dy = -offset;
                }
                scrollBy(0, dy);
                consumed[1] = dy;
            }
        }

    }

    /**
     * 计算超过最大值前 的距离
     *
     * @param dy
     * @return
     */
    private int getTargetOffset(int dy) {
        int symbol;
        if (dy > 0) {
            symbol = 1;
        } else {
            symbol = -1;
        }
        int offset = getScrollY();
        int pending = offset + dy;
        if (Math.abs(pending) > mMaxOffset) {
            dy = symbol * mMaxOffset - offset;
        }
        return dy;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (isLoading || isRefreshing || !isEnabled()) {
            return super.dispatchTouchEvent(ev);
        }
        if (isNestedScrollingChild) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    motionMask &= 0;
                    onNestedScrollingStopMask = 1;
                    isNestedScrollingChildFling = false;
                    isResettingInertia = false;
                    mNestedScrollingChildFlingVelocity = 0;
                    break;
                case MotionEvent.ACTION_MOVE:
                    motionMask |= 2;
                    break;
                case MotionEvent.ACTION_UP:
                    motionMask |= 1;
                    break;
            }
        } else {
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
                    if (dy > 0) {
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
                        scrollBy(0, getTargetOffset((int) dy));
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    scrollEnd();
                    break;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void scrollEnd() {
        if (isLoading || isRefreshing || !isEnabled()) {
            return;
        }

        int offset = getScrollY();
        if (offset < 0) {
            if (isNestedScrollingChildFling) {
                smoothScrollTo(0);
                isResettingInertia = true;
            } else {
                if (Math.abs(offset) >= mHeader.getHeight()) {
                    isRefreshing = true;
                    smoothScrollTo(-mHeader.getHeight());
                    setHeadAnim(true);
                    if (mRefreshListener != null) mRefreshListener.onRefresh();
                } else {
                    smoothScrollTo(0);
                }
            }
        }
        if (offset > 0) {
            if (isNestedScrollingChildFling && mNestedScrollingChildFlingVelocity > mSlopFlingVelocity && !isAutoLoadMore) {
                smoothScrollTo(0);
                isResettingInertia = true;
            } else {
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
}
