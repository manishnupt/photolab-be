package com.photowah.photowah_be.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EventArchivedException extends RuntimeException {

    public EventArchivedException(java.util.UUID eventId) {
        super("Event " + eventId + " is archived and cannot accept new uploads");
    }
}
