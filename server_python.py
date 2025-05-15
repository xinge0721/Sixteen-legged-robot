#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import socket
import threading
import re

def parse_http_request(request_data):
    """Parse HTTP request and extract the body"""
    try:
        # 将二进制数据解码为字符串，替换无法解码的部分
        request_str = request_data.decode('utf-8', errors='replace')
        
        # 打印完整的请求以便调试
        print(f"Received HTTP request: \n{request_str}")
        
        # 检查是否有请求体(在空行之后)
        parts = request_str.split('\r\n\r\n', 1)
        if len(parts) > 1:
            body = parts[1].strip()
            return body
        else:
            # 如果没有请求体，尝试直接从请求数据中提取内容
            # 这不符合HTTP协议，但有时候客户端可能直接发送数据
            return request_str.strip()
    except Exception as e:
        print(f"Error parsing HTTP request: {e}")
        
        # 如果解析错误，尝试直接查找"hello"字符串
        try:
            if b'hello' in request_data:
                return 'hello'
        except:
            pass
            
        # 失败时返回原始字节的十六进制表示，用于调试
        return f"[Raw data: {request_data.hex()}]"

def handle_client(client_socket, client_address):
    """Handle each client connection"""
    print(f"Accepted connection from {client_address}")
    
    try:
        # 接收数据
        request_data = client_socket.recv(4096)
        
        if not request_data:
            print(f"No data received from {client_address}")
            return
            
        # 解析HTTP请求
        message = parse_http_request(request_data)
        print(f"Extracted message from {client_address}: {message}")
        
        # 构造HTTP响应
        response = f"""HTTP/1.1 200 OK
Content-Type: text/plain
Connection: close

Server received: {message}
"""
        # 发送响应
        client_socket.send(response.encode('utf-8'))
        print(f"Response sent to {client_address}")
        
    except Exception as e:
        print(f"Error handling client {client_address}: {e}")
    finally:
        # 关闭连接
        client_socket.close()

def start_server():
    server_host = '0.0.0.0'  # 监听所有接口
    server_port = 8080       # 与Android客户端相同的端口
    
    # 创建TCP套接字
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    
    # 设置端口复用
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    
    # 绑定IP和端口
    server_socket.bind((server_host, server_port))
    
    # 开始监听
    server_socket.listen(5)
    print(f"HTTP Server started on {server_host}:{server_port}, waiting for connections...")
    
    try:
        while True:
            # 接受新的客户端连接
            client_socket, client_address = server_socket.accept()
            
            # 为每个客户端创建新线程
            client_thread = threading.Thread(
                target=handle_client,
                args=(client_socket, client_address)
            )
            client_thread.daemon = True
            client_thread.start()
    except KeyboardInterrupt:
        print("Server shutting down...")
    finally:
        server_socket.close()

if __name__ == "__main__":
    start_server() 