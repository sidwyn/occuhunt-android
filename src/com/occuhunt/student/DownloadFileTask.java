package com.occuhunt.student;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class DownloadFileTask extends AsyncTask<String, Void, String> {

    private final Context mContext;
    
    public DownloadFileTask(Context context) {
        mContext = context;
    }
    
    @Override
    protected String doInBackground(String... url) {
        String fullPath;

        try {
            InputStream input = new URL(url[0]).openStream();
            String filename = url[0].substring(url[0].lastIndexOf('/') + 1);

            File filesDir = mContext.getFilesDir();
            fullPath = filesDir.toString() + "/" + filename;
            OutputStream output = new FileOutputStream(fullPath);

            try {
                byte[] buffer = new byte[1024];
                int bytesRead = 0;

                while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
                    output.write(buffer, 0, bytesRead);
                }
            } finally {
                output.close();
                input.close();
            }
        } catch (Exception e) {
            Log.e("DownloadImageTask", e.toString());
            return null;
        }

        return fullPath;
    }
}