package me.lirx.grpc.server.grpc.interceptor;

import io.grpc.*;
import me.lirx.grpc.common.MetadataHeaders;

public class ServerHeaderInterceptor implements ServerInterceptor {

    static final Metadata.Key<String> CUSTOM_HEADER_KEY =
            Metadata.Key.of("custom_server_header_key", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        //获取Headers
        String user = headers.get(MetadataHeaders.USER);
        String password = headers.get(MetadataHeaders.PASSWORD);
        if (!"root".equals(user) || !"1q2w3e".equals(password)) {
            throw new StatusRuntimeException(Status.UNAUTHENTICATED);
        }
        return next.startCall(call, headers);
    }
}
