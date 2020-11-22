package message;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

public class MessageInputStream {
    private DatagramSocket socket;

    public MessageInputStream(DatagramSocket socket) {
        this.socket = socket;
    }

    public void closeSocket() {
        this.socket.close();
    }

    public void enableTimeout(int ms) {
        try {
            socket.setSoTimeout(ms);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public Message readMessage() {
        DatagramPacket messagePacket = new DatagramPacket(new byte[65571], 65571);
        try {
            socket.receive(messagePacket);
        } catch (IOException e) {
            return null;
        }
        byte[] uuid = Arrays.copyOfRange(messagePacket.getData(), 0, 32);
        byte[] messageType = Arrays.copyOfRange(messagePacket.getData(), 32, 33);
        byte[] message = Arrays.copyOfRange(messagePacket.getData(), 33, messagePacket.getData().length);
        return new Message(new String(uuid).trim(), new String(message).trim(), new BigInteger(messageType).intValue(), messagePacket.getAddress(), messagePacket.getPort());
    }

    public DatagramSocket getSocket() {
        return socket;
    }
}
