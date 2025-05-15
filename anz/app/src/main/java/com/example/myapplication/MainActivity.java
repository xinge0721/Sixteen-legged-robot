package com.example.myapplication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SocketTest";
    private TextView logTextView;
    private static final String SERVER_IP = "114.132.88.212";
    private static final int SERVER_PORT = 8080;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logTextView = findViewById(R.id.log_text);
        Button sendButton = findViewById(R.id.send_button);
        
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logMessage("开始发送数据...");
                checkNetworkConnection();
                sendHelloHttp();
            }
        });

        // 显示网络接口信息
        showNetworkInterfaces();
    }

    // 检查网络连接状态
    private void checkNetworkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            logMessage("网络连接状态: 已连接");
            logMessage("网络类型: " + activeNetworkInfo.getTypeName());
            logMessage("网络详情: " + activeNetworkInfo.getSubtypeName());
            logMessage("网络状态: " + activeNetworkInfo.getState());
        } else {
            logMessage("网络连接状态: 未连接!");
        }
    }

    // 使用简单的HTTP发送数据
    private void sendHelloHttp() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    logMessage("准备连接到服务器: " + SERVER_IP + ":" + SERVER_PORT);
                    
                    // 测试是否可以ping通
                    try {
                        InetAddress address = InetAddress.getByName(SERVER_IP);
                        logMessage("服务器IP: " + address.getHostAddress());
                    } catch (Exception e) {
                        logMessage("解析服务器IP失败: " + e.getMessage());
                    }
                    
                    // 创建连接
                    String urlString = "http://" + SERVER_IP + ":" + SERVER_PORT;
                    logMessage("尝试连接URL: " + urlString);
                    
                    URL url = new URL(urlString);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    
                    // 连接
                    logMessage("正在连接...");
                    connection.connect();
                    logMessage("连接成功，发送数据...");
                    
                    // 发送hello
                    OutputStream os = connection.getOutputStream();
                    os.write("hello".getBytes());
                    os.flush();
                    os.close();
                    logMessage("数据已发送");
                    
                    // 获取响应
                    int responseCode = connection.getResponseCode();
                    logMessage("服务器响应: " + responseCode);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "发送成功，响应: " + responseCode, Toast.LENGTH_SHORT).show();
                        }
                    });
                    
                } catch (Exception e) {
                    final String error = e.toString();
                    logMessage("发生错误: " + error);
                    
                    if (error.contains("EPERM")) {
                        logMessage("权限错误，可能原因:");
                        logMessage("1. 网络权限问题");
                        logMessage("2. 防火墙阻止");
                        logMessage("3. 安全策略限制");
                    }
                    
                    // 记录堆栈
                    for (StackTraceElement element : e.getStackTrace()) {
                        logMessage("- " + element.toString());
                    }
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }
    
    // 显示网络接口信息
    private void showNetworkInterfaces() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    logMessage("=== 网络接口信息 ===");
                    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                    while (interfaces.hasMoreElements()) {
                        NetworkInterface networkInterface = interfaces.nextElement();
                        logMessage("接口: " + networkInterface.getName() + " (" + networkInterface.getDisplayName() + ")");
                        logMessage("  状态: " + (networkInterface.isUp() ? "启用" : "禁用"));
                        logMessage("  回环: " + (networkInterface.isLoopback() ? "是" : "否"));
                        
                        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            InetAddress address = addresses.nextElement();
                            logMessage("  地址: " + address.getHostAddress());
                        }
                    }
                    logMessage("====================");
                } catch (Exception e) {
                    logMessage("获取网络接口信息失败: " + e.getMessage());
                }
            }
        }).start();
    }
    
    // 添加日志消息
    private void logMessage(final String message) {
        Log.d(TAG, message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logTextView.append(message + "\n");
                
                // 自动滚动到底部
                final int scrollAmount = logTextView.getLayout().getLineTop(logTextView.getLineCount()) - logTextView.getHeight();
                if (scrollAmount > 0) {
                    logTextView.scrollTo(0, scrollAmount);
                } else {
                    logTextView.scrollTo(0, 0);
                }
            }
        });
    }
}