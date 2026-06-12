package com.xiaomiqiu.manager.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.xiaomiqiu.manager.R;
import com.xiaomiqiu.manager.model.Config;
import com.xiaomiqiu.manager.service.MonitorService;
import com.xiaomiqiu.manager.utils.ConfigManager;
import com.xiaomiqiu.manager.utils.NotificationHelper;
import com.xiaomiqiu.manager.utils.RootUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "MainActivity";

    private TextView tvStatus;
    private TextView tvServerAddr;
    private TextView tvAuthToken;
    private TextView tvBinaryPath;
    private TextView tvConfigCount;
    private Spinner spinnerConfigs;
    private MaterialButton btnToggle;
    private RecyclerView recyclerView;
    private ConfigAdapter configAdapter;
    private FloatingActionButton fabAdd;
    private ImageButton btnRefresh;
    private View statusIndicator;
    private ImageView ivStatusIcon;
    private LinearLayout emptyState;

    private ConfigManager configManager;
    private Handler handler;
    private ExecutorService executorService;
    private boolean spinnerInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configManager = new ConfigManager(this);
        handler = new Handler(Looper.getMainLooper());
        executorService = Executors.newSingleThreadExecutor();

        initViews();
        checkPermissions();
        startMonitorService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tv_status);
        tvServerAddr = findViewById(R.id.tv_server_addr);
        tvAuthToken = findViewById(R.id.tv_auth_token);
        tvBinaryPath = findViewById(R.id.tv_binary_path);
        tvConfigCount = findViewById(R.id.tv_config_count);
        spinnerConfigs = findViewById(R.id.spinner_configs);
        btnToggle = findViewById(R.id.btn_toggle);
        recyclerView = findViewById(R.id.recycler_configs);
        fabAdd = findViewById(R.id.fab_add);
        btnRefresh = findViewById(R.id.btn_refresh);
        statusIndicator = findViewById(R.id.status_indicator);
        ivStatusIcon = findViewById(R.id.iv_status_icon);
        emptyState = findViewById(R.id.empty_state);

        // 初始状态为检查中
        tvStatus.setText("检查中...");
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_unknown));
        setStatusIndicatorColor(R.color.status_unknown);
        btnToggle.setEnabled(false);

        btnToggle.setOnClickListener(v -> toggleService());
        fabAdd.setOnClickListener(v -> addNewConfig());
        btnRefresh.setOnClickListener(v -> checkStatus());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        configAdapter = new ConfigAdapter();
        recyclerView.setAdapter(configAdapter);

        spinnerConfigs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Config config = (Config) parent.getItemAtPosition(position);
                if (config != null) {
                    displayConfig(config);
                    if (spinnerInitialized) {
                        Config currentDefault = configManager.getDefaultConfig();
                        if (currentDefault == null || !currentDefault.getId().equals(config.getId())) {
                            setDefaultConfig(config);
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setStatusIndicatorColor(int colorRes) {
        if (statusIndicator != null && statusIndicator.getBackground() instanceof GradientDrawable) {
            GradientDrawable drawable = (GradientDrawable) statusIndicator.getBackground();
            drawable.setColor(ContextCompat.getColor(this, colorRes));
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void startMonitorService() {
        Intent serviceIntent = new Intent(this, MonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConfigs();
        // 延迟执行状态检查
        handler.postDelayed(this::checkStatus, 300);
    }

    private void checkStatus() {
        if (executorService == null || executorService.isShutdown()) return;
        
        executorService.execute(() -> {
            boolean isRunning = RootUtils.isProcessRunning("xiaomiqiu");
            handler.post(() -> updateUIStatus(isRunning));
        });
    }

    private void updateUIStatus(boolean isRunning) {
        if (isRunning) {
            tvStatus.setText("运行中");
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_running));
            setStatusIndicatorColor(R.color.status_running);
            btnToggle.setText("停止服务");
            btnToggle.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_stop));
            btnToggle.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.color_stop));
            ivStatusIcon.setImageResource(R.drawable.ic_server);
            ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_running));
        } else {
            tvStatus.setText("已停止");
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_stopped));
            setStatusIndicatorColor(R.color.status_stopped);
            btnToggle.setText("启动服务");
            btnToggle.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_play));
            btnToggle.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.color_start));
            ivStatusIcon.setImageResource(R.drawable.ic_server);
            ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_stopped));
        }
        btnToggle.setEnabled(true);
    }

    private void loadConfigs() {
        List<Config> configs = configManager.getAllConfigs();
        configAdapter.setConfigs(configs);

        // 更新配置计数
        tvConfigCount.setText(configs.size() + " 个配置");

        // 显示/隐藏空状态
        if (configs.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        ArrayAdapter<Config> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, configs);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerConfigs.setAdapter(spinnerAdapter);

        spinnerInitialized = false;
        Config defaultConfig = configManager.getDefaultConfig();
        if (defaultConfig != null && !configs.isEmpty()) {
            for (int i = 0; i < configs.size(); i++) {
                if (configs.get(i).getId().equals(defaultConfig.getId())) {
                    spinnerConfigs.setSelection(i);
                    displayConfig(defaultConfig);
                    break;
                }
            }
        } else if (configs.isEmpty()) {
            tvServerAddr.setText("服务器: (无配置)");
            tvAuthToken.setText("Token: (无配置)");
            // 建议默认路径依然使用 /data/local/tmp/xmq 避免 SELinux 拦截
            tvBinaryPath.setText("路径: /data/local/tmp/xmq/xiaomiqiu");
        }
        spinnerInitialized = true;
    }

    private void displayConfig(Config config) {
        tvServerAddr.setText("服务器: " + (config.getServerAddr() != null ? config.getServerAddr() : ""));
        tvAuthToken.setText("Token: " + (config.getAuthToken() != null ? maskToken(config.getAuthToken()) : ""));
        tvBinaryPath.setText("路径: " + (config.getBinaryPath() != null ? config.getBinaryPath() : ""));
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 4) {
            return "****";
        }
        return token.substring(0, 2) + "****" + token.substring(token.length() - 2);
    }

    // ================= 二进制文件释放与 MD5 校验核心逻辑 =================
    private boolean prepareBinary(String targetPath) {
        try {
            File privateFile = new File(getFilesDir(), "xiaomiqiu");
            boolean needExtract = true;

            // 1. 如果私有目录已有文件，进行 MD5 校验
            if (privateFile.exists() && privateFile.length() > 0) {
                String assetMd5 = getAssetMd5("xiaomiqiu");
                String localMd5 = getFileMd5(privateFile);
                
                if (assetMd5 != null && assetMd5.equals(localMd5)) {
                    needExtract = false;
                    Log.d(TAG, "Binary MD5 match, skip extraction.");
                } else {
                    Log.d(TAG, "Binary MD5 mismatch, updating binary...");
                }
            }

            // 2. 需要释放时，从 assets 覆盖写入私有目录
            if (needExtract) {
                try (InputStream is = getAssets().open("xiaomiqiu");
                     FileOutputStream fos = new FileOutputStream(privateFile)) {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                }
            }

            // 3. 将文件从私有目录拷贝到 targetPath (利用 Root 权限，解决权限组和 SELinux 问题)
            String[] copyCommands = {
                    "mkdir -p \"$(dirname '" + targetPath + "')\"",
                    "cp -f \"" + privateFile.getAbsolutePath() + "\" \"" + targetPath + "\"",
                    "chmod 777 \"" + targetPath + "\""
            };
            
            RootUtils.CommandResult result = RootUtils.executeRootCommands(copyCommands);
            return result.isSuccess();

        } catch (Exception e) {
            Log.e(TAG, "Failed to prepare binary", e);
            return false;
        }
    }

    // --- MD5 计算辅助方法 ---
    private String getAssetMd5(String assetName) {
        try (InputStream is = getAssets().open(assetName)) {
            return getStreamMd5(is);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get asset MD5", e);
            return null;
        }
    }

    private String getFileMd5(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            return getStreamMd5(fis);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get file MD5", e);
            return null;
        }
    }

    private String getStreamMd5(InputStream is) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }
        byte[] md5Bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : md5Bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    // =================================================================

    private void toggleService() {
        // 检查是否已选择配置
        Config config = (Config) spinnerConfigs.getSelectedItem();
        if (config == null) {
            Toast.makeText(this, "请先选择一个配置", Toast.LENGTH_SHORT).show();
            return;
        }

        // 禁用按钮，显示检查中
        btnToggle.setEnabled(false);
        tvStatus.setText("检查中...");
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_unknown));
        setStatusIndicatorColor(R.color.status_unknown);

        executorService.execute(() -> {
            // 检查Root权限
            boolean hasRoot = RootUtils.isRootAvailable();
            if (!hasRoot) {
                handler.post(() -> {
                    Toast.makeText(this, "需要Root权限", Toast.LENGTH_LONG).show();
                    NotificationHelper.showNotification(this, "错误", "需要Root权限");
                    checkStatus();
                });
                return;
            }

            // 检测当前状态
            boolean isRunning = RootUtils.isProcessRunning("xiaomiqiu");

            if (isRunning) {
                // 停止服务
                handler.post(() -> Toast.makeText(this, "正在停止服务...", Toast.LENGTH_SHORT).show());
                NotificationHelper.showNotification(this, "小米球", "正在停止服务...");

                RootUtils.CommandResult result = RootUtils.stopProcess("xiaomiqiu");

                handler.post(() -> {
                    if (result.isSuccess()) {
                        Toast.makeText(this, "服务已停止", Toast.LENGTH_SHORT).show();
                        NotificationHelper.showNotification(this, "小米球", "服务已停止");
                    } else {
                        Toast.makeText(this, "停止失败", Toast.LENGTH_LONG).show();
                        NotificationHelper.showNotification(this, "错误", "停止失败");
                    }
                    // 延迟检查状态
                    handler.postDelayed(this::checkStatus, 1000);
                });
            } else {
                // 启动服务前：释放二进制文件
                handler.post(() -> Toast.makeText(this, "正在准备核心文件...", Toast.LENGTH_SHORT).show());
                NotificationHelper.showNotification(this, "小米球", "正在启动服务...");

                boolean isPrepared = prepareBinary(config.getBinaryPath());
                if (!isPrepared) {
                    handler.post(() -> {
                        Toast.makeText(this, "核心文件释放失败，请检查 Assets", Toast.LENGTH_LONG).show();
                        NotificationHelper.showNotification(this, "错误", "二进制文件准备失败");
                        checkStatus();
                    });
                    return;
                }

                // 写入配置
                RootUtils.CommandResult configResult = RootUtils.writeConfigFile(
                        config.getConfigPath(),
                        config.getServerAddr(),
                        config.getAuthToken()
                );

                if (!configResult.isSuccess()) {
                    handler.post(() -> {
                        Toast.makeText(this, "写入配置失败", Toast.LENGTH_LONG).show();
                        NotificationHelper.showNotification(this, "错误", "写入配置失败");
                        checkStatus();
                    });
                    return;
                }

                // 启动进程
                RootUtils.CommandResult startResult = RootUtils.startProcess(config.getBinaryPath());

                handler.post(() -> {
                    if (startResult.isSuccess()) {
                        Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show();
                        NotificationHelper.showNotification(this, "小米球", "服务已启动");
                    } else {
                        Toast.makeText(this, "启动失败", Toast.LENGTH_LONG).show();
                        NotificationHelper.showNotification(this, "错误", "启动失败");
                    }
                    // 延迟检查状态
                    handler.postDelayed(this::checkStatus, 1500);
                });
            }
        });
    }

    private void addNewConfig() {
        Intent intent = new Intent(this, ConfigEditActivity.class);
        startActivity(intent);
    }

    private void editConfig(Config config) {
        Intent intent = new Intent(this, ConfigEditActivity.class);
        intent.putExtra("config_id", config.getId());
        startActivity(intent);
    }

    private void deleteConfig(Config config) {
        new AlertDialog.Builder(this)
                .setTitle("删除配置")
                .setMessage("确定要删除配置 \"" + config.getName() + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    configManager.deleteConfig(config.getId());
                    loadConfigs();
                    Toast.makeText(this, "配置已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void setDefaultConfig(Config config) {
        configManager.setDefaultConfig(config.getId());
        loadConfigs();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            checkStatus();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class ConfigAdapter extends RecyclerView.Adapter<ConfigAdapter.ViewHolder> {

        private List<Config> configs = new ArrayList<>();

        public void setConfigs(List<Config> configs) {
            this.configs = configs;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_config, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Config config = configs.get(position);
            holder.tvName.setText(config.getName());
            holder.tvServer.setText(config.getServerAddr());

            // 显示/隐藏默认标签
            if (config.isDefault()) {
                holder.chipDefault.setVisibility(View.VISIBLE);
            } else {
                holder.chipDefault.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> editConfig(config));
            holder.itemView.setOnLongClickListener(v -> {
                showConfigOptions(config);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return configs.size();
        }

        private void showConfigOptions(Config config) {
            String[] options = {"编辑", "设为默认", "删除"};
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(config.getName())
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                editConfig(config);
                                break;
                            case 1:
                                setDefaultConfig(config);
                                break;
                            case 2:
                                deleteConfig(config);
                                break;
                        }
                    })
                    .show();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvServer;
            Chip chipDefault;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_config_name);
                tvServer = itemView.findViewById(R.id.tv_config_server);
                chipDefault = itemView.findViewById(R.id.chip_default);
            }
        }
    }
}
