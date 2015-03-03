package de.timomeh.podcasts.utils;

import android.text.format.DateUtils;

import java.util.Date;

/**
 * Created by Timo Maemecke (@timomeh) on 29/01/15.
 * <p/>
 * TODO: Add a class header comment
 */
public class StringHelper {

    public static String convertDuration(long duration) {
        int min = Math.round(duration / 60);
        int sec = Math.round(duration % 60);
        String minutes;
        String seconds;
        if (min < 10) {
            minutes = "0" + min;
        } else {
            minutes = "" + min;
        }

        if (sec < 10) {
            seconds = "0" + sec;
        } else {
            seconds = "" + sec;
        }
        return minutes + ":" + seconds;
    }

    public static String humanDate(Date date) {
        if (date == null) {
            return "";
        } else {
            long now = new Date().getTime();
            return DateUtils.getRelativeTimeSpanString(date.getTime(), now, DateUtils.DAY_IN_MILLIS).toString();
        }
    }

    public static String humanByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
