package me.lirx.grpc.server.grpc;

import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import me.lirx.grpc.server.grpc.interceptor.ServerHeaderInterceptor;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;

public class MessageServer {
    private static final String trustCertPath = "cert\\ca\\ca.crt";
    private static final String keyCertChainFilePath = "cert\\server\\server.crt";
    private static final String keyFilePath = "cert\\server\\server.pem";
    private Server server;

    public MessageServer(String host, int port) throws SSLException {
        String classPath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("")).getPath();
        SslContext sslContext = GrpcSslContexts.forServer(new File(classPath + keyCertChainFilePath), new File(classPath + keyFilePath))
                .trustManager(new File(classPath + trustCertPath)).clientAuth(ClientAuth.REQUIRE)
                .sslProvider(SslProvider.OPENSSL).build();
        server = NettyServerBuilder.forAddress(new InetSocketAddress(host, port)).addService(
                ServerInterceptors.intercept(new MessageServiceImpl(), new ServerHeaderInterceptor())
        ).sslContext(sslContext).build();
    }

    public MessageServer start() throws IOException {
        server.start();
        System.out.println("started.");
        return this;
    }

    public void stop() {
        if (server != null && !server.isShutdown()) {
            server.shutdown();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
