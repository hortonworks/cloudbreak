package com.sequenceiq.freeipa.flow.freeipa.upscale.action;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;

@Component
public class PrivateIdProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrivateIdProvider.class);

    public Long getFirstValidPrivateId(Set<InstanceGroup> instanceGroups) {
        LOGGER.debug("Get first valid PrivateId of instanceGroups");
        long id = instanceGroups.stream()
                .flatMap(ig -> ig.getAllInstanceMetaData().stream())
                .filter(im -> im.getPrivateId() != null)
                .map(InstanceMetaData::getPrivateId)
                .map(i -> i + 1)
                .max(Long::compare)
                .orElse(0L);
        LOGGER.debug("First valid privateId: {}", id);
        return id;
    }
}
