package com.vvboty;

import message.Message;
import message.MessageInputStream;
import message.MessageOutputStream;
import message.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;

public class Node {
    private static final boolean NODE_IS_ONLINE = true;
    private static final int MESSAGE_TIMEOUT = 5;
    private static final int MESSAGE_MS_GAP = 5000;
    private String name;
    private Integer packetLoss;
    private Integer nodePort;
    private MessageOutputStream sendSocket;
    private MessageInputStream readSocket;
    private BufferedReader in;
    private HashMap<Message, SendTry> notSent;
    private HashMap<InetSocketAddress, InetSocketAddress> neighbours;
    private InetSocketAddress reconnectNode;

    public Node(String name, Integer packetLoss, Integer port) {
        this.name = name;
        this.nodePort = port;
        this.packetLoss = packetLoss;
        try {
            this.readSocket = new MessageInputStream(new DatagramSocket(port));
            this.sendSocket = new MessageOutputStream(new DatagramSocket(port + 1));
            this.in = new BufferedReader(new InputStreamReader(System.in));
            this.notSent = new HashMap<>();
            this.neighbours = new HashMap<>();
            this.reconnectNode = null;
        } catch (SocketException e) {
            System.out.println("Socket init error!");
            System.exit(0);
        }
    }

    public Node(String name, Integer packetLoss, Integer port, String ipAddress, Integer mPort) {
        this.name = name;
        this.nodePort = port;
        this.packetLoss = packetLoss;
        try {
            this.readSocket = new MessageInputStream(new DatagramSocket(port));
            this.sendSocket = new MessageOutputStream(new DatagramSocket());
            this.in = new BufferedReader(new InputStreamReader(System.in));
            this.notSent = new HashMap<>();
            this.neighbours = new HashMap<>();
            neighbours.put(new InetSocketAddress(InetAddress.getByName(ipAddress), mPort), null);
            this.reconnectNode = new InetSocketAddress(InetAddress.getByName(ipAddress), mPort);
        } catch (SocketException e) {
            System.out.println("Socket init error");
            System.exit(0);
        } catch (UnknownHostException e) {
            System.out.println("Host resolving error");
            System.exit(0);
        }
    }

    private Message findMessageByUUID(UUID uuid) {
        for (Map.Entry<Message, SendTry> msg : notSent.entrySet()) {
            if (msg.getKey().getUuid().equals(uuid))
                return msg.getKey();
        }
        return null;
    }

    private Boolean isMessageLost() {
        return (new Random().nextDouble()) * 100 < this.packetLoss;
    }

    public void connect() throws IOException {
        if (this.reconnectNode == null)
            return;
        readSocket.enableTimeout(1000);
        StringBuilder reconnectInfo = new StringBuilder(nodePort.toString());
        for (int i = 0; i < MESSAGE_TIMEOUT; i++) {
            System.out.println("Trying to connect to host");
            Message registration = new Message(reconnectInfo.toString(),
                    MessageType.NewConnection,
                    this.reconnectNode.getAddress(),
                    this.reconnectNode.getPort());
            sendSocket.sendMessage(registration);
            Message sent = readSocket.readMessage();
            if (sent != null && sent.getMessageType().equals(MessageType.Delivered)) {
                if (sent.getMessage().split(" ").length == 2) {
                    String[] nodeInfo = sent.getMessage().split(" ");
                    if (nodeInfo.length > 1) {
                        neighbours.put(reconnectNode, new InetSocketAddress(InetAddress.getByName(nodeInfo[0]), Integer.parseInt(nodeInfo[1])));
                    } else {
                        neighbours.put(reconnectNode, null);
                    }
                } else {
                    neighbours.put(reconnectNode, null);
                }
                System.out.println("Connected");
                return;
            }
        }
        System.out.println("Host timeout");
        this.neighbours.remove(reconnectNode);
        reconnectNode = null;
    }

