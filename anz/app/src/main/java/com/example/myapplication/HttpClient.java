package com.example.myapplication;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HTTP客户端工具类
 * 
 * 用于向服务器发送各种类型的数据，已优化简化为单一API入口
 * 
 * 使用示例:
 * HttpClient client = new HttpClient("服务器IP", 端口);
 * 
 * // 发送字符串
 * client.send("hello");
 * 
 * // 发送字节数组
 * byte[] data = ...;
 * client.send(data);
 * 
 * // 发送整数数组
 * int[] numbers = {1, 2, 3};
 * client.send(numbers);
 */
public class HttpClient {
    private static final String TAG = "HttpClient";
    private String serverIp;
    private int serverPort;
    private int connectTimeout = 5000; // 默认连接超时5秒
    private int readTimeout = 5000;    // 默认读取超时5秒
    private ExecutorService executor;
    private Handler mainHandler;
    private boolean debugMode = false; // 是否显示调试日志

    /**
     * 回调接口，用于通知操作结果
     */
    public interface Callback {
        void onSuccess(int responseCode);
        void onError(Exception e);
    }

    /**
     * 构造函数
     * @param serverIp 服务器IP地址
     * @param serverPort 服务器端口
     */
    public HttpClient(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        // 默认关闭调试模式
        this.debugMode = false;
    }
    
    /**
     * 启用调试模式，将在LogCat中输出详细信息
     * @param debug true启用调试，false关闭调试
     */
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }

    /**
     * 设置连接超时时间
     * @param timeout 超时时间（毫秒）
     */
    public void setConnectTimeout(int timeout) {
        this.connectTimeout = timeout;
    }

    /**
     * 设置读取超时时间
     * @param timeout 超时时间（毫秒）
     */
    public void setReadTimeout(int timeout) {
        this.readTimeout = timeout;
    }
    
    /**
     * 简单发送方法 - 发送任意数据到服务器（无回调版本）
     * 
     * @param data 要发送的数据，支持字符串、字节数组、整数数组、对象数组等
     */
    public void send(Object data) {
        send(data, null);
    }

    /**
     * 简单发送方法 - 发送任意数据到服务器
     * 
     * @param data 要发送的数据，可以是:
     *             - 字符串: 直接发送字符串内容
     *             - byte[]: 直接发送字节数组
     *             - int[]: 转换为byte[]后发送
     *             - Object[]: 转换为字符串后发送
     * @param callback 回调接口，返回发送结果，可为null
     */
    public void send(Object data, Callback callback) {
        // 根据数据类型选择不同的处理方式
        if (data instanceof String) {
            // 发送字符串
            sendString((String)data, callback);
            
        } else if (data instanceof byte[]) {
            // 直接发送字节数组
            sendData((byte[])data, callback);
            
        } else if (data instanceof int[]) {
            // 将int[]转换为byte[]后发送
            int[] intArray = (int[])data;
            byte[] byteArray = new byte[intArray.length];
            
            for (int i = 0; i < intArray.length; i++) {
                byteArray[i] = (byte)(intArray[i] & 0xFF); // 取低8位
            }
            
            logMessage("整数数组已转换为字节数组，长度: " + byteArray.length);
            sendData(byteArray, callback);
            
        } else if (data instanceof Object[]) {
            // 将对象数组转为字符串后发送
            Object[] objArray = (Object[])data;
            StringBuilder sb = new StringBuilder();
            
            for (Object obj : objArray) {
                sb.append(obj.toString()).append(",");
            }
            
            // 移除最后一个逗号
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }
            
            logMessage("对象数组已转换为字符串: " + sb.toString());
            sendString(sb.toString(), callback);
            
        } else if (data instanceof List) {
            // 处理List类型
            List<?> list = (List<?>)data;
            StringBuilder sb = new StringBuilder();
            
            for (Object obj : list) {
                sb.append(obj.toString()).append(",");
            }
            
            // 移除最后一个逗号
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }
            
            logMessage("列表已转换为字符串: " + sb.toString());
            sendString(sb.toString(), callback);
            
        } else if (data != null) {
            // 不支持的类型，直接使用toString()然后发送
            logMessage("警告: 不支持的数据类型 " + data.getClass().getName() + "，尝试使用toString()方法");
            sendString(data.toString(), callback);
        } else {
            // 数据为null
            logMessage("错误: 发送的数据为null");
            if (callback != null) {
                mainHandler.post(() -> callback.onError(new IllegalArgumentException("发送的数据不能为null")));
            }
        }
    }

    /**
     * 发送字符串数据
     * @param data 要发送的字符串
     * @param callback 回调接口
     */
    private void sendString(String data, Callback callback) {
        logMessage("准备发送字符串数据: " + data);
        byte[] bytes = data.getBytes();
        sendData(bytes, callback);
    }

    /**
     * 发送字节数组数据
     * @param data 要发送的字节数组
     * @param callback 回调接口
     */
    private void sendData(byte[] data, Callback callback) {
        logMessage("准备发送字节数据，长度: " + data.length);
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                // 构建URL
                String urlString = "http://" + serverIp + ":" + serverPort;
                logMessage("连接到URL: " + urlString);

                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(connectTimeout);
                connection.setReadTimeout(readTimeout);

                // 连接
                logMessage("正在连接...");
                connection.connect();
                logMessage("连接成功，发送数据...");

                // 发送数据
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(data);
                    os.flush();
                }
                logMessage("数据已发送");

                // 获取响应码
                final int responseCode = connection.getResponseCode();
                logMessage("服务器响应: " + responseCode);

                // 通知成功
                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(responseCode));
                }

            } catch (Exception e) {
                // 记录错误
                final String error = e.toString();
                logMessage("发生错误: " + error);
                
                for (StackTraceElement element : e.getStackTrace()) {
                    logMessage("- " + element.toString());
                }

                // 通知错误
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e));
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
    
    /**
     * 记录日志消息
     * @param message 日志消息
     */
    private void logMessage(String message) {
        if (debugMode) {
            Log.d(TAG, message);
        }
    }
    
    /**
     * 关闭客户端，释放资源
     */
    public void close() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
} 