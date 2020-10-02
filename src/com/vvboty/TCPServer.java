package com.vvboty;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer {
    private ServerSocket mainServerSocket;
    private static final boolean SERVER_ONLINE = true;
    private int numOfClients;

    TCPServer(int serverPort){
        try {
            mainServerSocket = new ServerSocket(serverPort);
            new File("uploads").mkdirs();
            numOfClients = 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void listen(){
        Socket clientSocket;
        try {
            while (SERVER_ONLINE){
                clientSocket = mainServerSocket.accept();
                Thread clientThread = new Thread(new ServerReadThread(clientSocket),"TCPClient: " + numOfClients);
                numOfClients++;
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