    private void sendToNeighbours(String message, InetSocketAddress notSendTo, MessageType messageType) throws IOException {
        message = new StringBuilder().append(nodePort).append(" ").append(message).toString();
        for (InetSocketAddress neighbour : neighbours.keySet()) {
            if (!neighbour.equals(notSendTo)) {
                Message messageToSend = new Message(message, messageType, neighbour.getAddress(), neighbour.getPort());
                sendSocket.sendMessage(messageToSend);
                notSent.put(messageToSend, new SendTry());
            }
        }
        //System.out.println("Message sent");
    }

    private void readMessage(Message msg) throws IOException {
        switch (msg.getMessageType()) {
            case Delivered:
                if (findMessageByUUID(UUID.fromString(msg.getMessage())) != null) {
                    notSent.remove(findMessageByUUID(UUID.fromString(msg.getMessage())));
                }
                break;
            case Message:
                int portSource = Integer.parseInt(msg.getMessage().split(" ", 2)[0]);
                String message = msg.getMessage().split(" ", 2)[1];
                System.out.println(/*msg.getIpSource() + ":" + portSource + "| " + */message);
                sendToNeighbours(message, new InetSocketAddress(msg.getIpSource(), portSource), MessageType.Message);
                Message sendingConfirmation = new Message(msg.getUuid().toString(),
                        MessageType.Delivered,
                        msg.getIpSource(),
                        portSource);
                sendSocket.sendMessage(sendingConfirmation);
                break;
            case NodeUpdate:
                String[] updateInfo = msg.getMessage().split(" ");
                if (updateInfo.length == 1) {
                    neighbours.put(new InetSocketAddress(msg.getIpSource(), Integer.parseInt(updateInfo[0])), null);
                } else {
                    neighbours.put(new InetSocketAddress(msg.getIpSource(), Integer.parseInt(updateInfo[0])),
                            new InetSocketAddress(InetAddress.getByName(updateInfo[1]), Integer.parseInt(updateInfo[2])));
                }
                Message sendingConfirm = new Message(msg.getUuid().toString(),
                        MessageType.Delivered,
                        msg.getIpSource(),
                        Integer.parseInt(updateInfo[0]));
                sendSocket.sendMessage(sendingConfirm);
                System.out.println("Reconnecting info is updated");
                break;
            case NewConnection:
                System.out.println("Connecting new neighbour");
                String registerInfo = msg.getMessage();
                InetSocketAddress newNeighbour = new InetSocketAddress(msg.getIpSource(), Integer.parseInt(registerInfo));
                neighbours.put(newNeighbour, null);
                System.out.println("Sending ack to " + newNeighbour.toString());
                StringBuilder reconnectingInfo = new StringBuilder();
                if (reconnectNode != null)
                    reconnectingInfo.append(reconnectNode.getAddress().toString().replaceFirst("/", "")).append(" ").append(reconnectNode.getPort());
                sendSocket.sendMessage(new Message(reconnectingInfo.toString(), MessageType.Delivered, msg.getIpSource(), Integer.parseInt(msg.getMessage())));
                if(!neighbours.isEmpty()){
                    neighbours.entrySet().removeIf(entry -> {
                        if(entry.getValue() != null)
                            return entry.getValue().equals(newNeighbour);
                        else
                            return false;
                    });
                }
                if (neighbours.size() == 2) {
                    reconnectNode = new ArrayList<>(neighbours.keySet()).get(0);
                    String newReconnectingInfo = reconnectNode.getAddress().toString().replaceFirst("/", "") + " " + reconnectNode.getPort();
                    sendToNeighbours(newReconnectingInfo, reconnectNode, MessageType.NodeUpdate);
                    newReconnectingInfo = newNeighbour.getAddress().toString().replaceFirst("/", "") + " " + newNeighbour.getPort();
                    sendToNeighbours(newReconnectingInfo, newNeighbour, MessageType.NodeUpdate);
                }
                break;
        }
    }

