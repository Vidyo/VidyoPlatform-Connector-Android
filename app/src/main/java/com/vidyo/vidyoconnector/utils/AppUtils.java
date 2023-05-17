package com.vidyo.vidyoconnector.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import com.vidyo.vidyoconnector.BuildConfig;
import com.vidyo.vidyoconnector.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class AppUtils {

    private static final String DEFAULT_LOG_FILE_NAME = "VidyoClient";

    /**
     * Expose log file URI for sharing.
     *
     * @param context {@link Context}
     * @return log file uri.
     */
    private static ArrayList<Uri> logFileUri(Context context) {
        File cacheDir = context.getCacheDir();
        if (cacheDir == null || cacheDir.list() == null) return new ArrayList<>();

        ArrayList<Uri> uris = new ArrayList<>();
        for (String file : cacheDir.list()) {
            if (file.startsWith(DEFAULT_LOG_FILE_NAME)) {
                File logFile = new File(cacheDir, file);
                uris.add(FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".file.provider", logFile));
            }
        }

        Logger.i("Log file uris: " + uris.size());
        return uris;
    }

    /**
     * Send email with log file
     */
    public static void sendLogs(Context context) {
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Vidyo Connector Sample Logs");
        intent.putExtra(Intent.EXTRA_TEXT, "Logs attached..." + additionalInfo());

        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, logFileUri(context));

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(Intent.createChooser(intent, "Choose sender..."));
        } catch (Exception sendReportEx) {
            sendReportEx.printStackTrace();
        }
    }

    private static String additionalInfo() {
        return "\n\nModel: " + Build.MODEL +
                "\n" + "Manufactured: " + Build.MANUFACTURER +
                "\n" + "Brand: " + Build.BRAND +
                "\n" + "Android OS version: " + Build.VERSION.RELEASE +
                "\n" + "Hardware : " + Build.HARDWARE +
                "\n" + "SDK Version : " + Build.VERSION.SDK_INT;
    }

    private static final String CERTIFICATE_RAW_NAME = "ca-certificates.crt";

    public static String writeCaCertificates(Context context) {
        try {
            InputStream caCertStream = context.getResources().openRawResource(R.raw.ca_certificates);

            File caCertDirectory = new File(context.getFilesDir() + File.separator);
            File caFile = new File(caCertDirectory, CERTIFICATE_RAW_NAME);

            FileOutputStream caCertFile = new FileOutputStream(caFile);

            byte[] buffer = new byte[1024];
            int len;

            while ((len = caCertStream.read(buffer)) != -1) {
                caCertFile.write(buffer, 0, len);
            }

            caCertStream.close();
            caCertFile.close();
            return caFile.getPath();
        } catch (Exception e) {
            return null;
        }
    }
}