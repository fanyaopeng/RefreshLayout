package com.fyp.refresh;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class FooterView extends FrameLayout {
    private ImageView mFootImage;

    public FooterView(@NonNull Context context, Drawable footAnim) {
        super(context);
        inflate(context, R.layout.refresh_footer, this);
        mFootImage = findViewById(R.id.img_footer);
        mFootImage.setImageDrawable(footAnim);
    }

    public void setFootAnim(boolean start) {
        if (!(mFootImage.getDrawable() instanceof AnimationDrawable)) {
            return;
        }
        AnimationDrawable drawable = (AnimationDrawable) mFootImage.getDrawable();
        if (start) {
            drawable.start();
        } else {
            drawable.selectDrawable(0);
            drawable.stop();
        }
    }
}
