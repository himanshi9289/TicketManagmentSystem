package com.ticketmanagement.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ClientErrorException extends RuntimeException {

    public ClientErrorException(String message) {
        super(message);
    }

    public ClientErrorException(String message, Throwable ex) {
        super(message, ex);
    }
}
