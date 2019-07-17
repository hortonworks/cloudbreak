package com.sequenceiq.redbeams.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class UUIDGeneratorService {

    public String randomUuid() {
        return UUID.randomUUID().toString();
    }
}
