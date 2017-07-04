package com.sequenceiq.cloudbreak.service;

import org.springframework.stereotype.Service;

@Service
public class Clock {

    public long getCurrentTime() {
        return System.currentTimeMillis();
    }
}
