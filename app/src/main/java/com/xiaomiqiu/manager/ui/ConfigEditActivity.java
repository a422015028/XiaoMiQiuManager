package com.xiaomiqiu.manager.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.xiaomiqiu.manager.R;
import com.xiaomiqiu.manager.model.Config;
import com.xiaomiqiu.manager.utils.ConfigManager;

public class ConfigEditActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etServerAddr;
    private EditText etAuthToken;
    private EditText etBinaryPath;
    private CheckBox cbDefault;
    private Button btnSave;

    private ConfigManager configManager;
    private Config config;
    private boolean isEdit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_edit);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        configManager = new ConfigManager(this);

        initViews();

        String configId = getIntent().getStringExtra("config_id");
        if (configId != null) {
            config = configManager.getConfigById(configId);
            isEdit = true;
            loadConfig();
            setTitle("编辑配置");
        } else {
            config = new Config();
            setTitle("新建配置");
        }
    }

    private void initViews() {
        etName = findViewById(R.id.et_name);
        etServerAddr = findViewById(R.id.et_server_addr);
        etAuthToken = findViewById(R.id.et_auth_token);
        etBinaryPath = findViewById(R.id.et_binary_path);
        cbDefault = findViewById(R.id.cb_default);
        btnSave = findViewById(R.id.btn_save);

        btnSave.setOnClickListener(v -> saveConfig());
    }

    private void loadConfig() {
        if (config == null) return;

        etName.setText(config.getName());
        etServerAddr.setText(config.getServerAddr());
        etAuthToken.setText(config.getAuthToken());
        etBinaryPath.setText(config.getBinaryPath());
        cbDefault.setChecked(config.isDefault());
    }

    private void saveConfig() {
        String name = etName.getText().toString().trim();
        String serverAddr = etServerAddr.getText().toString().trim();
        String authToken = etAuthToken.getText().toString().trim();
        String binaryPath = etBinaryPath.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("请输入配置名称");
            return;
        }

        if (serverAddr.isEmpty()) {
            etServerAddr.setError("请输入服务器地址");
            return;
        }

        if (authToken.isEmpty()) {
            etAuthToken.setError("请输入认证Token");
            return;
        }

        if (binaryPath.isEmpty()) {
            binaryPath = "/data/local/tmp/xmq/xiaomiqiu";
        }

        config.setName(name);
        config.setServerAddr(serverAddr);
        config.setAuthToken(authToken);
        config.setBinaryPath(binaryPath);
        config.setDefault(cbDefault.isChecked());

        configManager.saveConfig(config);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
