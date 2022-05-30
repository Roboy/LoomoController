import random
import socket
import time
from datetime import datetime
from threading import Thread

HOST = "172.20.10.2"  # Standard loopback interface address (localhost)
PORT = 5050  # Port to listen on (non-privileged ports are > 1023)

velocity_commands = [
    (0.5, 0.0),
    (0.5, 0.0),
    (0.5, 0.0),
    (0.5, 0.0),
    (0.5, 0.0),
    (-0.5, 0.0),
    (-0.5, 0.0),
    (-0.5, 0.0),
    (-0.5, 0.0),
    (-0.5, 0.0),
    (-0.5, 0.0),
    (-0.5, 0.0),
    (0.0, 0.5),
    (0.0, 0.5),
    (0.0, 0.5),
    (0.0, 0.5),
    (0.0, 0.5),
    (0.0, 0.5),
    (0.0, -0.5),
    (0.0, -0.5),
    (0.0, -0.5),
    (0.0, -0.5),
    (0.0, -0.5),
    (0.0, -0.5),
    (0.0, -0.5),
    (0.0, -0.5),
    (0.0, -0.5),
]

def publisher(conn):
    try:
        while True:
            timestamp = datetime.now().strftime("%I:%M:%S %p")
            timestamp = timestamp.encode()
            print(f"sending {timestamp}")
            conn.send(timestamp)
            time.sleep(2)
    except:
        print("publisher died, Connection got lost, opening a new socket")

def command_publisher(conn):
    try:
        for i in range(4):
            for command in velocity_commands:
                go, turn = command
                go_message = f"GO,{go},{turn},"
                conn.send(go_message.encode())
                time.sleep(0.3)
    except:
        print("publisher died, Connection got lost, opening a new socket")


def listener(conn):
    try:
        while True:
            data = conn.recv(1024)
            print(data)
    except:
        print("listener died, Connection got lost, opening a new socket")
        open_socket()


def open_socket():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        print("Socket connection successful")
        s.listen()
        conn, addr = s.accept()
        Thread(target=command_publisher, args=(conn,)).start()
        Thread(target=listener, args=(conn,)).start()

open_socket()
