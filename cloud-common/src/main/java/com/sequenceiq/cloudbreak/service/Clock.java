package com.sequenceiq.cloudbreak.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

@Service
public class Clock {

    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public Instant getCurrentInstant() {
        return Instant.now();
    }
}
