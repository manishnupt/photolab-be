package com.photowah.photowah_be.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class StorageLimitExceededException extends RuntimeException {

    public StorageLimitExceededException(long usedMb, long limitMb) {
        super("Storage limit reached: " + usedMb + " MB used of " + limitMb + " MB");
    }
}
