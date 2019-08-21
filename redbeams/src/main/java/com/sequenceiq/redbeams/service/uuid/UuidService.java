package com.sequenceiq.redbeams.service.uuid;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class UuidService {
    public String randomUuid() {
        return UUID.randomUUID().toString();
    }
}
