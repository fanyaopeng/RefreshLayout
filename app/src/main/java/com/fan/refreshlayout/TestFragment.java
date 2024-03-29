package com.fan.refreshlayout;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.fyp.refresh.UniversalRefreshLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by huisoucw on 2018/9/18.
 */

public class TestFragment extends Fragment {
    private RecyclerView mList;
    private List<String> mData = new ArrayList<>();
    private UniversalRefreshLayout mRefreshLayout;
    private int max = 30;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_test, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mList = getView().findViewById(R.id.list);
        for (int i = 0; i < 20; i++) {
            mData.add("这是数据" + i);

        }
        int pos = getArguments().getInt("pos");
        mList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mList.setAdapter(new DataAdapter());

        mRefreshLayout = getView().findViewById(R.id.refresh);
        mRefreshLayout.setAutoLoadMore(pos < 2);
        mRefreshLayout.setOnRefreshListener(new UniversalRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mRefreshLayout.loadMoreEnabled();
                max += 5;
                mHandler.sendEmptyMessageDelayed(1, 2000);
            }
        });
        mRefreshLayout.setOnLoadMoreListener(new UniversalRefreshLayout.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                mHandler.sendEmptyMessageDelayed(2, 2000);
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
                int end = mData.size() + 5;
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
