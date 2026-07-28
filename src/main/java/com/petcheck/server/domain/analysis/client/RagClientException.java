package com.petcheck.server.domain.analysis.client;

import lombok.Getter;

@Getter
public class RagClientException extends RuntimeException {

    public enum Type {
        CONNECTION,
        TIMEOUT,
        CLIENT_ERROR,
        SERVER_ERROR,
        EMPTY_RESPONSE,
        UNEXPECTED
    }

    private final Type type;
    private final Integer statusCode;

    public RagClientException(Type type, String message) {
        this(type, null, message, null);
    }

    public RagClientException(Type type, String message, Throwable cause) {
        this(type, null, message, cause);
    }

    public RagClientException(Type type, Integer statusCode, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
        this.statusCode = statusCode;
    }
}
