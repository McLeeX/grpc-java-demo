package me.lirx.grpc.client;

import me.lirx.grpc.client.grpc.MessageClient;
import me.lirx.grpc.common.AppException;

import java.io.IOException;

public class Client {
    public static void main(String[] args) {
        try (MessageClient client = new MessageClient("localhost", 10080)) {
            System.out.println(client.sendMessage("test header", "test content"));
        } catch (IOException | AppException e) {
            e.printStackTrace();
        }
    }
}
