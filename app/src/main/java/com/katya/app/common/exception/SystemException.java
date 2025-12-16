package com.katya.app.common.exception;

import com.katya.app.util.ErrorCode;
import lombok.Getter;


@Getter
public class SystemException extends RuntimeException {

    private final String errorCode;

    public SystemException(String message) {
        super(message);
        this.errorCode = ErrorCode.SYSTEM_ERROR.toString();
    }

    public SystemException(String errorCode, String message) {
        super(message);
        this.errorCode = ErrorCode.SYSTEM_ERROR.toString();
    }

    public SystemException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.SYSTEM_ERROR.toString();
    }

    public SystemException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}