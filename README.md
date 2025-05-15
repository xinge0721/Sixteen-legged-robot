# Sixteen-legged-robot

# Android与服务器通信指南

## 服务器端代码（Ubuntu 22）

我们提供了两个版本的服务器代码，您可以根据个人偏好选择使用：

### Python版本

优点：简单易用，无需编译，适合快速部署测试

1. 确保已安装Python 3：
```bash
python3 --version
```

2. 如果未安装，请执行：
```bash
sudo apt update
sudo apt install python3 python3-pip
```

3. 运行服务器：
```bash
python3 server_python.py
```

### C++版本

优点：性能更高，适合长期运行

1. 确保已安装编译器和开发库：
```bash
sudo apt update
sudo apt install build-essential
```

2. 编译服务器代码：
```bash
g++ -o server server_ubuntu.cpp
```

3. 运行服务器：
```bash
./server
```

## 使用说明

1. 在服务器上启动其中一个服务器程序
2. 服务器将在8080端口监听来自Android客户端的连接
3. 当Android客户端连接并发送消息时，服务器会自动打印接收到的消息
4. 可以使用Ctrl+C停止服务器程序

## 重要提示

- 如果您的Ubuntu系统启用了防火墙，请确保开放8080端口：
```bash
sudo ufw allow 8080/tcp
sudo ufw reload
```

- 如果您需要从外部网络访问服务器，请确保您的路由器或云服务提供商允许8080端口的入站流量

## 修改端口

如果您需要更改监听端口：

- 在C++版本中，修改源代码顶部的`PORT`常量值
- 在Python版本中，修改`start_server()`函数中的`server_port`变量值
- 在Android客户端代码中，修改`SERVER_PORT`常量值

完成修改后重新编译/运行即可
