package me.lirx.grpc.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import me.lirx.grpc.common.AppException;
import me.lirx.proto.Message;
import me.lirx.proto.MessageServiceGrpc;
import me.lirx.proto.Response;

import javax.net.ssl.SSLException;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MessageClient implements Closeable {
    private ManagedChannel channel;
    private MessageServiceGrpc.MessageServiceBlockingStub blockingStub;
    private static final String trustCertPath = "cert\\ca\\ca.crt";
    private static final String keyCertChainFilePath = "cert\\client\\client.crt";
    private static final String keyFilePath = "cert\\client\\client.pem";

    public MessageClient(String host, int port, SslContext sslContext) {
        channel = NettyChannelBuilder.forAddress(host, port).negotiationType(NegotiationType.TLS).sslContext(sslContext).build();
        blockingStub = MessageServiceGrpc.newBlockingStub(channel);
    }

    public MessageClient(String host, int port) throws SSLException {
        String classPath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("")).getPath();
        SslContext sslContext = GrpcSslContexts.forClient().trustManager(new File(classPath + trustCertPath))
                .keyManager(new File(classPath + keyCertChainFilePath), new File(classPath + keyFilePath))
                .sslProvider(SslProvider.OPENSSL_REFCNT).build();
        channel = NettyChannelBuilder.forAddress(host, port).negotiationType(NegotiationType.TLS).sslContext(sslContext).build();
        blockingStub = MessageServiceGrpc.newBlockingStub(channel);
    }

    public String sendMessage(String header, String content) throws AppException {
        Message message = Message.newBuilder().setHeader(header).setContent(content).build();
        Response response = blockingStub.sendMessage(message);
        int errorCode = response.getErrCode();
        String responseContent = response.getContent();
        if (errorCode == 0) {
            return responseContent;
        } else {
            throw new AppException(errorCode, responseContent);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }
}
