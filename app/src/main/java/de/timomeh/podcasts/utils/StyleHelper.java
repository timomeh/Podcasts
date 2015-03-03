package de.timomeh.podcasts.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import de.timomeh.podcasts.R;

/**
 * Created by Timo Maemecke (@timomeh) on 27/01/15.
 * <p/>
 * TODO: Add a class header comment
 */
public class StyleHelper {

    public final static int THEME_DARK = 0;
    public final static int THEME_TRANSLUCENT = 1;

    // see http://stackoverflow.com/questions/16312792/how-to-check-color-brightness-in-android
    public static boolean isBrightColor(int color) {
        if (android.R.color.transparent == color)
            return true;

        boolean rtnValue = false;

        int[] rgb = { Color.red(color), Color.green(color), Color.blue(color) };

        int brightness = (int) Math.sqrt(rgb[0] * rgb[0] * .241 + rgb[1]
                * rgb[1] * .691 + rgb[2] * rgb[2] * .068);

        // color is light
        if (brightness >= 200) {
            rtnValue = true;
        }

        return rtnValue;
    }

    public static float convertDpToPixel(int dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }

    public static int darkenColor(int color, float amount) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= amount;
        return Color.HSVToColor(hsv);
    }

    public static int getWindowWidth(Context c) {
        WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }
}
