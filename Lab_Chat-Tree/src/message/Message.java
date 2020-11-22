package message;

import java.net.InetAddress;
import java.util.UUID;

public class Message {
    private UUID uuid;
    private String message;
    private MessageType messageType;
    private InetAddress ipSource;
    private Integer portSource;

    public Message(String message, MessageType messageType, InetAddress ipSource, Integer portSource) {
        this.uuid = UUID.randomUUID();
        this.message = message;
        this.messageType = messageType;
        this.ipSource = ipSource;
        this.portSource = portSource;
    }

    public Message(String uuid, String message, Integer messageType, InetAddress ipSource, Integer portSource) {
        this.uuid = UUID.fromString(new StringBuilder()
                .append(uuid, 0, 8)
                .append('-')
                .append(uuid, 8, 12)
                .append('-')
                .append(uuid, 12, 16)
                .append('-')
                .append(uuid, 16, 20)
                .append('-')
                .append(uuid, 20, 32).toString());
        this.message = message;
        switch (messageType) {
            case 0:
                this.messageType = MessageType.Delivered;
                break;
            case 1:
                this.messageType = MessageType.Message;
                break;
            case 2:
                this.messageType = MessageType.NewConnection;
                break;
            case 3:
                this.messageType = MessageType.NodeUpdate;
                break;
        }
        this.ipSource = ipSource;
        this.portSource = portSource;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getMessage() {
        return message;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public InetAddress getIpSource() {
        return ipSource;
    }

    public Integer getPortSource() {
        return portSource;
    }

    public void setIpSource(InetAddress ipSource) {
        this.ipSource = ipSource;
    }

    public void setPortSource(Integer portSource) {
        this.portSource = portSource;
    }
}
