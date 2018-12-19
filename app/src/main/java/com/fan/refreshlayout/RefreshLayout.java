package com.fan.refreshlayout;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.OverScroller;
import android.widget.Scroller;

/**
 * Created by huisoucw on 2018/8/30.
 */

@SuppressLint("NewApi")
public class RefreshLayout extends ViewGroup implements ViewTreeObserver.OnScrollChangedListener, View.OnScrollChangeListener {
    private View mChild;
    private View mFooter;
    private View mHeader;
    private int mFootMaxOffset;
    private int mHeadMaxOffset;
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
    private ImageView mHeadImage, mFootImage;
    private boolean isAutoLoadMore;
    private OverScroller mChildScroller;
    private VelocityTracker mVelocityTracker;

    public RefreshLayout(Context context) {
        super(context);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        createHeadView();
        createFootView();
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mFootMaxOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());
        mHeadMaxOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, context.getResources().getDisplayMetrics());
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
        mHeader = getChildAt(0);
        mFooter = getChildAt(1);
        mChild = getChildAt(2);
        mChild.getViewTreeObserver().addOnScrollChangedListener(this);
        mChild.setOnScrollChangeListener(this);
        if (mChild instanceof RecyclerView) {
            mChildScroller = new OverScroller(getContext(), sQuinticInterpolator);
        } else {
            mChildScroller = new OverScroller(getContext());
        }
    }

    private void createFootView() {
        LayoutInflater.from(getContext()).inflate(R.layout.refresh_footer, this, true);
        mFootImage = findViewById(R.id.img_footer);
    }

    private void createHeadView() {
        LayoutInflater.from(getContext()).inflate(R.layout.refresh_header, this, true);
        mHeadImage = findViewById(R.id.img_head);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            reset();
        }
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
        AnimationDrawable drawable = (AnimationDrawable) mHeadImage.getDrawable();
        if (start) {
            drawable.start();
        } else {
            drawable.selectDrawable(0);
            drawable.stop();
        }

    }

    private void setFootAnim(boolean start) {
        AnimationDrawable drawable = (AnimationDrawable) mFootImage.getDrawable();
        if (start) {
            drawable.start();
        } else {
            drawable.selectDrawable(0);
            drawable.stop();
        }
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        if (listener != null)
            isEnabledLoadMore = true;
        mLoadMoreListener = listener;
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        mRefreshListener = listener;
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
        smoothScrollTo(0);
        isLoading = false;
        mPendingLoadMore = false;
        setFootAnim(false);
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

    public void setAutoLoadMore(boolean autoLoadMore) {
        isAutoLoadMore = autoLoadMore;
    }

    @Override
    public void onScrollChanged() {

    }

    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        if (mChildVelocity < 0) {
            mChildScroller.computeScrollOffset();
            Log.e("main", "开始fling  " + mChildScroller.getCurrVelocity() + "总速率为 " + mChildVelocity);
            if (!canChildScrollDown()) {
                flingTask.fling((int) mChildScroller.getCurrVelocity());
            }
        }
    }

    static final Interpolator sQuinticInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };
    private FlingTask flingTask = new FlingTask();

    private class FlingTask implements Runnable {
        private Scroller scroller;

        FlingTask() {
            scroller = new Scroller(getContext(), new DecelerateInterpolator());
        }

        void fling(int velocity) {
            scroller.fling(0, getScrollY(), 0, velocity, Integer.MIN_VALUE, Integer.MAX_VALUE,
                    Integer.MIN_VALUE, mFooter.getHeight());
            post(this);
            Log.e("main", "当前速率为" + velocity);
        }

        @Override
        public void run() {
            if (scroller.computeScrollOffset()) {
                Log.e("main", " curY " + scroller.getCurrY());
                scrollTo(0, scroller.getCurrY());
                post(this);
                if (isAutoLoadMore) {
                    if (isLoading) return;
                    isLoading = true;
                    setFootAnim(true);
                    if (mLoadMoreListener != null) {
                        mLoadMoreListener.onLoadMore();
                    }
                }
            }
        }
    }


    private void computerFlingWithChild() {
        mChildScroller.fling(0, getScrollY(), 0, (int) -mChildVelocity, Integer.MIN_VALUE, Integer.MAX_VALUE,
                Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private float mChildVelocity;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (isLoading && !isAutoLoadMore || isRefreshing || !isEnabled()) {
            return super.dispatchTouchEvent(ev);
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        if (mChildVelocity != 0) {
            mChildVelocity = 0;
        }
        mVelocityTracker.addMovement(ev);
        int offset = getScrollY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = ev.getRawY();
                mLastY = mDownY;
                break;
            case MotionEvent.ACTION_MOVE:
                float curY = ev.getRawY();
                int dy = (int) (mLastY - curY);
                mLastY = curY;
                if (Math.abs(curY - mDownY) < mTouchSlop) return super.dispatchTouchEvent(ev);
                mVelocityTracker.computeCurrentVelocity(1000);
                int pending = dy + offset;
                if (dy > 0) {
                    if (mPendingRefresh) {
                        if (offset >= 0) {
                            scrollTo(0, 0);
                            return super.dispatchTouchEvent(ev);
                        }
                    } else {
                        if (!isEnabledLoadMore || canChildScrollDown()) {
                            return super.dispatchTouchEvent(ev);
                        }
                        if (Math.abs(pending) > mFootMaxOffset) {
                            scrollTo(0, mFootMaxOffset);
                        }
                        mPendingLoadMore = true;
                        if (isAutoLoadMore && !isLoading) {
                            isLoading = true;
                            setFootAnim(true);
                            if (mLoadMoreListener != null) {
                                mLoadMoreListener.onLoadMore();
                            }
                        }
                    }
                }
                if (dy < 0) {
                    if (mPendingLoadMore) {
                        if (offset <= 0) {
                            scrollTo(0, 0);
                            return super.dispatchTouchEvent(ev);
                        }
                    } else {
                        if (canChildScrollUp()) {
                            return super.dispatchTouchEvent(ev);
                        }
                        if (Math.abs(pending) > mHeadMaxOffset) {
                            scrollTo(0, -mHeadMaxOffset);
                            return true;
                        }
                        mPendingRefresh = true;
                    }
                }
                getParent().requestDisallowInterceptTouchEvent(Math.abs(offset) != 0);
                scrollBy(0, dy);
                return true;
            case MotionEvent.ACTION_UP:
                mPendingRefresh = false;
                mPendingLoadMore = false;
                mChildVelocity = mVelocityTracker.getYVelocity();
                mVelocityTracker.clear();
                if (isAutoLoadMore) {
                    computerFlingWithChild();
                }
                if (offset > 0) {
                    if (offset > mFooter.getHeight()) {
                        smoothScrollTo(mFooter.getHeight());
                    }
                    if (!isAutoLoadMore && offset >= mFooter.getHeight()) {
                        isLoading = true;
                        setFootAnim(true);
                        if (mLoadMoreListener != null) mLoadMoreListener.onLoadMore();
                    }
                } else if (offset < 0 && Math.abs(offset) >= mHeader.getHeight()) {
                    smoothScrollTo(-mHeader.getHeight());
                    isRefreshing = true;
                    setHeadAnim(true);
                    if (mRefreshListener != null) mRefreshListener.onRefresh();
                } else {
                    smoothScrollTo(0);
                }
                break;

        }
        return super.dispatchTouchEvent(ev);
    }

    private ValueAnimator resetAnimator;

    public void smoothScrollTo(int to) {
        if (resetAnimator != null && resetAnimator.isRunning()) {
            resetAnimator.cancel();
        }
        resetAnimator = ValueAnimator.ofInt(getScrollY(), to);
        resetAnimator.setDuration(200);
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