    private void checkTimeout() throws IOException {
        ArrayList<Message> toRemove = new ArrayList<>();
        for (Map.Entry<Message, SendTry> nsm : notSent.entrySet()) {
            SendTry sendCheck = nsm.getValue();
            sendCheck.setMsFromSending((int) (new Date().getTime() - sendCheck.getSent().getTime()));
            if (sendCheck.getMsFromSending() > MESSAGE_MS_GAP * MESSAGE_TIMEOUT) {
                InetSocketAddress toDelete = new InetSocketAddress(nsm.getKey().getIpSource(), nsm.getKey().getPortSource());
                InetSocketAddress reconnectLeftover = neighbours.remove(toDelete);
                toRemove.add(nsm.getKey());
                for (Map.Entry<Message, SendTry> nsm2 : notSent.entrySet()) {
                    if (nsm.getKey().getIpSource().equals(nsm2.getKey().getIpSource())
                            && nsm.getKey().getPortSource().equals(nsm2.getKey().getPortSource())
                            && !nsm.getKey().getUuid().equals(nsm2.getKey().getUuid())) {
                        toRemove.add(nsm2.getKey());
                    }
                }
                System.out.println("Node disconnected");

                if (toDelete.equals(reconnectNode)) {
                    String registerInfo = "";
                    if (neighbours != null && neighbours.size() > 1) {
                        reconnectNode = new ArrayList<>(neighbours.keySet()).get(0);
                        for (InetSocketAddress recForRec: neighbours.keySet()) {
                            if(!recForRec.equals(reconnectNode)){
                                InetSocketAddress recForRecNode = recForRec;
                                registerInfo = nodePort + " " + recForRecNode.getAddress().toString().replaceFirst("/", "") + " " + recForRecNode.getPort();
                                sendSocket.sendMessage( new Message(registerInfo, MessageType.NodeUpdate, reconnectNode.getAddress(), reconnectNode.getPort()));
                                break;
                            }
                        }
                        registerInfo = reconnectNode.getAddress().toString().replaceFirst("/", "") + " " + reconnectNode.getPort();
                    } else {
                        reconnectNode = null;
                    }
                    if (neighbours != null && !neighbours.containsKey(reconnectNode)) {
                        reconnectNode = reconnectLeftover;
                        connect();
                    }
                    sendToNeighbours(registerInfo, reconnectNode, MessageType.NodeUpdate);
                }
                if(neighbours.size() > 1){
                    reconnectNode = new ArrayList<>(neighbours.keySet()).get(0);
                    String registerInfo = reconnectNode.getAddress().toString().replaceFirst("/", "") + " " + reconnectNode.getPort();
                    sendToNeighbours(registerInfo, reconnectNode, MessageType.NodeUpdate);
                    reconnectNode = new ArrayList<>(neighbours.keySet()).get(1);
                    registerInfo = reconnectNode.getAddress().toString().replaceFirst("/", "") + " " + reconnectNode.getPort();
                    sendToNeighbours(registerInfo, reconnectNode, MessageType.NodeUpdate);
                }

            } else if ((sendCheck.getTries() + 1) * MESSAGE_MS_GAP < sendCheck.getMsFromSending()) {
                sendCheck.setTries(sendCheck.getTries() + 1);
                sendSocket.sendMessage(nsm.getKey());
                System.out.println("Resending message. try #" + sendCheck.getTries() + ", ms: " + sendCheck.getMsFromSending());
            }
        }
        for (Message toDel : toRemove) {
            notSent.remove(toDel);
        }
    }

    public void start() {
        System.out.println("Node is online");
        readSocket.enableTimeout(100);
        while (NODE_IS_ONLINE) {
            try {
                Message msg = readSocket.readMessage();
                if (msg != null) {
                    //System.out.println("Got message from " + msg.getIpSource().toString() + ":" + msg.getPortSource().toString());
                    if (!isMessageLost()) {
                        readMessage(msg);
                    } else {
                        System.out.println("Message lost");
                    }
                } else{
                    if(in.ready()){
                        String newMessage = name + " says: " + in.readLine();
                        sendToNeighbours(newMessage, null, MessageType.Message);
                    }
                }
                checkTimeout();
            } catch (IOException e) {
                e.printStackTrace();
                this.readSocket.closeSocket();
                this.sendSocket.closeSocket();
                break;
            }
        }
    }
}
