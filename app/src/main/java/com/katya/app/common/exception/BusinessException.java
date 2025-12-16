package com.katya.app.common.exception;

import com.katya.app.util.ErrorCode;
import lombok.Data;

@Data
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    public BusinessException(String message) {
        super(message);
        this.errorCode = ErrorCode.BUSINESS_ERROR.toString();
        this.args = null;
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = ErrorCode.BUSINESS_ERROR.toString();
        this.args = null;
    }

    public BusinessException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = ErrorCode.BUSINESS_ERROR.toString();
        this.args = args;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.BUSINESS_ERROR.toString();
        this.args = null;
    }
}
