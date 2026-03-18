package com.nd.me.filesystem;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaDataSource;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.nd.me.R;
import com.nd.me.util.StorageUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(IFileItem file);
    }

    public interface OnSelectionChangeListener {
        void onSelectionChanged(List<IFileItem> fileItems);
    }

    private List<IFileItem> items;
    private OnItemClickListener listener;
    private OnSelectionChangeListener selectionChangeListener;

    private IFileProvider fileProvider;

    private final ExecutorService imageExecutor = Executors.newFixedThreadPool(1);
    private final ExecutorService videoExecutor = Executors.newFixedThreadPool(1);


    private final Set<String> selected = new HashSet<>();
    private boolean selectionMode = false;

    public FileAdapter(List<IFileItem> items, IFileProvider fileProvider, OnItemClickListener listener, OnSelectionChangeListener selectionChangeListener) {
        this.items = items;
        this.fileProvider = fileProvider;
        this.listener = listener;
        this.selectionChangeListener = selectionChangeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        IFileItem file = items.get(position);
        boolean isSelected = selected.contains(file.getPath());
        holder.itemView.setActivated(isSelected);

        holder.fileName.setText(file.getName());
        String path = file.getPath();
        holder.fileIcon.setTag(path);
        if (file.isDirectory()) {
            holder.fileIcon.setImageResource(R.drawable.round_folder_24);
        } else if (StorageUtils.isImage(file.getPath())) {
            holder.fileIcon.setImageResource(R.drawable.round_image_24);
            File thumbFile = StorageUtils.getThumbFile(holder.fileIcon.getContext(), file.getUrl());
            if (thumbFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath());
                holder.fileIcon.setImageBitmap(bitmap);
            } else {
                imageExecutor.execute(() -> {
                    try (InputStream inputStream = fileProvider.getInputStream(file.getPath())) {
                        byte[] data = StorageUtils.readAllBytes(inputStream);
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeByteArray(data, 0, data.length, options);
                        options.inSampleSize = calculateInSampleSize(options, 200, 200);
                        options.inJustDecodeBounds = false;
                        options.inPreferredConfig = Bitmap.Config.RGB_565;
                        options.inDither = true;
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                        holder.fileIcon.post(() -> {
                            if (!path.equals(holder.fileIcon.getTag())) {
                                return;
                            }
                            holder.fileIcon.setImageBitmap(bitmap);
                            StorageUtils.saveThumb(holder.fileIcon.getContext(), file.getUrl(), bitmap);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } else if (StorageUtils.isVideo(file.getPath())) {
            holder.fileIcon.setImageResource(R.drawable.round_video_file_24);
            File thumbFile = StorageUtils.getThumbFile(holder.fileIcon.getContext(), file.getUrl());
            if (thumbFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath());
                holder.fileIcon.setImageBitmap(bitmap);
            } else {
                videoExecutor.execute(() -> {
                    try {
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(new MediaDataSource() {
                            @Override
                            public long getSize() {
                                return file.getSize();
                            }

                            @Override
                            public int readAt(long pos, byte[] buffer, int offset, int size) {
                                try (BufferedInputStream in = new BufferedInputStream(fileProvider.getInputStreamOffset(file.getPath(), pos))) {
                                    return in.read(buffer, offset, size);
                                } catch (Exception e) {

                                }
                                return -1;
                            }

                            @Override
                            public void close() {

                            }
                        });
                        long atTime = 0;
                        if (file.getSize() > 100_000_000L) {
                            atTime = 100_000_000L;
                        }
                        Bitmap bitmap = retriever.getFrameAtTime(atTime, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                        if (bitmap == null) {
                            return;
                        }
                        holder.fileIcon.post(() -> {
                            if (!path.equals(holder.fileIcon.getTag())) {
                                return;
                            }
                            holder.fileIcon.setImageBitmap(bitmap);
                            StorageUtils.saveThumb(holder.fileIcon.getContext(), file.getUrl(), bitmap);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } else if (StorageUtils.isMusic(file.getPath())) {
            holder.fileIcon.setImageResource(R.drawable.round_audio_file_24);
        } else {
            holder.fileIcon.setImageResource(R.drawable.round_insert_drive_file_24);
        }

        holder.fileInfo.setText(StorageUtils.formatSize(file.getSize()));

        Date date = new Date(file.getModTime());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
        String formatted = sdf.format(date);
        holder.fileTime.setText(formatted);
        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                toggleSelection(file);
                notifyItemChanged(holder.getAdapterPosition());
            } else {
                listener.onItemClick(file);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                selectionMode = true;
            }
            toggleSelection(file);
            notifyItemChanged(holder.getAdapterPosition());
            return true;
        });
        //holder.itemView.setBackgroundColor(isSelected ? 0x330096FF : Color.TRANSPARENT);
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        int height = options.outHeight;
        int width = options.outWidth;

        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIcon;
        TextView fileName, fileInfo, fileTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.file_icon);
            fileName = itemView.findViewById(R.id.file_name);
            fileInfo = itemView.findViewById(R.id.file_info);
            fileTime = itemView.findViewById(R.id.file_time);
        }
    }


    public boolean isSelectionMode() {
        return selectionMode;
    }

    public List<IFileItem> getSelectedItems() {
        List<IFileItem> list = new ArrayList<>();
        for (IFileItem item : items) {
            if (selected.contains(item.getPath())) {
                list.add(item);
            }
        }
        return list;
    }

    public void clearSelection() {
        selected.clear();
        selectionMode = false;
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(getSelectedItems());
        }
    }

    public int getSelectedCount() {
        return selected.size();
    }

    private void toggleSelection(IFileItem file) {

        String path = file.getPath();

        if (selected.contains(path)) {
            selected.remove(path);
        } else {
            selected.add(path);
        }

        if (selected.isEmpty()) {
            selectionMode = false;
        }

        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(getSelectedItems());
        }
    }
}
