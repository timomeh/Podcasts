package de.timomeh.podcasts.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import de.timomeh.podcasts.R;

/**
 * Created by Timo Maemecke (@timomeh) on 28/01/15.
 * <p/>
 * TODO: Add a class header comment
 */
public class FixedAspectLayout extends RelativeLayout {

    private float aspect = 1.0f;

    public FixedAspectLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.FixedAspectLayout);
        aspect = a.getFloat(R.styleable.FixedAspectLayout_aspectRatio, 1.0f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);

        if (w == 0) {
            h = 0;
        } else if (h / w < aspect) {
            w = (int)(h / aspect);
        } else {
            h = (int)(w * aspect);
        }

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(w,
                        MeasureSpec.getMode(widthMeasureSpec)),
                MeasureSpec.makeMeasureSpec(h,
                        MeasureSpec.getMode(heightMeasureSpec)));
    }
}