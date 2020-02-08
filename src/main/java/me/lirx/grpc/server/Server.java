package me.lirx.grpc.server;

import me.lirx.grpc.server.grpc.MessageServer;

import java.io.IOException;

public class Server {
    public static void main(String[] args) {
        try {
            MessageServer messageServer = new MessageServer("127.0.0.1", 10080);
            Runtime.getRuntime().addShutdownHook(new Thread(messageServer::stop));
            messageServer.start().blockUntilShutdown();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
