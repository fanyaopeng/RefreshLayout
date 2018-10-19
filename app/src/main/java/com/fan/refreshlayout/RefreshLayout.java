package com.fan.refreshlayout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

/**
 * Created by huisoucw on 2018/8/30.
 */

public class RefreshLayout extends ViewGroup {
    private View mChild;
    private View mBottom;
    private View mHeader;
    private int mFootMaxOffset;
    private int mHeadMaxOffset;
    private boolean isLoading;
    private boolean isRefreshing;
    private float mLastY;
    private float mDownY;
    private OnLoadMoreListener mLoadMoreListener;
    private OnRefreshListener mRefreshListener;
    private ChildScrollListener mChildScrollListener;
    private TextView tvLoading;
    private boolean isEnabledLoadMore;
    private ProgressBar mProgressBar;
    private final String LOADING = "加载中";
    private final String PENDING_LOADING = "加载更多";
    private final String REFRESHING = "正在刷新...";
    private final String PENDING_REFRESHING = "下拉刷新";
    private boolean mPendingRefresh;
    private boolean mPendingLoadMore;
    private int mTouchSlop;

    public RefreshLayout(Context context) {
        super(context);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        createHeadView();
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
        int t = -mHeader.getMeasuredHeight();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.layout(left, t, right, t + child.getMeasuredHeight());
            t += child.getMeasuredHeight();
        }
    }

    private void createFootView() {
        mBottom = LayoutInflater.from(getContext()).inflate(R.layout.footer, this, false);
        tvLoading = mBottom.findViewById(R.id.tv_progress);
        mProgressBar = mBottom.findViewById(R.id.progress);
        mProgressBar.setVisibility(GONE);
        addView(mBottom);
    }

    private void createHeadView() {
        mHeader = LayoutInflater.from(getContext()).inflate(R.layout.header, this, false);
        addView(mHeader);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        //父类有一个圆形的 head
        if (getChildCount() > 2) {
            throw new InflateException("can only have one child");
        }
        mChild = getChildAt(1);
        createFootView();
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
        setEnabled(true);
        slowReset(0);
        isLoading = false;
        mPendingLoadMore = false;
        mProgressBar.setVisibility(GONE);
        tvLoading.setText(PENDING_LOADING);
    }

    public void setRefreshing(boolean refreshing) {
        isRefreshing = refreshing;
        mPendingRefresh = refreshing;
        if (refreshing) {
            post(new Runnable() {
                @Override
                public void run() {
                    slowReset(-mHeader.getHeight());
                }
            });
        } else {
            slowReset(0);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isLoading || isRefreshing)
            return super.onInterceptTouchEvent(ev);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastY = ev.getRawY();
                mDownY = mLastY;
                break;
            case MotionEvent.ACTION_MOVE:
                float curY = ev.getRawY();
                float dy = curY - mLastY;
                mLastY = curY;
                if (Math.abs(curY - mDownY) < mTouchSlop) return super.onInterceptTouchEvent(ev);
                if (dy < 0) {
                    //加载更多
                    if (canChildScrollDown() || !isEnabledLoadMore) {
                        return super.onInterceptTouchEvent(ev);
                    }
                    mPendingLoadMore = true;
                }
                if (dy > 0) {
                    //下拉刷新
                    if (canChildScrollUp()) {
                        return super.onInterceptTouchEvent(ev);
                    }
                    mPendingRefresh = true;
                }
                return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isLoading || isRefreshing) {
            return super.onTouchEvent(ev);
        }
        int offset = getScrollY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float curY = ev.getRawY();
                int dy = (int) (curY - mLastY);
                mLastY = curY;
                int pending = -dy + offset;
                if (mPendingLoadMore) {
                    if (pending > mFootMaxOffset) {
                        scrollTo(0, mFootMaxOffset);
                        return super.onTouchEvent(ev);
                    }
                    if (pending < 0) {
                        scrollTo(0, 0);
                        return super.onTouchEvent(ev);
                    }
                }
                if (mPendingRefresh) {
                    if (Math.abs(pending) > mHeadMaxOffset) {
                        scrollTo(0, -mHeadMaxOffset);
                        return super.onTouchEvent(ev);
                    }
                    if (pending > 0) {
                        scrollTo(0, 0);
                        return super.onTouchEvent(ev);
                    }
                }
                scrollBy(0, -dy);
                break;
            case MotionEvent.ACTION_UP:
                if (offset > 0 && offset >= mBottom.getHeight()) {
                    isLoading = true;
                    mProgressBar.setVisibility(VISIBLE);
                    tvLoading.setText(LOADING);
                    slowReset(mBottom.getHeight());
                    if (mLoadMoreListener != null) mLoadMoreListener.onLoadMore();
                } else if (offset < 0 && Math.abs(offset) >= mHeader.getHeight()) {
                    slowReset(-mHeader.getHeight());
                    isRefreshing = true;
                    if (mRefreshListener != null) mRefreshListener.onRefresh();
                } else {
                    slowReset(0);
                    mPendingRefresh = false;
                    mPendingLoadMore = false;
                }
                break;

        }
        return true;
    }

    public void slowReset(int to) {
        ValueAnimator animator = ValueAnimator.ofInt(getScrollY(), to);
        animator.setDuration(200);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int cur = (int) animation.getAnimatedValue();
                scrollTo(0, cur);
            }
        });
        animator.start();
    }

    private boolean canChildScrollDown() {
        if (mChildScrollListener != null) return mChildScrollListener.canChildScrollDown(1);
        return mChild.canScrollVertically(1);
    }

    private boolean canChildScrollUp() {
        if (mChildScrollListener != null) return mChildScrollListener.canChildScrollUp(-1);
        return mChild.canScrollVertically(-1);
    }

    public interface ChildScrollListener {
        boolean canChildScrollDown(int direction);

        boolean canChildScrollUp(int direction);
    }


    public void setChildScrollListener(ChildScrollListener listener) {
        mChildScrollListener = listener;
    }
}
