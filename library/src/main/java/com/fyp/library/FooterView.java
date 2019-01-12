package com.fyp.library;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.support.annotation.NonNull;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class FooterView extends FrameLayout {
    private ImageView mFootImage;

    public FooterView(@NonNull Context context) {
        super(context);
        inflate(context, R.layout.refresh_footer, this);
        mFootImage = findViewById(R.id.img_footer);
    }

    public void setFootAnim(boolean start) {
        AnimationDrawable drawable = (AnimationDrawable) mFootImage.getDrawable();
        if (start) {
            drawable.start();
        } else {
            drawable.selectDrawable(0);
            drawable.stop();
        }
    }
}
