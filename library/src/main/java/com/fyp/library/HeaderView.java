package com.fyp.library;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.support.annotation.NonNull;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class HeaderView extends FrameLayout {
    private ImageView mHeadImage;

    public HeaderView(@NonNull Context context) {
        super(context);
        inflate(context, R.layout.refresh_header, this);
        mHeadImage = findViewById(R.id.img_head);
    }

    public void setHeadAnim(boolean start) {
        AnimationDrawable drawable = (AnimationDrawable) mHeadImage.getDrawable();
        if (start) {
            drawable.start();
        } else {
            drawable.selectDrawable(0);
            drawable.stop();
        }

    }
}
