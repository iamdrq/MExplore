package com.nd.me;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nd.me.filesystem.*;
import com.nd.me.util.SimpleURI;
import com.nd.me.util.StorageUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExploreActivity extends AppCompatActivity {

    public static final String EXTRA_SELECT_SUBTITLE = "select_subtitle";
    public static final String EXTRA_SELECTED_URL = "selected_url";

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private SwipeRefreshLayout swipeRefresh;

    private IFileProvider fileProvider;
    private String currentPath;
    private String initPath;

    private String url;

    private String baseUrl;
    private List<IFileItem> currentFileList;
    private List<IFileItem> selectedFileList;
    private FileSortType currentSortType = FileSortType.NAME_ASC;
    private Map<String, Parcelable> scrollStates = new HashMap<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    private int requestId = 0;
    private boolean selectSubtitleMode = false;

    MaterialToolbar toolbar;
    TextView toolbarTitle;

    ImageButton btnSort, btnDelete, btnRename, btnCut, btnCopy, btnPaste;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_explore);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        url = getIntent().getStringExtra("url");
        selectSubtitleMode = getIntent().getBooleanExtra(EXTRA_SELECT_SUBTITLE, false);
        try {
            currentPath = new SimpleURI(url).getPath();
            baseUrl = url.replace(currentPath, "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Log.e("currentPath", currentPath);
        initPath = currentPath;

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText(currentPath);

        recyclerView = findViewById(R.id.recyclerView);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::refreshCurrentDirectory);

        btnSort = findViewById(R.id.btn_sort);
        btnSort.setOnClickListener(this::showSortPopup);
        btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(this::showDelete);
        btnRename = findViewById(R.id.btn_rename);
        btnRename.setOnClickListener(this::showRename);

        btnCut = findViewById(R.id.btn_cut);
        btnCut.setOnClickListener(v -> {
            StorageUtils.PASTE_ACTION = StorageUtils.PasteActionMode.CUT;
            StorageUtils.PASTE_FILE_ITEMS = new ArrayList<>(((FileAdapter) recyclerView.getAdapter()).getSelectedItems());
            ((FileAdapter) recyclerView.getAdapter()).clearSelection();
            updatePasteBtn();
        });
        btnCopy = findViewById(R.id.btn_copy);
        btnCopy.setOnClickListener(v -> {
            StorageUtils.PASTE_ACTION = StorageUtils.PasteActionMode.COPY;
            StorageUtils.PASTE_FILE_ITEMS = new ArrayList<>(((FileAdapter) recyclerView.getAdapter()).getSelectedItems());
            ((FileAdapter) recyclerView.getAdapter()).clearSelection();
            updatePasteBtn();
        });
        btnPaste = findViewById(R.id.btn_paste);
        updatePasteBtn();
        btnPaste.setOnLongClickListener(v -> {
            StorageUtils.PASTE_FILE_ITEMS.clear();
            updatePasteBtn();
            Toast.makeText(this, getString(R.string.paste_cancel), Toast.LENGTH_LONG).show();
            return true;
        });
        btnPaste.setOnClickListener(v -> {
            StorageUtils.paste(new IFileItem() {
                @Override
                public String getName() {
                    return "";
                }

                @Override
                public long getModTime() {
                    return 0;
                }

                @Override
                public long getSize() {
                    return 0;
                }

                @Override
                public boolean isDirectory() {
                    return true;
                }

                @Override
                public String getPath() {
                    return currentPath;
                }

                @Override
                public String getUrl() {
                    return baseUrl + currentPath;
                }
            }, (success, e) -> {
                runOnUiThread(() -> {
                    if (!success) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        showFiles(currentPath);
                    }
                });
            });
            StorageUtils.PASTE_FILE_ITEMS.clear();
            updatePasteBtn();
        });

        try {
            fileProvider = FileProviderFactory.create(url);
            showFiles(currentPath);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePasteBtn() {
        btnPaste.setVisibility(StorageUtils.PASTE_FILE_ITEMS.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showRename(View anchor) {
        IFileItem fileItem = selectedFileList.getFirst();
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
        editText.setHint(R.string.rename);
        editText.setText(fileItem.getName());
        textInputLayout.addView(editText);
        textInputLayout.setLayoutParams(layoutParams);
        container.addView(textInputLayout);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.rename)
                .setIcon(R.drawable.save_as_24px)
                .setView(container)
                .setCancelable(false)
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                .setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                    if (editText.getText().isEmpty()) {
                        return;
                    }
                    Path oldPath = Paths.get(fileItem.getPath());
                    Path newPath = oldPath.resolveSibling(editText.getText().toString());
                    ((FileAdapter) recyclerView.getAdapter()).clearSelection();
                    executorService.execute(() -> {
                        try {
                            fileProvider.rename(oldPath.toString(), newPath.toString());
                            runOnUiThread(() -> showFiles(currentPath));
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(this, getString(R.string.rename) + e, Toast.LENGTH_LONG).show());
                        }
                    });
                })
                .show();
    }

    private void showDelete(View anchor) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.delete))
                .setIcon(R.drawable.delete_24px)
                .setMessage(getString(R.string.delete) + " " + (selectedFileList.size() == 1 ? selectedFileList.getFirst().getName() : selectedFileList.size() + " " + getString(R.string.files)) + " ?")
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    List<IFileItem> deleteFileList = new ArrayList<>(selectedFileList);
                    ((FileAdapter) recyclerView.getAdapter()).clearSelection();
                    executorService.execute(() -> {
                        try {
                            for (IFileItem file : deleteFileList) {
                                fileProvider.delete(file);
                                runOnUiThread(() -> {
                                    currentFileList.removeIf((i) -> i.getPath().equals(file.getPath()));
                                    recyclerView.getAdapter().notifyDataSetChanged();
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> Toast.makeText(this,  e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });
                }).show();
    }

    private void showSortPopup(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        Menu menu = popup.getMenu();

        createSortMenuItem(menu, getString(R.string.sort_by_name), FileSortType.NAME_ASC, FileSortType.NAME_DESC);
        createSortMenuItem(menu, getString(R.string.sort_by_size), FileSortType.SIZE_ASC, FileSortType.SIZE_DESC);
        createSortMenuItem(menu, getString(R.string.sort_by_date), FileSortType.DATE_ASC, FileSortType.DATE_DESC);

        popup.setOnMenuItemClickListener(item -> {
            FileSortType[] types = (FileSortType[]) item.getIntent().getSerializableExtra("sortTypes");
            FileSortType ascType = types[0];
            FileSortType descType = types[1];

            currentSortType = (currentSortType == ascType) ? descType : ascType;

            for (int i = 0; i < menu.size(); i++) {
                MenuItem menuItem = menu.getItem(i);
                menuItem.setTitle(menuItem.getTitle().toString().replace(" ↑", "").replace(" ↓", ""));
            }

            String title = item.getTitle().toString();

            if (currentSortType == ascType) {
                title += " ↑";
            } else if (currentSortType == descType) {
                title += " ↓";
            }

            item.setTitle(title);

            if (currentFileList != null) {
                FileSorter.sort(currentFileList, currentSortType);
                recyclerView.getAdapter().notifyDataSetChanged();
            }
            return true;
        });

        popup.show();
    }

    private void createSortMenuItem(Menu menu, String title, FileSortType ascType, FileSortType descType) {
        if (currentSortType == ascType) {
            title += " ↑";
        } else if (currentSortType == descType) {
            title += " ↓";
        }
        MenuItem item = menu.add(title);
        Intent intent = new Intent();
        intent.putExtra("sortTypes", new FileSortType[]{ascType, descType});
        item.setIntent(intent);
    }

    private void showFiles(String path) {
        swipeRefresh.setRefreshing(true);
        try {
            if (currentPath != null) {
                scrollStates.put(currentPath, recyclerView.getLayoutManager().onSaveInstanceState());
            }

            currentPath = path;
            toolbarTitle.setText(currentPath);

            int currentRequest = ++requestId;

            executorService.execute(() -> {
                try {
                    currentFileList = fileProvider.listFiles(path);
                    if (selectSubtitleMode) {
                        currentFileList.removeIf(file -> !file.isDirectory() && !StorageUtils.isSubtitle(file.getPath()));
                    }
                    FileSorter.sort(currentFileList, currentSortType);

                    runOnUiThread(() -> {
                        if (currentRequest != requestId) {
                            return;
                        }

                        swipeRefresh.setRefreshing(false);

                        FileAdapter adapter = new FileAdapter(currentFileList, fileProvider, file -> {
                            if (file.isDirectory()) {
                                showFiles(file.getPath());
                            } else {
                                if (selectSubtitleMode) {
                                    Intent result = new Intent();
                                    result.putExtra(EXTRA_SELECTED_URL, file.getUrl());
                                    setResult(RESULT_OK, result);
                                    finish();
                                    return;
                                }
                                if (StorageUtils.isVideo(file.getPath()) | StorageUtils.isMusic(file.getPath())) {
                                    Intent intent = new Intent(ExploreActivity.this, VideoActivity.class);
                                    intent.putExtra("url", file.getUrl());
                                    startActivity(intent);
                                } else if (StorageUtils.isImage(file.getPath())) {
                                    executorService.execute(() -> {
                                        try {
                                            byte[] data = fileProvider.readAllBytes(file.getPath());
                                            //StorageUtils.bitmapShare = BitmapFactory.decodeByteArray(data, 0, data.length);
                                            StorageUtils.IMAGE_BYTE_SHARE = data;
                                            runOnUiThread(() -> {
                                                Intent intent = new Intent(ExploreActivity.this, ImageActivity.class);
                                                startActivity(intent);
                                            });
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                                }
                                Log.e(this.getClass().getSimpleName(), file.getPath());
                            }
                        }, (fileItems) -> {
                            if (!fileItems.isEmpty()) {
                                btnDelete.setVisibility(View.VISIBLE);
                                btnCut.setVisibility(View.VISIBLE);
                                btnCopy.setVisibility(View.VISIBLE);
                                btnSort.setVisibility(View.GONE);
                            } else {
                                btnDelete.setVisibility(View.GONE);
                                btnCut.setVisibility(View.GONE);
                                btnCopy.setVisibility(View.GONE);
                                btnSort.setVisibility(View.VISIBLE);
                            }
                            if (fileItems.size() == 1) {
                                btnRename.setVisibility(View.VISIBLE);
                            } else {
                                btnRename.setVisibility(View.GONE);
                            }
                            selectedFileList = fileItems;
                        });

                        recyclerView.setAdapter(adapter);

                        Parcelable state = scrollStates.get(path);
                        if (state != null) {
                            recyclerView.getLayoutManager().onRestoreInstanceState(state);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(this, path + " " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, path + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshCurrentDirectory() {
        scrollStates.remove(currentPath);
        showFiles(currentPath);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        try {
            if (selectedFileList != null && !selectedFileList.isEmpty()) {
                ((FileAdapter) recyclerView.getAdapter()).clearSelection();
                return;
            }
            if (currentPath != null && !currentPath.equals(initPath)) {
                SimpleURI uri = new SimpleURI(currentPath);
                String parent = new File(uri.getPath()).getParent();
                if (parent != null) {
                    String raw = currentPath.replace(uri.getPath(), "");
                    Log.e("raw", raw);
                    Log.e("parent", parent);
                    showFiles(raw + parent);
                    return;
                }
            }
        } catch (Exception ignored) {
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fileProvider != null) {
            fileProvider.close();
        }
        StorageUtils.IMAGE_BYTE_SHARE = null;
    }
}
