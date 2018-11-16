package com.fan.refreshlayout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.AnimationDrawable;
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
import android.widget.ImageView;
import android.widget.Scroller;

/**
 * Created by huisoucw on 2018/8/30.
 */
public class RefreshLayout extends ViewGroup implements View.OnScrollChangeListener {
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
    private int mTouchSlop;
    private ImageView mHeadImage, mFootImage;
    private VelocityTracker mVelocityTracker;
    private Scroller mScroller;
    private boolean isAutoLoadMore = true;

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
        mScroller = new Scroller(context);
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
        mChild = getChildAt(2);
        mChild.setOnScrollChangeListener(this);
    }

    private void createFootView() {
        mFooter = LayoutInflater.from(getContext()).inflate(R.layout.refresh_footer, this, false);
        addView(mFooter);
        mFootImage = mFooter.findViewById(R.id.img_footer);
    }

    private void createHeadView() {
        mHeader = LayoutInflater.from(getContext()).inflate(R.layout.refresh_header, this, false);
        addView(mHeader);
        mHeadImage = mHeader.findViewById(R.id.img_head);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            reset();
        }
    }

    private void reset() {
        slowReset(0);
        isLoading = false;
        isRefreshing = false;
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
        slowReset(0);
        isLoading = false;
        setFootAnim(false);
    }

    public void setRefreshing(boolean refreshing) {
        isRefreshing = refreshing;
        if (refreshing) {
            post(new Runnable() {
                @Override
                public void run() {
                    setHeadAnim(true);
                    slowReset(-mHeader.getHeight());
                }
            });
        } else {
            setHeadAnim(false);
            slowReset(0);
        }
    }

    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        if (mVelocityTracker == null) return;
        if (mChildVelocity < 0 && !canChildScrollDown()) {
            mScroller.fling(0, getScrollY(), 0, (int) -mChildVelocity,
                    Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, mFootMaxOffset);
            invalidate();
        }
    }

    private float mChildVelocity;

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            int target = mScroller.getCurrY();
            if (getScrollY() >= mHeader.getHeight()) {
                target = mHeader.getHeight();
            }
            scrollTo(0, target);
            postInvalidate();
            if (!isLoading) {
                setFootAnim(true);
                isLoading = true;
                if (mLoadMoreListener != null) mLoadMoreListener.onLoadMore();
            }
            Log.e("main", "velocity " + mScroller.getCurrVelocity() + "   " + mScroller.getCurrY());
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (isLoading || isRefreshing || !isEnabled()) {
            return super.dispatchTouchEvent(ev);
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        if (mChildVelocity != 0) mChildVelocity = 0;
        int offset = getScrollY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = ev.getRawY();
                mLastY = mDownY;
                mVelocityTracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                mVelocityTracker.addMovement(ev);
                mVelocityTracker.computeCurrentVelocity(1000);
                float curY = ev.getRawY();
                float dy = mLastY - curY;
                mLastY = curY;
                if (Math.abs(curY - mDownY) < mTouchSlop) return super.dispatchTouchEvent(ev);
                float pending = dy + offset;
                if (dy > 0) {
                    //加载更多
                    if (!isEnabledLoadMore) {
                        return super.dispatchTouchEvent(ev);
                    }
                    if (canChildScrollDown() && offset >= 0) {
                        scrollTo(0, 0);
                        return super.dispatchTouchEvent(ev);
                    }
                    if (pending > mFootMaxOffset) {
                        scrollTo(0, mFootMaxOffset);
                        return true;
                    }
                }
                if (dy < 0) {
                    if (canChildScrollUp() && offset <= 0) {
                        scrollTo(0, 0);
                        return super.dispatchTouchEvent(ev);
                    }
                    if (Math.abs(pending) > mHeadMaxOffset) {
                        scrollTo(0, -mHeadMaxOffset);
                        return true;
                    }
                }
                getParent().requestDisallowInterceptTouchEvent(Math.abs(offset) != 0);
                scrollBy(0, (int) dy);
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mChildVelocity = mVelocityTracker.getYVelocity();
                mVelocityTracker.clear();
                if (offset > 0 && offset >= mFooter.getHeight()) {
                    isLoading = true;
                    slowReset(mFooter.getHeight());
                    setFootAnim(true);
                    if (mLoadMoreListener != null) mLoadMoreListener.onLoadMore();
                } else if (offset < 0 && Math.abs(offset) >= mHeader.getHeight()) {
                    slowReset(-mHeader.getHeight());
                    isRefreshing = true;
                    setHeadAnim(true);
                    if (mRefreshListener != null) mRefreshListener.onRefresh();
                } else {
                    slowReset(0);
                }
                break;

        }
        return super.dispatchTouchEvent(ev);
    }

    private ValueAnimator resetAnimator;

    public void slowReset(int to) {
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
