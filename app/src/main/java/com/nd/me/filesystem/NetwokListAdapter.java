package com.nd.me.filesystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.nd.me.R;
import com.nd.me.util.NetworkLink;

import java.util.List;

public class NetwokListAdapter extends RecyclerView.Adapter<NetwokListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(NetworkLink networkLink);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(View view, NetworkLink networkLink);
    }

    private List<NetworkLink> items;
    private OnItemClickListener listener;
    private OnItemLongClickListener longListener;

    public NetwokListAdapter(List<NetworkLink> items, OnItemClickListener listener, OnItemLongClickListener longListener) {
        this.items = items;
        this.listener = listener;
        this.longListener = longListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_network, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NetworkLink networkLink = items.get(position);

        holder.fileProtocol.setText(networkLink.protocol);

        holder.fileName.setText(networkLink.name);

        holder.fileInfo.setText(networkLink.host);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(networkLink));
        holder.itemView.setOnLongClickListener(v -> longListener.onItemLongClick(v, networkLink));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView fileName, fileInfo, fileProtocol;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fileProtocol = itemView.findViewById(R.id.file_protocol);
            fileName = itemView.findViewById(R.id.file_name);
            fileInfo = itemView.findViewById(R.id.file_info);
        }
    }
}
