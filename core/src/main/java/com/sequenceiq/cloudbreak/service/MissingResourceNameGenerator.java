package com.sequenceiq.cloudbreak.service;

import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MissingResourceNameGenerator {

    @Value("${cb.missing.resource.name.pattern:'%s%s'}")
    private String namePatternForMissingReources;

    public String generateName(APIResourceType apiResourceType) {
        return apiResourceType != null ? String.format("%s%s", apiResourceType.namePrefix(), UUID.randomUUID().toString())
                : String.format("%s%s", "un", UUID.randomUUID().toString());
    }
}
