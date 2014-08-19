package com.sequenceiq.periscope.rest;

public class AlarmNotFoundException extends RuntimeException {

    private final long id;

    public AlarmNotFoundException(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
