package com.ticketmanagement.exception;

public class CommentNotAllowedException extends ClientErrorException {

    public CommentNotAllowedException() {
        super("A finished ticket cannot receive comments.");
    }

    public CommentNotAllowedException(String message, Throwable ex) {
        super(message, ex);
    }
}
