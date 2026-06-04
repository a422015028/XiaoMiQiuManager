package com.xiaomiqiu.manager.model;

public class Config {
    private String id;
    private String name;
    private String serverAddr;
    private String authToken;
    private String binaryPath;
    private boolean isDefault;
    private long createdAt;

    public Config() {
        this.id = String.valueOf(System.currentTimeMillis());
        this.createdAt = System.currentTimeMillis();
        this.binaryPath = "/data/local/tmp/xmq/xiaomiqiu";
        this.isDefault = false;
    }

    public Config(String name, String serverAddr, String authToken) {
        this();
        this.name = name;
        this.serverAddr = serverAddr;
        this.authToken = authToken;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getBinaryPath() {
        return binaryPath;
    }

    public void setBinaryPath(String binaryPath) {
        this.binaryPath = binaryPath;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getConfigPath() {
        if (binaryPath == null || binaryPath.isEmpty()) {
            return "/data/local/tmp/xmq/xiaomiqiu.conf";
        }
        int lastSlash = binaryPath.lastIndexOf('/');
        if (lastSlash >= 0) {
            return binaryPath.substring(0, lastSlash + 1) + "xiaomiqiu.conf";
        }
        return "/data/local/tmp/xmq/xiaomiqiu.conf";
    }

    public String getDirectory() {
        if (binaryPath == null || binaryPath.isEmpty()) {
            return "/data/local/tmp/xmq";
        }
        int lastSlash = binaryPath.lastIndexOf('/');
        if (lastSlash >= 0) {
            return binaryPath.substring(0, lastSlash);
        }
        return "/data/local/tmp/xmq";
    }
    // ================= 新增：供 Spinner 等 UI 控件显示文本使用 =================
    @Override
    public String toString() {
        // 如果名字为空，给个保底提示，否则直接返回配置名称
        return (name != null && !name.isEmpty()) ? name : "未命名配置";
    }
}
