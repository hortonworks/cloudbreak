package com.sequenceiq.environment.environment.service.freeipa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;

@Service
public class FreeIpaInstanceCountByGroupProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaInstanceCountByGroupProvider.class);

    @Value("${environment.freeipa.groupInstanceCount.default}")
    private Integer defaultInstanceCountByGroup;

    public int getInstanceCount(AttachedFreeIpaRequest request) {
        int instanceCount = defaultInstanceCountByGroup;
        if (request != null && request.getInstanceCountByGroup() != null) {
            instanceCount = request.getInstanceCountByGroup();
        } else {
            LOGGER.debug("Attached FreeIpa request doesn't contain instance count by group, falling back to the default: '{}'", defaultInstanceCountByGroup);
        }
        return instanceCount;
    }

    public int getDefaultInstanceCount() {
        return defaultInstanceCountByGroup;
    }
}
