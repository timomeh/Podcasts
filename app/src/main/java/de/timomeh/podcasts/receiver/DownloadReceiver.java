package de.timomeh.podcasts.receiver;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import de.timomeh.podcasts.services.DownloadService;

/**
 * Created by Timo Maemecke (@timomeh) on 21/02/15.
 * <p/>
 * TODO: Add a class header comment
 */
public class DownloadReceiver extends ResultReceiver {
    public DownloadReceiver(Handler handler) {
        super(handler);
    }

    public interface OnProgressListener {
        public void onProgress(int progress);
        public void onFinished(String destination);
    }

    private OnProgressListener mOnProgressListener;

    public void setOnProgressListener(OnProgressListener listener) {
        mOnProgressListener = listener;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        super.onReceiveResult(resultCode, resultData);
        if (resultCode == DownloadService.UPDATE_PROGRESS) {
            mOnProgressListener.onProgress(resultData.getInt("progress"));
        } else if (resultCode == DownloadService.UPDATE_FINISHED) {
            mOnProgressListener.onFinished(resultData.getString("destination"));
        }
    }
}
