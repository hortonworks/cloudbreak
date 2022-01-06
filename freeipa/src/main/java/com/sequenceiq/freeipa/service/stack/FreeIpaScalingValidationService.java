package com.sequenceiq.freeipa.service.stack;

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.FormFactor;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@Service
@ConfigurationProperties(prefix = "freeipa.scaling")
public class FreeIpaScalingValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaScalingValidationService.class);

    @Inject
    private EntitlementService entitlementService;

    private Map<FormFactor, List<FormFactor>> allowedScalingPaths;

    public void validateStackForUpscale(Set<InstanceMetaData> allInstances, Stack stack) {
        if (allInstances.isEmpty()) {
            LOGGER.warn("Instances are empty for stack.");
            throw new BadRequestException("There are no instances available for scaling!");
        } else if (allInstances.size() >= FormFactor.HA.getInstanceCount()) {
            LOGGER.warn("FreeIPA instance count is bigger then allowed. Size: [{}]", allInstances.size());
            throw new BadRequestException("Scaling currently only available for FreeIPA installation with 1 to 3 instances");
        } else if (isAnyInstanceInNotAvailableState(allInstances)) {
            throwErrorForNotAvailableInstances(allInstances);
        } else if (!stack.isAvailable()) {
            LOGGER.warn("Refusing upscale as stack is not available. Current state: [{}]", stack.getStackStatus());
            throw new BadRequestException("Stack is not in available state, refusing to upscale. Current state: " + stack.getStackStatus().getStatus());
        }
    }

    public void validateStackForDownscale(Set<InstanceMetaData> allInstances, Stack stack) {
        if (allInstances.isEmpty()) {
            LOGGER.warn("Instances are empty for stack.");
            throw new BadRequestException("There are no instances available for scaling!");
        } else if (allInstances.size() != FormFactor.HA.getInstanceCount()) {
            LOGGER.warn("FreeIPA instance count is not allowed. Size: [{}]", allInstances.size());
            throw new BadRequestException("Downscaling currently only available for FreeIPA installation with 3 instances");
        } else if (isAnyInstanceInNotAvailableState(allInstances)) {
            throwErrorForNotAvailableInstances(allInstances);
        } else if (!stack.isAvailable()) {
            LOGGER.warn("Refusing downscale as stack is not available. Current state: [{}]", stack.getStackStatus());
            throw new BadRequestException("Stack is not in available state, refusing to downscale. Current state: " + stack.getStackStatus().getStatus());
        }
    }

    private boolean isAnyInstanceInNotAvailableState(Set<InstanceMetaData> allInstances) {
        return allInstances.stream().anyMatch(not(InstanceMetaData::isAvailable));
    }

    private void throwErrorForNotAvailableInstances(Set<InstanceMetaData> allInstances) {
        Set<String> notAvailableInstances = allInstances.stream()
                .filter(not(InstanceMetaData::isAvailable))
                .map(InstanceMetaData::getInstanceId)
                .collect(Collectors.toSet());
        LOGGER.warn("Instances are not available, refusing to scaling. Instances: {}", notAvailableInstances);
        throw new BadRequestException("Some of the instances is not available. Please fix them first! Instances: " + notAvailableInstances);
    }
}

