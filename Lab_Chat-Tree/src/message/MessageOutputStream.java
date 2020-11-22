package message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

public class MessageOutputStream {
    private DatagramSocket datagramSocket;

    public MessageOutputStream(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
    }

    public void closeSocket() {
        this.datagramSocket.close();
    }

    public void sendMessage(Message message) throws IOException {
        byte[] uuidBytes = message.getUuid().toString().replaceAll("-", "").getBytes();
        Integer messageType = message.getMessageType().ordinal();
        byte messageTypeBytes = messageType.byteValue();
        byte[] messageBytes = message.getMessage().getBytes();
        ByteArrayOutputStream bytesToSend = new ByteArrayOutputStream();
        bytesToSend.write(uuidBytes);
        bytesToSend.write(messageTypeBytes);
        bytesToSend.write(messageBytes);
        DatagramPacket packetToSend =  new DatagramPacket(bytesToSend.toByteArray(), uuidBytes.length + messageBytes.length + 1, message.getIpSource(), message.getPortSource());
        datagramSocket.send(packetToSend);
    }
}
