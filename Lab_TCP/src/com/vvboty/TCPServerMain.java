package com.vvboty;

public class TCPServerMain {

    public static void main(String[] args){
        if (args.length < 1) {
            System.out.println("Port for TCP server not found!");
        }
        else{
            TCPServer server = new TCPServer(Integer.parseInt(args[0]));
            System.out.println("Server started listening");
            server.listen();
        }
    }
}
