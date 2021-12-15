package com.vidyo.vidyoconnector.utils;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FontsUtils {

    private final static String FONT_FILE_NAME = "System.vyf";

    private final Context mContext;

    public FontsUtils(Context context) {
        mContext = context;
    }

    public File fontFile() {
        File fontFile = new File(mContext.getFilesDir(), FONT_FILE_NAME);

        if (fontFile.exists()) {
            Logger.i("Font file exists, do nothing");
        } else {
            AssetManager assetManager = mContext.getAssets();
            Logger.i("Write font file from assets to storage.");

            try (InputStream input = assetManager.open(FONT_FILE_NAME);
                 OutputStream output = new FileOutputStream(fontFile)) {

                Logger.i("Font file does not exist, write it");

                byte[] buffer = new byte[input.available()];
                int length;
                while ((length = input.read(buffer)) != -1) {
                    output.write(buffer, 0, length);
                }

                Logger.e("Font file has been written.");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Logger.e("Font file is not found");
            } catch (IOException e) {
                e.printStackTrace();
                Logger.e("Error while writing the font file");
            }
        }

        return fontFile;
    }
}