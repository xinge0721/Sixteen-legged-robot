#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import socket
import threading
import sys

def handle_client(client_socket, client_address):
    """Handle each client connection"""
    print(f"Accepted connection from {client_address}")
    
    try:
        # Receive data
        data = client_socket.recv(1024).decode('utf-8').strip()
        print(f"Received message from {client_address}: {data}")
        
        # Optional: send response back to client
        # client_socket.send("Message received".encode('utf-8'))
    except Exception as e:
        print(f"Error handling client {client_address}: {e}")
    finally:
        # Close connection
        client_socket.close()

def start_server():
    server_host = '0.0.0.0'  # Listen on all interfaces
    server_port = 8080       # Same port as Android client
    
    # Create TCP socket
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    
    # Set port reuse
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    
    # Bind IP and port
    server_socket.bind((server_host, server_port))
    
    # Start listening
    server_socket.listen(5)
    print(f"Server started on {server_host}:{server_port}, waiting for connections...")
    
    try:
        while True:
            # Accept new client connection
            client_socket, client_address = server_socket.accept()
            
            # Create a new thread for each client
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
    # Print Python version and encoding info for debug
    print(f"Python version: {sys.version}")
    print(f"Default encoding: {sys.getdefaultencoding()}")
    
    # Start the server
    start_server() 