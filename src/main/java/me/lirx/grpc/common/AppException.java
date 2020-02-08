package me.lirx.grpc.common;

public class AppException extends Exception {
    private int errorCode;

    public AppException(int errorCode, String message) {
        super("错误码:" + errorCode + ",详情：" + message + "。");
        this.errorCode = errorCode;
    }

    public AppException(int errorCode, String message, Throwable cause) {
        super("错误码:" + errorCode + ",详情：" + message + "。", cause);
        this.errorCode = errorCode;
    }
}
