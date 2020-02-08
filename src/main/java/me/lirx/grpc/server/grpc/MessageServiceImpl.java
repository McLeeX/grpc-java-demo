package me.lirx.grpc.server.grpc;

import io.grpc.stub.StreamObserver;
import me.lirx.proto.Message;
import me.lirx.proto.MessageServiceGrpc;
import me.lirx.proto.Response;

public class MessageServiceImpl extends MessageServiceGrpc.MessageServiceImplBase {
    @Override
    public void sendMessage(Message request, StreamObserver<Response> responseObserver) {
        String header = request.getHeader();
        String content = request.getContent();
        System.out.println("接收到消息：[header=" + header + ", content=" + content + "]");
        Response response = Response.newBuilder().setErrCode(0).setContent("success.").build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
