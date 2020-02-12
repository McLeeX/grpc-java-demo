package me.lirx.grpc.client;

import me.lirx.grpc.client.grpc.MessageClient;
import me.lirx.grpc.common.AppException;

import java.io.IOException;

public class Client {
    public static void main(String[] args) {
        try (MessageClient client = new MessageClient("127.0.0.1", 10080, "root", "1q2w3e")) {
            String response = client.sendMessage("test header", "test content");
            System.out.println("服务端返回：" + response);
        } catch (IOException | AppException e) {
            e.printStackTrace();
        }
    }
}
