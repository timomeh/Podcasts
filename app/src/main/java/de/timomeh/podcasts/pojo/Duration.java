package de.timomeh.podcasts.pojo;

/**
 * Created by Timo Maemecke (@timomeh) on 23/02/15.
 * <p/>
 * TODO: Add a class header comment
 */
public class Duration {

    private Long mTotalSeconds;
    private int mSeconds;
    private int mMinutes;

    public Duration(Long totalSeconds) {
        mTotalSeconds = totalSeconds;
        mMinutes = Math.round(totalSeconds / 60);
        mSeconds = Math.round(totalSeconds % 60);
    }

    public int getSeconds() {
        return mSeconds;
    }

    public void setSeconds(int seconds) {
        mSeconds = seconds;
    }

    public int getMinutes() {
        return mMinutes;
    }

    public void setMinutes(int minutes) {
        mMinutes = minutes;
    }

    public String getTwoDigitSeconds() {
        return convertTwoDigit(mSeconds);
    }

    public String getTwoDigitMinutes() {
        return convertTwoDigit(mMinutes);
    }

    private String convertTwoDigit(int size) {
        if (size < 10) {
            return "0" + size;
        } else {
            return "" + size;
        }
    }
}
