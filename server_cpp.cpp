#include <iostream>
#include <string>
#include <cstring>
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <signal.h>

// 如果在Windows上运行，需要包含winsock2.h并进行初始化
#ifdef _WIN32
    #include <winsock2.h>
    #include <ws2tcpip.h>
    #pragma comment(lib, "ws2_32.lib")
    #define CLOSE_SOCKET closesocket
    typedef int socklen_t;
#else
    #define CLOSE_SOCKET close
#endif

const int PORT = 8080;  // 与Android客户端相同的端口
const int BUFFER_SIZE = 1024;
bool running = true;

// 信号处理函数，用于优雅地处理Ctrl+C
void signalHandler(int signal) {
    if (signal == SIGINT) {
        std::cout << "\n接收到中断信号，服务器正在关闭..." << std::endl;
        running = false;
    }
}

int main() {
    // 在Windows上初始化Winsock
    #ifdef _WIN32
        WSADATA wsaData;
        int wsaResult = WSAStartup(MAKEWORD(2, 2), &wsaData);
        if (wsaResult != 0) {
            std::cerr << "WSAStartup失败: " << wsaResult << std::endl;
            return 1;
        }
    #endif

    // 设置信号处理
    signal(SIGINT, signalHandler);

    // 创建服务器套接字
    int server_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (server_fd < 0) {
        std::cerr << "套接字创建失败" << std::endl;
        return 1;
    }

    // 设置端口重用选项
    int opt = 1;
    if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) < 0) {
        std::cerr << "setsockopt失败" << std::endl;
        CLOSE_SOCKET(server_fd);
        return 1;
    }

    // 配置服务器地址
    struct sockaddr_in address;
    memset(&address, 0, sizeof(address));
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;  // 监听所有网络接口
    address.sin_port = htons(PORT);

    // 绑定套接字
    if (bind(server_fd, (struct sockaddr*)&address, sizeof(address)) < 0) {
        std::cerr << "绑定失败" << std::endl;
        CLOSE_SOCKET(server_fd);
        return 1;
    }

    // 开始监听
    if (listen(server_fd, 5) < 0) {
        std::cerr << "监听失败" << std::endl;
        CLOSE_SOCKET(server_fd);
        return 1;
    }

    std::cout << "服务器在端口 " << PORT << " 上启动，等待连接..." << std::endl;
    std::cout << "按 Ctrl+C 停止服务器" << std::endl;

    // 设置超时，使accept能够定期检查running标志
    struct timeval timeout;
    timeout.tv_sec = 1;
    timeout.tv_usec = 0;
    
    while (running) {
        fd_set readfds;
        FD_ZERO(&readfds);
        FD_SET(server_fd, &readfds);
        
        // 使用select监控套接字，允许超时
        int activity = select(server_fd + 1, &readfds, NULL, NULL, &timeout);
        
        if (activity < 0 && errno != EINTR) {
            std::cerr << "select错误" << std::endl;
            break;
        }
        
        // 检查是否有新连接
        if (activity > 0 && FD_ISSET(server_fd, &readfds)) {
            // 接受客户端连接
            struct sockaddr_in client_address;
            socklen_t client_addrlen = sizeof(client_address);
            int client_socket = accept(server_fd, (struct sockaddr*)&client_address, &client_addrlen);
            
            if (client_socket < 0) {
                if (errno != EINTR) {
                    std::cerr << "接受连接失败: " << strerror(errno) << std::endl;
                }
                continue;
            }

            // 获取客户端IP地址
            char client_ip[INET_ADDRSTRLEN];
            inet_ntop(AF_INET, &client_address.sin_addr, client_ip, INET_ADDRSTRLEN);
            std::cout << "接受来自 " << client_ip << ":" << ntohs(client_address.sin_port) << " 的连接" << std::endl;

            // 接收数据
            char buffer[BUFFER_SIZE] = {0};
            int bytes_read = recv(client_socket, buffer, BUFFER_SIZE - 1, 0);
            
            if (bytes_read > 0) {
                buffer[bytes_read] = '\0';  // 确保字符串正确终止
                std::cout << "从 " << client_ip << " 接收到消息: " << buffer << std::endl;
                
                // 可选：发送响应回客户端
                // send(client_socket, "已收到消息", strlen("已收到消息"), 0);
            }
            else if (bytes_read == 0) {
                std::cout << "客户端断开连接" << std::endl;
            }
            else {
                std::cerr << "接收失败: " << strerror(errno) << std::endl;
            }

            // 关闭客户端连接
            CLOSE_SOCKET(client_socket);
        }
    }

    // 关闭服务器套接字
    CLOSE_SOCKET(server_fd);
    
    // 在Windows上清理Winsock
    #ifdef _WIN32
        WSACleanup();
    #endif

    std::cout << "服务器已关闭" << std::endl;

    return 0;
} 