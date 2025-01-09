package com.sequenceiq.cloudbreak.common.converter;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.util.UuidUtil;

@Service
public class ResourceNameGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceNameGenerator.class);

    public String generateName(APIResourceType apiResourceType) {
        return apiResourceType != null ? String.format("%s%s", apiResourceType.namePrefix(), UUID.randomUUID())
                : String.format("%s%s", "un", UUID.randomUUID());
    }

    public String generateHashBasedName(APIResourceType apiResourceType, Optional<String> base) {
        String uuid = base.isPresent() ? UuidUtil.nameBasedUuid(base.get()) : UuidUtil.randomUuid();
        LOGGER.debug("Generated name: {}, base string: {}", uuid, base);
        return String.format("%s%s", apiResourceType.namePrefix(), uuid);
    }

}