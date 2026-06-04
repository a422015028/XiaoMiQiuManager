package com.xiaomiqiu.manager.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.xiaomiqiu.manager.R;
import com.xiaomiqiu.manager.XiaoMiQiuApp;
import com.xiaomiqiu.manager.model.Config;
import com.xiaomiqiu.manager.ui.MainActivity;
import com.xiaomiqiu.manager.utils.ConfigManager;
import com.xiaomiqiu.manager.utils.RootUtils;

public class MonitorService extends Service {

    private static final int NOTIFICATION_ID = 1001;
    private Handler handler;
    private Runnable checkRunnable;
    private ConfigManager configManager;
    private boolean lastStatus = false;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        configManager = new ConfigManager(this);
        startMonitoring();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification(false));
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startMonitoring() {
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                checkStatus();
                handler.postDelayed(this, 3000);
            }
        };
        handler.post(checkRunnable);
    }

    private void checkStatus() {
        boolean isRunning = RootUtils.isProcessRunning("xiaomiqiu");
        if (isRunning != lastStatus) {
            lastStatus = isRunning;
            updateNotification(isRunning);
            sendBroadcast(new Intent("com.xiaomiqiu.manager.STATUS_CHANGED"));
        }
    }

    private Notification createNotification(boolean isRunning) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = isRunning ? "小米球 运行中" : "小米球 已停止";
        Config config = configManager.getDefaultConfig();
        String message = (config != null && isRunning) ? "配置: " + config.getName() : "服务监控中";

        return new NotificationCompat.Builder(this, XiaoMiQiuApp.CHANNEL_ID)
                .setSmallIcon(isRunning ? R.drawable.ic_play : R.drawable.ic_stop)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(boolean isRunning) {
        startForeground(NOTIFICATION_ID, createNotification(isRunning));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
        }
    }
}
