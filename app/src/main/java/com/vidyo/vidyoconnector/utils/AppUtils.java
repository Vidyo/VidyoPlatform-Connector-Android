package com.vidyo.vidyoconnector.utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import com.vidyo.vidyoconnector.BuildConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppUtils {

    private static final String LOGS_FOLDER = "VidyoConnectorLogs";
    private static final String LOG_FILE_NAME = "VidyoConnectorLog";
    private static final String LOG_FILE = LOG_FILE_NAME + ".log";

    /**
     * Log file is create individually for every session
     *
     * @param context {@link Context}
     * @return log file path
     */
    public static String configLogFile(Context context) {
        File cacheDir = context.getCacheDir();
        File logDir = new File(cacheDir, LOGS_FOLDER);
        deleteRecursive(logDir);

        File logFile = new File(logDir, LOG_FILE);
        logFile.mkdirs();

        String[] logFiles = logDir.list();
        if (logFiles != null)
            for (String file : logFiles) Logger.i(AppUtils.class, "Cached log file: " + file);

        return logFile.getAbsolutePath();
    }

    /**
     * Expose log file URI for sharing.
     *
     * @param context {@link Context}
     * @return log file uri.
     */
    private static ArrayList<Uri> logFileUri(Context context) {
        File cacheDir = context.getCacheDir();
        File logDir = new File(cacheDir, LOGS_FOLDER);

        ArrayList<Uri> uris = new ArrayList<>();

        for (String file : logDir.list()) {
            if (file.startsWith(LOG_FILE_NAME)) {
                File logFile = new File(logDir, file);
                uris.add(FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".file.provider", logFile));
            }
        }

        Logger.i("Log file uris: " + uris.size());
        return uris;
    }

    private static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
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

    public static boolean isLandscape(Resources resources) {
        return resources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static <T> void dump(List<T> list) {
        for (T t : list) Logger.i("Item: %s", t.toString());
    }

    public static boolean isEmulator() {
        return (Build.MANUFACTURER.contains("Genymotion")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.toLowerCase().contains("droid4x")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.HARDWARE.equalsIgnoreCase("goldfish")
                || Build.HARDWARE.equalsIgnoreCase("vbox86")
                || Build.HARDWARE.toLowerCase().contains("nox")
                || Build.FINGERPRINT.startsWith("generic")
                || Build.PRODUCT.equalsIgnoreCase("sdk")
                || Build.PRODUCT.equalsIgnoreCase("google_sdk")
                || Build.PRODUCT.equalsIgnoreCase("sdk_x86")
                || Build.PRODUCT.equalsIgnoreCase("vbox86p")
                || Build.PRODUCT.toLowerCase().contains("nox")
                || Build.BOARD.toLowerCase().contains("nox")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")));
    }
}