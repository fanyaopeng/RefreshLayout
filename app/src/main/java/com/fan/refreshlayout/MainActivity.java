package com.fan.refreshlayout;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private List<Fragment> mFragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ViewPager pager = findViewById(R.id.vp);
        TabLayout tab = findViewById(R.id.tab);
        mFragments = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            mFragments.add(new TestFragment());
        }
        pager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                Bundle bundle=new Bundle();
                bundle.putInt("pos",position);
                Fragment f=mFragments.get(position);
                f.setArguments(bundle);
                return f;
            }

            @Override
            public int getCount() {
                return mFragments.size();
            }

            @Nullable
            @Override
            public CharSequence getPageTitle(int position) {
                return "tab "+position;
            }
        });
        tab.setupWithViewPager(pager);
    }

}
