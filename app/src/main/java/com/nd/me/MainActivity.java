package com.nd.me;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import android.os.Environment;
import android.provider.CallLog;
import android.provider.Telephony;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nd.me.filesystem.NetwokListAdapter;
import com.nd.me.util.NetworkLink;
import com.nd.me.util.StorageUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor sharedPreferencesEditor;

    List<NetworkLink> networkList;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;

    TextView localStorageSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPreferences = getSharedPreferences("network_list", Context.MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        MaterialCardView local_storage_card = findViewById(R.id.local_storage_card);
        local_storage_card.setOnClickListener((v) -> {

            Intent intent = new Intent(MainActivity.this, ExploreActivity.class);
            intent.putExtra("url", "file://" + Environment.getExternalStorageDirectory().getPath());
            startActivity(intent);
        });

        localStorageSize = findViewById(R.id.local_storage_size);

        ImageButton network_storage_add = findViewById(R.id.network_storage_add);
        network_storage_add.setOnClickListener(v -> {
            View addNetworkView = LayoutInflater.from(this)
                    .inflate(R.layout.add_network, null, false);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.network_protocols,
                    android.R.layout.simple_list_item_1
            );
            AutoCompleteTextView network_protocol = addNetworkView.findViewById(R.id.network_protocol);
            network_protocol.setAdapter(adapter);

            TextInputEditText network_name = addNetworkView.findViewById(R.id.network_name);
            TextInputEditText network_host = addNetworkView.findViewById(R.id.network_host);
            TextInputEditText network_port = addNetworkView.findViewById(R.id.network_port);
            TextInputEditText network_path = addNetworkView.findViewById(R.id.network_path);
            TextInputEditText network_username = addNetworkView.findViewById(R.id.network_username);
            TextInputEditText network_password = addNetworkView.findViewById(R.id.network_password);

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.add_network_storage)
                    .setIcon(R.drawable.round_share_24)
                    .setView(addNetworkView)
                    .setCancelable(false)
                    .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                    .setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                        NetworkLink networkLink = new NetworkLink(new Date().getTime(), network_name.getText().toString(),
                                network_protocol.getText().toString(), network_host.getText().toString(),
                                network_port.getText().toString(), network_path.getText().toString(),
                                network_username.getText().toString(), network_password.getText().toString());
                        sharedPreferencesEditor.putString(String.valueOf(networkLink.id), networkLink.toJsonString());
                        sharedPreferencesEditor.commit();

                        networkList.clear();
                        networkList.addAll(getNetworkList(sharedPreferences));
                        recyclerView.getAdapter().notifyDataSetChanged();
                    })
                    .show();
        });

        networkList = getNetworkList(sharedPreferences);

        recyclerView = findViewById(R.id.recyclerView);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        NetwokListAdapter adapter = new NetwokListAdapter(networkList, networkLink -> {
            Intent intent = new Intent(MainActivity.this, ExploreActivity.class);
            intent.putExtra("url", networkLink.buildUrl());
            startActivity(intent);
        }, (v, networkLink) -> {
            PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
            popupMenu.inflate(R.menu.network_item_menu);
            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_delete) {
                    sharedPreferencesEditor.remove(String.valueOf(networkLink.id));
                    sharedPreferencesEditor.commit();
                    networkList.clear();
                    networkList.addAll(getNetworkList(sharedPreferences));
                    recyclerView.getAdapter().notifyDataSetChanged();
                    return true;
                }
                return false;
            });
            popupMenu.show();
            return true;
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        localStorageSize.setText(StorageUtils.formatSize1G(StorageUtils.getTotalUsedSpace(this))
                + " / " + StorageUtils.formatSize1G(StorageUtils.getTotalSpace(this)));
    }

    public List<NetworkLink> getNetworkList(SharedPreferences sharedPreferences) {
        Map<String, ?> allEntries = sharedPreferences.getAll();
        List<Map.Entry<String, ?>> entryList = new ArrayList<>(allEntries.entrySet());
        entryList.sort((e1, e2) -> {
            long t1 = Long.parseLong(e1.getKey());
            long t2 = Long.parseLong(e2.getKey());
            return Long.compare(t1, t2);
        });
        List<NetworkLink> result = new ArrayList<>();
        for (Map.Entry<String, ?> entry : entryList) {
            result.add(NetworkLink.fromJsonString(entry.getValue().toString()));
        }
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_settings) {

            return true;
        } else if (item.getItemId() == R.id.action_url) {
            LinearLayout container = new LinearLayout(this);
            int marginTop = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
            int marginLeft = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
            container.setPadding(marginLeft, marginTop, marginLeft, marginTop);
            ViewGroup.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            container.setLayoutParams(layoutParams);
            TextInputLayout textInputLayout = new TextInputLayout(this);
            TextInputEditText editText = new TextInputEditText(textInputLayout.getContext());
            editText.setHint(R.string.network_url);
            textInputLayout.addView(editText);
            textInputLayout.setLayoutParams(layoutParams);
            container.addView(textInputLayout);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.network_url)
                    .setIcon(R.drawable.round_link_24)
                    .setView(container)
                    .setCancelable(false)
                    .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                    .setPositiveButton(getString(R.string.play), (dialog, which) -> {
                        Intent intent = new Intent(this, VideoActivity.class);
                        intent.putExtra("url", editText.getText().toString());
                        startActivity(intent);
                    })
                    .show();
        }

        return super.onOptionsItemSelected(item);
    }
}