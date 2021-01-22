package com.eh7n.f1telemetry;

import java.io.IOException;
import java.net.*;

public class EchoClient {

    private DatagramSocket socket;
    private InetAddress address;

    private byte[] buf;

    public EchoClient() throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        address = InetAddress.getByName(Main.IP_ADDRESS);
    }

    public String sendEcho(String msg) throws IOException {
        buf = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, Main.PORT);
        socket.send(packet);
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        return received;
    }

    public void close() {
        socket.close();
    }
}
