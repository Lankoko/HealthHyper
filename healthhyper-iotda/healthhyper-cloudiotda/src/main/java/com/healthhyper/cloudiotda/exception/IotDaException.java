package com.healthhyper.cloudiotda.exception;

public class IotDaException extends RuntimeException {
    private String errorCode; // 华为云错误码

    public IotDaException(String message) {
        super(message);
    }

    public IotDaException(String message, Throwable cause) {
        super(message, cause);
    }

    public IotDaException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    // getter/setter
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
