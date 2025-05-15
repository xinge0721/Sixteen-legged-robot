package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String SERVER_IP = "114.132.88.212";
    private static final int SERVER_PORT = 8080;
    private HttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button sendButton = findViewById(R.id.send_button);
        Button sendHexButton = findViewById(R.id.send_hex_button);
        
        // 初始化HttpClient (全局单例)
        httpClient = new HttpClient(SERVER_IP, SERVER_PORT);
        
        // 启用调试模式，查看详细日志
        httpClient.setDebugMode(true);
        
        // 设置普通发送按钮点击事件
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 示例：发送测试数据
                httpClient.send("hello", new HttpClient.Callback() {
                    @Override
                    public void onSuccess(int responseCode) {
                        Toast.makeText(MainActivity.this, "发送成功: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(MainActivity.this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        
        // 设置十六进制数组发送按钮点击事件
        sendHexButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 创建一个十六进制字节数组
                // 示例: 0xAA, 0x55, 0x01, 0x02, 0xFF, 0xFE
                // 这是一个典型的单片机通信协议格式:
                // 0xAA, 0x55: 帧头
                // 0x01: 命令类型
                // 0x02: 数据长度
                // 0xFF, 0xFE: 数据内容
                byte[] hexData = new byte[] {
                    (byte)0xAA, (byte)0x55,  // 帧头
                    (byte)0x01,              // 命令类型
                    (byte)0x02,              // 数据长度
                    (byte)0xFF, (byte)0xFE   // 数据内容
                };
                
                // 发送十六进制数据
                httpClient.send(hexData, new HttpClient.Callback() {
                    @Override
                    public void onSuccess(int responseCode) {
                        Toast.makeText(MainActivity.this, 
                            "十六进制数据发送成功: " + responseCode, 
                            Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(MainActivity.this, 
                            "十六进制数据发送失败: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpClient != null) {
            httpClient.close();
        }
    }
    
    // 获取HttpClient实例，供其他组件使用
    public HttpClient getHttpClient() {
        return httpClient;
    }
    
    /**
     * 工具方法：将十六进制字符串转换为字节数组
     * 例如: "AA55010203" -> {0xAA, 0x55, 0x01, 0x02, 0x03}
     * 
     * @param hexString 十六进制字符串(不含空格或其他分隔符)
     * @return 字节数组
     */
    public static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i+1), 16));
        }
        return data;
    }
    
    /**
     * 工具方法：将字节数组转换为十六进制字符串
     * 例如: {0xAA, 0x55, 0x01, 0x02, 0x03} -> "AA55010203"
     * 
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString().toUpperCase();
    }
}