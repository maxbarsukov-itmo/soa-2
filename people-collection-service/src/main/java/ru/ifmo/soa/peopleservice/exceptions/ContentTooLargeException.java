package ru.ifmo.soa.peopleservice.exceptions;

public class ContentTooLargeException extends RuntimeException {
    public ContentTooLargeException(String message) {
        super(message);
    }
}
