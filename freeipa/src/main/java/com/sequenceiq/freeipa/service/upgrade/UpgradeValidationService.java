package com.sequenceiq.freeipa.service.upgrade;

import static java.util.function.Predicate.not;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.ImageInfoResponse;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.SupportedOsService;

@Service
public class UpgradeValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeValidationService.class);

    private static final int MAX_NUMBER_OF_INSTANCES_FOR_UPGRADE = 3;

    @Inject
    private SupportedOsService supportedOsService;

    @Inject
    private EntitlementService entitlementService;

    public void validateUpgradeRequest(FreeIpaUpgradeRequest request) {
        if (Boolean.TRUE.equals(request.getAllowMajorOsUpgrade()) && !supportedOsService.isRhel8Supported()) {
            LOGGER.warn("Major OS upgrade is not supported");
            throw new BadRequestException("Major OS upgrade is not supported");
        }
        if (request.getImage() != null && !supportedOsService.isSupported(request.getImage().getOs())) {
            LOGGER.warn("Selected os '{}' is not supported", request.getImage().getOs());
            throw new BadRequestException(String.format("Selected os '%s' is not supported", request.getImage().getOs()));
        }
    }

    public void validateStackForUpgrade(Set<InstanceMetaData> allInstances, Stack stack) {
        if (allInstances.isEmpty()) {
            LOGGER.warn("Instances are empty for stack.");
            throw new BadRequestException("There are no instances available for upgrade!");
        } else if (allInstances.size() > MAX_NUMBER_OF_INSTANCES_FOR_UPGRADE) {
            LOGGER.warn("FreeIPA instance count is bigger then allowed. Size: [{}]", allInstances.size());
            throw new BadRequestException("Upgrade currently only available for FreeIPA installation with 1 to 3 instances");
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

    public void validateSelectedImageDifferentFromCurrent(ImageInfoResponse currentImage, ImageInfoResponse selectedImage, Set<String> instancesOnOldImage) {
        if (currentImage.getId().equals(selectedImage.getId()) && CollectionUtils.isEmpty(instancesOnOldImage)) {
            LOGGER.warn("Selected {} and current {} image are the same", selectedImage, currentImage);
            throw new BadRequestException("Selected and current image are the same with id: " + currentImage.getId());
        }
    }

    public void validateSelectedImageEntitledFor(String accountId, ImageInfoResponse selectedImage) {
        if (!entitlementService.isEntitledToUseOS(accountId, OsType.getByOs(selectedImage.getOs()))) {
            throw new BadRequestException(String.format("Your account is not entitled to use %s images.", selectedImage.getOs()));
        }
    }
}

