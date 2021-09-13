package com.sequenceiq.freeipa.service.upgrade;

import static java.util.function.Predicate.not;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.ImageInfoResponse;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class UpgradeValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeValidationService.class);

    @Inject
    private EntitlementService entitlementService;

    public void validateStackForUpgrade(Set<InstanceMetaData> allInstances, Stack stack) {
        if (allInstances.isEmpty()) {
            LOGGER.warn("Instances are empty for stack.");
            throw new BadRequestException("There are no instances available for upgrade!");
        } else if (allInstances.size() > 2) {
            LOGGER.warn("FreeIPA instance count is bigger then allowed. Size: [{}]", allInstances.size());
            throw new BadRequestException("Upgrade currently only available for FreeIPA installation with 1 or 2 instances");
        } else if (isAnyInstanceInNotAvailableState(allInstances)) {
            throwErrorForNotAvailableInstances(allInstances);
        } else if (!stack.isAvailable()) {
            LOGGER.warn("Refusing upgrade as stack is not available. Current state: [{}]", stack.getStackStatus());
            throw new BadRequestException("Stack is not in available state, refusing to upgrade. Current state: " + stack.getStackStatus().getStatus());
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
        LOGGER.warn("Instances are not available, refusing to upgrade. Instances: {}", notAvailableInstances);
        throw new BadRequestException("Some of the instances is not available. Please fix them first! Instances: " + notAvailableInstances);
    }

    public void validateEntitlement(String accountId) {
        boolean freeIpaUpgradeEnabled = entitlementService.isFreeIpaUpgradeEnabled(accountId);
        if (!freeIpaUpgradeEnabled) {
            throw new BadRequestException("FreeIPA upgrade is not enabled for account");
        }
    }

    public void validateSelectedImageDifferentFromCurrent(ImageInfoResponse currentImage, ImageInfoResponse selectedImage) {
        if (currentImage.getId().equals(selectedImage.getId())) {
            LOGGER.warn("Selected {} and current {} image are the same", selectedImage, currentImage);
            throw new BadRequestException("Selected and current image are the same with id: " + currentImage.getId());
        }
    }
}

