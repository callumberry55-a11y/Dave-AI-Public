package com.local.daveai.storage;

import android.content.Context;
import android.util.AtomicFile;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FileAccessManager {

    private static final String TAG = "FileAccessManager";
    private final File baseDir;

    public FileAccessManager(@NonNull Context context, @NonNull String subDirectory) {
        this.baseDir = new File(context.getFilesDir(), subDirectory);
        if (!this.baseDir.exists()) {
            boolean created = this.baseDir.mkdirs();
            if (!created) {
                Log.w(TAG, "Directory already exists or failed to create: " + baseDir.getAbsolutePath());
            }
        }
    }

    /**
     * Atomically writes text data to disk.
     */
    public synchronized boolean writeAtomic(@NonNull String fileName, @NonNull String content) {
        File targetFile = new File(baseDir, fileName);
        AtomicFile atomicFile = new AtomicFile(targetFile);
        FileOutputStream fos = null;

        try {
            fos = atomicFile.startWrite();
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            fos.write(bytes);
            atomicFile.finishWrite(fos);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Write failed for " + fileName + ", rolling back", e);
            if (fos != null) {
                atomicFile.failWrite(fos);
            }
            return false;
        }
    }

    /**
     * Reads complete file content into a String.
     */
    @Nullable
    public synchronized String readAtomic(@NonNull String fileName) {
        File targetFile = new File(baseDir, fileName);
        if (!targetFile.exists()) {
            return null;
        }

        AtomicFile atomicFile = new AtomicFile(targetFile);
        try (FileInputStream fis = atomicFile.openRead()) {
            byte[] data = atomicFile.readFully();
            return new String(data, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "Failed reading " + fileName, e);
            return null;
        }
    }

    /**
     * Deletes base file and legacy backups if present.
     */
    public synchronized boolean deleteFile(@NonNull String fileName) {
        File targetFile = new File(baseDir, fileName);
        AtomicFile atomicFile = new AtomicFile(targetFile);
        atomicFile.delete();
        return !targetFile.exists();
    }

    /**
     * Lists existing managed files in this directory.
     */
    @NonNull
    public synchronized String[] listManagedFiles() {
        String[] files = baseDir.list();
        return files != null ? files : new String[0];
    }
}
