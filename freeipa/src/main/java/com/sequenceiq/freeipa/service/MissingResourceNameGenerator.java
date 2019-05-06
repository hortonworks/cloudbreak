package com.sequenceiq.freeipa.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.APIResourceType;

@Service
public class MissingResourceNameGenerator {

    @Value("${cb.missing.resource.name.pattern:'%s%s'}")
    private String namePatternForMissingReources;

    public String generateName(APIResourceType apiResourceType) {
        return apiResourceType != null ? String.format("%s%s", apiResourceType.namePrefix(), UUID.randomUUID().toString())
                : String.format("%s%s", "un", UUID.randomUUID().toString());
    }
}
