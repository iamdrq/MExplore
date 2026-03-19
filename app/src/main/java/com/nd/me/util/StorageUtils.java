package com.nd.me.util;

import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import com.nd.me.filesystem.FileProviderFactory;
import com.nd.me.filesystem.IFileItem;
import com.nd.me.filesystem.IFileProvider;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StorageUtils {

    public enum PasteActionMode {
        CUT, COPY
    }

    public interface PasteListener {
        void onResult(boolean success, Exception e);
    }

    public static Bitmap BITMAP_SHARE;
    public static byte[] IMAGE_BYTE_SHARE;
    public static PasteActionMode PASTE_ACTION;
    public static List<IFileItem> PASTE_FILE_ITEMS = new ArrayList<>();

    private final static ExecutorService executorService = Executors.newFixedThreadPool(1);

    public static long getExternalTotalSpace() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return blockSize * totalBlocks;
    }

    public static long getExternalAvailableSpace() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return blockSize * availableBlocks;
    }

    public static long getExternalUsedSpace() {
        return getExternalTotalSpace() - getExternalAvailableSpace();
    }

    public static long getTotalSpace(Context context) {
        StorageStatsManager storageStatsManager = (StorageStatsManager) context.getSystemService(Context.STORAGE_STATS_SERVICE);
        try {
            return storageStatsManager.getTotalBytes(StorageManager.UUID_DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static long getTotalFreeSpace(Context context) {
        StorageStatsManager storageStatsManager = (StorageStatsManager) context.getSystemService(Context.STORAGE_STATS_SERVICE);
        try {
            return storageStatsManager.getFreeBytes(StorageManager.UUID_DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static long getTotalUsedSpace(Context context) {
        return getTotalSpace(context) - getTotalFreeSpace(context);
    }

    public static void saveThumb(Context context, String url, Bitmap bitmap) {
        if (bitmap == null || url == null) return;

        File thumbDir = new File(context.getCacheDir(), "thumb");
        if (!thumbDir.exists()) {
            thumbDir.mkdirs();
        }

        String fileName = md5(url) + ".jpg";
        File img = new File(thumbDir, fileName);

        if (img.exists()) return;

        try (FileOutputStream fos = new FileOutputStream(img)) {
            Bitmap resizeImg = resize(bitmap, 200);
            resizeImg.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File getThumbFile(Context context, String url) {
        File thumbDir = new File(context.getCacheDir(), "thumb");
        return new File(thumbDir, md5(url) + ".jpg");
    }

    public static byte[] readAllBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] tmp = new byte[16384];
        int n;
        while ((n = inputStream.read(tmp)) != -1) {
            buffer.write(tmp, 0, n);
        }
        return buffer.toByteArray();
    }

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static Bitmap resize(Bitmap src, int maxWidth) {
        if (src.getWidth() <= maxWidth) return src;

        float ratio = (float) maxWidth / src.getWidth();
        int newHeight = (int) (src.getHeight() * ratio);

        return Bitmap.createScaledBitmap(src, maxWidth, newHeight, true);
    }

    private static final DecimalFormat DF = new DecimalFormat("#.##");

    public static String formatSize(long size) {
        if (size >= 1024L * 1024 * 1024)
            return DF.format(size / (1024f * 1024 * 1024)) + " GB";
        if (size >= 1024L * 1024)
            return DF.format(size / (1024f * 1024)) + " MB";
        if (size >= 1024)
            return DF.format(size / 1024f) + " KB";
        return size + " B";
    }

    public static String formatSize1G(long size) {
        if (size >= 1000L * 1000 * 1000)
            return DF.format(size / (1000f * 1000 * 1000)) + " GB";
        if (size >= 1000L * 1000)
            return DF.format(size / (1000f * 1000)) + " MB";
        if (size >= 1000)
            return DF.format(size / 1000f) + " KB";
        return size + " B";
    }

    public static boolean isVideo(String url) {
        url = url.toLowerCase();
        return url.endsWith(".mp4") || url.endsWith(".mkv") || url.endsWith(".ts") || url.endsWith(".avi") || url.endsWith(".wmv") || url.endsWith(".mov") || url.endsWith(".webm");
    }

    public static boolean isImage(String url) {
        url = url.toLowerCase();
        return url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png") || url.endsWith(".bmp") || url.endsWith(".webp");
    }

    public static boolean isMusic(String url) {
        url = url.toLowerCase();
        return url.endsWith(".wav") || url.endsWith(".wma") || url.endsWith(".mp3") || url.endsWith(".flac") || url.endsWith(".ogg");
    }

    public static boolean isSubtitle(String url) {
        url = url.toLowerCase();
        return url.endsWith(".srt")
                || url.endsWith(".ass")
                || url.endsWith(".ssa")
                || url.endsWith(".vtt")
                || url.endsWith(".sub")
                || url.endsWith(".ttml");
    }

    public static void paste(IFileItem toFileItem, PasteListener pasteListener) {
        if (PASTE_FILE_ITEMS.isEmpty()) {
            return;
        }
        List<IFileItem> fileItemList = new ArrayList<>(PASTE_FILE_ITEMS);
        executorService.execute(() -> {
            try {
                IFileItem fileItem = fileItemList.getFirst();
                SimpleURI uriFrom = new SimpleURI(fileItem.getUrl());
                SimpleURI uriTo = new SimpleURI(toFileItem.getUrl());
                String fromId = fileItem.getUrl().replace(uriFrom.getPath(), "");
                String toId = toFileItem.getUrl().replace(uriTo.getPath(), "");
                if ((fromId).equals(toId)) {
                    IFileProvider fileProvider = FileProviderFactory.create(toFileItem.getUrl());
                    for (IFileItem item : fileItemList) {
                        if (PASTE_ACTION == PasteActionMode.CUT) {
                            fileProvider.move(item.getPath(), toFileItem.getPath());
                        } else if (PASTE_ACTION == PasteActionMode.COPY) {
                            fileProvider.copy(item.getPath(), toFileItem.getPath());
                        }
                    }
                    fileProvider.close();
                } else {
                    IFileProvider toFileProvider = FileProviderFactory.create(toFileItem.getUrl());
                    IFileProvider fromFileProvider = FileProviderFactory.create(fileItem.getUrl());
                    for (IFileItem item : fileItemList) {
                        Path src = Paths.get(item.getPath());
                        Path targetRoot = Paths.get(toFileItem.getPath()).resolve(src.getFileName());
                        walkCopy(item, targetRoot, fromFileProvider, toFileProvider);
                        if (PASTE_ACTION == PasteActionMode.CUT) {
                            fromFileProvider.delete(item);
                        }
                    }
                    toFileProvider.close();
                    fromFileProvider.close();
                }
                pasteListener.onResult(true, null);
            } catch (Exception e) {
                e.printStackTrace();
                pasteListener.onResult(false, e);
            }
        });
    }

    public static void walkCopy(IFileItem source, Path target, IFileProvider fromFileProvider, IFileProvider toFileProvider) throws Exception {
        if (source.isDirectory()) {
            toFileProvider.mkdir(target.toString());
            List<IFileItem> files = fromFileProvider.listFiles(source.getPath());
            for (IFileItem file : files) {
                walkCopy(file, Paths.get(target.toString(), file.getName()), fromFileProvider, toFileProvider);
            }
        } else {
            try (InputStream in = fromFileProvider.getInputStream(source.getPath());
                 OutputStream out = toFileProvider.getOutputstream(target.toString())) {
                in.transferTo(out);
            }
        }
    }
}
