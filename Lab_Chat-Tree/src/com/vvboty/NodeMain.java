package com.vvboty;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

public class NodeMain {
    public static void main(String[] args) {
        Node node = null;
        if (args.length == 5) {
            node = new Node(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3], Integer.parseInt(args[4]));
        } else if (args.length == 3) {
            node = new Node(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        } else {
            System.out.println("5 or 3 arguments!");
            System.exit(0);
        }
        try {
            node.connect();
            node.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
