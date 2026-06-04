package com.xiaomiqiu.manager.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class RootUtils {

    private static final String TAG = "RootUtils";

    public static boolean isRootAvailable() {
        CommandResult result = executeRootCommand("echo root_test");
        return result.isSuccess();
    }

    public static CommandResult executeRootCommand(String command) {
        return executeRootCommands(new String[]{command});
    }

    public static CommandResult executeRootCommands(String[] commands) {
        Process process = null;
        DataOutputStream os = null;
        BufferedReader reader = null;
        StringBuilder output = new StringBuilder();

        try {
            // 使用 ProcessBuilder 合并错误流与标准输出流，防止 stderr 塞满缓冲区导致进程死锁
            ProcessBuilder pb = new ProcessBuilder("su");
            pb.redirectErrorStream(true);
            process = pb.start();

            os = new DataOutputStream(process.getOutputStream());

            for (String command : commands) {
                if (command == null || command.trim().isEmpty()) continue;
                Log.d(TAG, "Executing: " + command);
                // 必须使用 getBytes(StandardCharsets.UTF_8)，writeBytes 会丢弃高8位导致字符乱码
                os.write((command + "\n").getBytes(StandardCharsets.UTF_8));
            }
            os.write("exit\n".getBytes(StandardCharsets.UTF_8));
            os.flush();

            // 指定 UTF-8 读取输出
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            String result = output.toString().trim();

            Log.d(TAG, "Commands exit code: " + exitCode + ", output: " + result);

            return new CommandResult(exitCode, result, "");

        } catch (Exception e) {
            Log.e(TAG, "Commands execution failed", e);
            return new CommandResult(-1, "", e.getMessage());
        } finally {
            try {
                if (os != null) os.close();
                if (reader != null) reader.close();
            } catch (Exception ignored) {}
            if (process != null) {
                process.destroy();
            }
        }
    }

    public static boolean isProcessRunning(String processName) {
        // 方法1: 使用 pgrep -x 精确匹配进程名（推荐，只匹配完全等于该名字的进程）
        CommandResult result = executeRootCommand("pgrep -x \"" + processName + "\"");
        if (result.exitCode == 0 && !result.output.isEmpty()) {
            Log.d(TAG, "Process found via pgrep -x, PID: " + result.output);
            return true;
        }

        // 方法2: 使用 pidof（备用）
        result = executeRootCommand("pidof \"" + processName + "\"");
        if (result.exitCode == 0 && !result.output.isEmpty()) {
            Log.d(TAG, "Process found via pidof, PID: " + result.output);
            return true;
        }

        // 删除了 ps -A | grep 的备用方案，防止由于包名包含二进制名导致的自身匹配误判
        Log.d(TAG, "Process not found");
        return false;
    }

    public static CommandResult startProcess(String binaryPath) {
        String dir;
        String binaryName;

        try {
            int lastSlash = binaryPath.lastIndexOf('/');
            if (lastSlash < 0) {
                dir = "/data/local/tmp/xmq";
                binaryName = binaryPath;
            } else {
                dir = binaryPath.substring(0, lastSlash);
                binaryName = binaryPath.substring(lastSlash + 1);
            }
        } catch (Exception e) {
            dir = "/data/local/tmp/xmq";
            binaryName = "xiaomiqiu";
        }

        String configPath = dir + "/xiaomiqiu.conf";

        String[] commands = {
                "cd \"" + dir + "\"",
                "chmod 777 \"" + binaryPath + "\"",
                "chmod 777 \"" + configPath + "\"",
                // 使用 nohup 替代 setsid，在 Android Root 环境下兼容性更好，彻底分离 stdout/stderr
                "nohup ./" + binaryName + " > /dev/null 2>&1 &",
                "sleep 1" // 留出 1 秒让进程初始化
        };

        return executeRootCommands(commands);
    }

    public static CommandResult stopProcess(String processName) {
        CommandResult result = executeRootCommand(
            "pids=$(pgrep -x \"" + processName + "\" 2>/dev/null); " +
            "if [ -n \"$pids\" ]; then " +
            "for pid in $pids; do kill -9 $pid 2>/dev/null; done; " +
            "echo \"Killed\"; " +
            "else echo 'No process found'; fi"
        );
        return result;
    }

    public static CommandResult writeConfigFile(String configPath, String serverAddr, String authToken) {
        // 使用 echo 分行写入，避免 printf 复杂的转义问题导致特殊字符的 Token 写入失败
        String[] commands = {
                "mkdir -p \"$(dirname '" + configPath + "')\"",
                "echo \"server_addr: " + serverAddr + "\" > \"" + configPath + "\"",
                "echo \"auth_token: " + authToken + "\" >> \"" + configPath + "\"",
                "chmod 777 \"" + configPath + "\""
        };

        return executeRootCommands(commands);
    }

    public static class CommandResult {
        public final int exitCode;
        public final String output;
        public final String error;

        public CommandResult(int exitCode, String output, String error) {
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
}