package com.fan.refreshlayout;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by huisoucw on 2018/9/18.
 */

public class TestFragment extends Fragment {
    private RecyclerView mList;
    private List<String> mData = new ArrayList<>();
    private RefreshLayout mRefreshLayout;
    private int max = Integer.MAX_VALUE;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_test, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mList = getView().findViewById(R.id.list);
        mList.setNestedScrollingEnabled(false);
        for (int i = 0; i < 5; i++) {
            mData.add("这是数据" + i);
        }
        mList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRefreshLayout = getView().findViewById(R.id.refresh);
        //mRefreshLayout.setRefreshing(true);
        mList.setAdapter(new DataAdapter());
        mRefreshLayout.setOnRefreshListener(new RefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mRefreshLayout.loadMoreEnabled();
                max += 5;
                mHandler.sendEmptyMessageDelayed(1, 2000);
            }
        });
        mRefreshLayout.setOnLoadMoreListener(new RefreshLayout.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                mHandler.sendEmptyMessageDelayed(2, 200);
            }
        });
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 1) {
                mRefreshLayout.setRefreshing(false);
            } else if (msg.what == 2) {
                int start = mData.size();
                int end = mData.size() + 20;
                while (start < end) {
                    start++;
                    mData.add("这是数据" + start);
                }
                mList.getAdapter().notifyDataSetChanged();
                mRefreshLayout.loadFinish();
                if (mData.size() >= max) mRefreshLayout.loadComplete();
            }
            return false;
        }
    });

    private class DataAdapter extends RecyclerView.Adapter<DataAdapter.VH> {


        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false));
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            holder.tv.setText(mData.get(position));
            holder.img.setImageResource(R.mipmap.image1);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tv;
            ImageView img;

            VH(View itemView) {
                super(itemView);
                tv = itemView.findViewById(R.id.tv);
                img = itemView.findViewById(R.id.image);
            }
        }
    }
}
