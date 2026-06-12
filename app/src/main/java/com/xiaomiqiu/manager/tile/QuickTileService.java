package com.xiaomiqiu.manager.tile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

import com.xiaomiqiu.manager.model.Config;
import com.xiaomiqiu.manager.service.MonitorService;
import com.xiaomiqiu.manager.utils.ConfigManager;
import com.xiaomiqiu.manager.utils.NotificationHelper;
import com.xiaomiqiu.manager.utils.RootUtils;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QuickTileService extends TileService {

    private ConfigManager configManager;
    private BroadcastReceiver statusReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        configManager = new ConfigManager(this);
        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateTile();
            }
        };
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
        registerReceiver(statusReceiver, new IntentFilter("com.xiaomiqiu.manager.STATUS_CHANGED"), Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        try {
            unregisterReceiver(statusReceiver);
        } catch (Exception ignored) {}
    }

    @Override
    public void onClick() {
        super.onClick();

        // 在后台线程执行所有操作
        new Thread(() -> {
            if (!RootUtils.isRootAvailable()) {
                NotificationHelper.showNotification(this, "错误", "需要Root权限");
                return;
            }

            Config config = configManager.getDefaultConfig();
            if (config == null) {
                NotificationHelper.showNotification(this, "错误", "请先配置默认设置");
                return;
            }

            startMonitorService();

            boolean isRunning = RootUtils.isProcessRunning("xiaomiqiu");

            if (isRunning) {
                // Stop process
                RootUtils.CommandResult result = RootUtils.stopProcess("xiaomiqiu");
                if (!result.isSuccess()) {
                    NotificationHelper.showNotification(this, "错误", "停止失败: " + result.error);
                }
            } else {
                // Write config and start process
                RootUtils.CommandResult configResult = RootUtils.writeConfigFile(
                        config.getConfigPath(),
                        config.getServerAddr(),
                        config.getAuthToken()
                );

                if (!configResult.isSuccess()) {
                    NotificationHelper.showNotification(this, "错误", "写入配置失败");
                    return;
                }

                RootUtils.CommandResult startResult = RootUtils.startProcess(config.getBinaryPath());
                if (!startResult.isSuccess()) {
                    NotificationHelper.showNotification(this, "错误", "启动失败: " + startResult.error);
                }
            }

            // 更新磁贴状态
            updateTileInternal();
        }).start();
    }

    private void updateTile() {
        new Thread(this::updateTileInternal).start();
    }

    private void updateTileInternal() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean isRunning = RootUtils.isProcessRunning("xiaomiqiu");

        if (isRunning) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setLabel("停止 小米球");
            tile.setContentDescription("小米球 运行中，点击停止");
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel("启动 小米球");
            tile.setContentDescription("小米球 已停止，点击启动");
        }

        tile.updateTile();
    }

    private void startMonitorService() {
        Intent serviceIntent = new Intent(this, MonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}
