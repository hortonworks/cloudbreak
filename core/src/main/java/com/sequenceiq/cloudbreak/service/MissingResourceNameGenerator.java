package com.sequenceiq.cloudbreak.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.APIResourceType;

@Service
public class MissingResourceNameGenerator {

    @Value("${cb.missing.resource.name.pattern:'%s%s'}")
    private String namePatternForMissingReources;

    public String generateName(APIResourceType apiResourceType) {
        if (apiResourceType != null) {
            return String.format("%s%s", apiResourceType.namePrefix(), UUID.randomUUID().toString());
        } else {
            return String.format("%s%s", "un", UUID.randomUUID().toString());
        }
    }
}
