package de.timomeh.podcasts.services;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

/**
 * Created by Timo Maemecke (@timomeh) on 21/02/15.
 * <p/>
 * TODO: Add a class header comment
 */
public class DownloadService extends IntentService {
    public static final int UPDATE_PROGRESS = 1608;
    public static final int UPDATE_FINISHED = 1609;

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String downloadUrl = intent.getStringExtra("url");
        String destinationDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS) + "/";
        Uri path = Uri.parse(downloadUrl);
        String destination = destinationDirectory + "podcasts_" + (new Date().getTime() / 1000) + "_" + path.getLastPathSegment();
        ResultReceiver receiver = intent.getParcelableExtra("receiver");

        try {
            URL url = new URL(downloadUrl);
            URLConnection connection = url.openConnection();
            connection.connect();
            int fileLength = connection.getContentLength();

            InputStream input = new BufferedInputStream(connection.getInputStream());
            OutputStream output = new FileOutputStream(destination);

            byte data[] = new byte[1024];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                total += count;
                Bundle resultData = new Bundle();
                int progress = -1;
                if (fileLength != -1) {
                    progress = (int) ((total * 100l) / fileLength);
                }
                resultData.putInt("progress", progress);
                receiver.send(UPDATE_PROGRESS, resultData);
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();
        } catch (Exception e) {
            Log.e("DownloadService", "Exception: ", e);
        }

        Bundle resultData = new Bundle();
        resultData.putInt("progress", 100);
        resultData.putString("destination", destination);
        receiver.send(UPDATE_FINISHED, resultData);
    }
}
