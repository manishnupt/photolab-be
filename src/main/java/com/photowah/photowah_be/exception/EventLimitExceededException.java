package com.photowah.photowah_be.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class EventLimitExceededException extends RuntimeException {

    public EventLimitExceededException(int limit) {
        super("Event limit of " + limit + " reached for your subscription");
    }
}
