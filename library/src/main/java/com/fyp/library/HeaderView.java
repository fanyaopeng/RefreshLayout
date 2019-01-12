package com.fyp.library;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class HeaderView extends FrameLayout {
    private ImageView mHeadImage;

    public HeaderView(@NonNull Context context, Drawable headAnim) {
        super(context);
        inflate(context, R.layout.refresh_header, this);
        mHeadImage = findViewById(R.id.img_head);
        mHeadImage.setImageDrawable(headAnim);
    }

    public void setHeadAnim(boolean start) {
        if (!(mHeadImage.getDrawable() instanceof AnimationDrawable)) {
            return;
        }
        AnimationDrawable drawable = (AnimationDrawable) mHeadImage.getDrawable();
        if (start) {
            drawable.start();
        } else {
            drawable.selectDrawable(0);
            drawable.stop();
        }

    }
}
