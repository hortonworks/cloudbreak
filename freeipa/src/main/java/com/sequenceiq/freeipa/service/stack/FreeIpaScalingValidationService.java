package com.sequenceiq.freeipa.service.stack;

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.AvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.ScalingPath;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.configuration.AllowedScalingPaths;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.multiaz.MultiAzCalculatorService;

@Service
public class FreeIpaScalingValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaScalingValidationService.class);

    @Inject
    private AllowedScalingPaths allowedScalingPaths;

    @Inject
    private VerticalScalingValidatorService verticalScalingValidatorService;

    @Inject
    private MultiAzCalculatorService multiAzCalculatorService;

    public void validateStackForUpscale(Set<InstanceMetaData> allInstances, Stack stack, ScalingPath scalingPath) {
        validateScalingIsUpscale(scalingPath);
        executeCommonValidations(allInstances, stack, scalingPath, OperationType.UPSCALE);
    }

    public void validateStackForVerticalUpscale(Stack stack, VerticalScaleRequest request) {
        verticalScalingValidatorService.validateRequest(stack, request);
    }

    public void validateStackForDownscale(Set<InstanceMetaData> allInstances, Stack stack, ScalingPath scalingPath, Set<String> instanceIdsToDelete) {
        validateScalingIsDownscale(scalingPath);
        validateInstanceIdsToDelete(allInstances, instanceIdsToDelete);
        executeCommonValidations(allInstances, stack, scalingPath, OperationType.DOWNSCALE);
        if (stack.isMultiAz()) {
            validateDownScaleForMultiAz(stack, scalingPath, allInstances, instanceIdsToDelete);
        }
    }

    private void validateInstanceIdsToDelete(Set<InstanceMetaData> allInstances, Set<String> instanceIdsToDelete) {
        if (Objects.nonNull(instanceIdsToDelete)) {
            validateInstanceIdsAreNotEmpty(instanceIdsToDelete);
            validateInstanceIdsArePartOfAllInstances(allInstances, instanceIdsToDelete);
            validateInstanceIdToDeleteAreNotPrimaryGateways(allInstances, instanceIdsToDelete);
        }
    }

    private void validateInstanceIdToDeleteAreNotPrimaryGateways(Set<InstanceMetaData> allInstances, Set<String> instanceIds) {
        Set<String> primaryGatewayInstanceMetadata = allInstances.stream()
                .filter(imd -> instanceIds.contains(imd.getInstanceId()))
                .filter(imd -> InstanceMetadataType.GATEWAY_PRIMARY == imd.getInstanceMetadataType())
                .map(InstanceMetaData::getInstanceId)
                .collect(Collectors.toSet());
        if (!primaryGatewayInstanceMetadata.isEmpty()) {
            String message = String.format("Refusing %s as instance ids contains an instance that is a primary gateway. Please select another " +
                    "instance. Primary gateway instance: %s.", OperationType.DOWNSCALE.getLowerCaseName(), primaryGatewayInstanceMetadata);
            LOGGER.warn(message);
            throw new BadRequestException(message);
        }
    }

    private void validateScalingIsUpscale(ScalingPath scalingPath) {
        if (scalingPath.getOriginalAvailabilityType().getInstanceCount() > scalingPath.getTargetAvailabilityType().getInstanceCount()) {
            throw new BadRequestException(String.format("Refusing %s as target node count is smaller than current. Current node count: %d, " +
                            "target node count: %d.", OperationType.UPSCALE.getLowerCaseName(), scalingPath.getOriginalAvailabilityType().getInstanceCount(),
                    scalingPath.getTargetAvailabilityType().getInstanceCount()));
        }
    }

    private void validateScalingIsDownscale(ScalingPath scalingPath) {
        if (scalingPath.getOriginalAvailabilityType().getInstanceCount() < scalingPath.getTargetAvailabilityType().getInstanceCount()) {
            throw new BadRequestException(String.format("Refusing %s as target node count is higher than current. Current node count: %d, " +
                            "target node count: %d.", OperationType.DOWNSCALE.getLowerCaseName(), scalingPath.getOriginalAvailabilityType().getInstanceCount(),
                    scalingPath.getTargetAvailabilityType().getInstanceCount()));
        }
    }

    private void validateInstanceIdsAreNotEmpty(Set<String> instanceIdsToDownscale) {
        if (instanceIdsToDownscale.isEmpty()) {
            throwErrorForEmptyDownscaleCandidates();
        }
    }

    private void validateInstanceIdsArePartOfAllInstances(Set<InstanceMetaData> allInstances, Set<String> instanceIdsToDownscale) {
        Set<String> allInstanceIds = allInstances.stream()
                .map(InstanceMetaData::getInstanceId).collect(Collectors.toSet());
        Set<String> unknownInstanceIds = Sets.difference(instanceIdsToDownscale, allInstanceIds);
        if (!unknownInstanceIds.isEmpty()) {
            throwErrorForUnknownDownscaleCandidates(unknownInstanceIds);
        }
    }

    private void executeCommonValidations(Set<InstanceMetaData> allInstances, Stack stack, ScalingPath scalingPath, OperationType operationType) {
        if (allInstances.isEmpty()) {
            throwErrorForNoInstance();
        }
        if (isAnyInstanceInNotAvailableState(allInstances)) {
            throwErrorForNotAvailableInstances(allInstances);
        }
        if (!stack.isAvailable()) {
            throwErrorForUnavailableStack(stack, operationType);
        }
        if (nodeCountAlreadyMatchesTarget(allInstances, scalingPath)) {
            throwErrorForNoChangeInNodeCountRequested(operationType, allInstances, scalingPath);
        }
        if (scalingPathDisabled(scalingPath)) {
            throwErrorForUnsupportedScalingPath(scalingPath, operationType);
        }
    }

    private boolean scalingPathDisabled(ScalingPath scalingPath) {
        List<AvailabilityType> targetAvailabilityTypes = allowedScalingPaths.getPaths().get(scalingPath.getOriginalAvailabilityType());
        return Objects.isNull(targetAvailabilityTypes) || !targetAvailabilityTypes.contains(scalingPath.getTargetAvailabilityType());
    }

    private void validateDownScaleForMultiAz(Stack stack, ScalingPath scalingPath, Set<InstanceMetaData> allInstances, Set<String> instanceIdsToDelete) {
        AvailabilityZoneConnector availabilityZoneConnector = multiAzCalculatorService.getAvailabilityZoneConnector(stack);
        if (availabilityZoneConnector != null) {
            long numberOfZonesAfterDownscale = calculateNumberOfZonesAfterDownscale(scalingPath, allInstances, instanceIdsToDelete);
            LOGGER.debug("Number of zones after downscale will be {}", numberOfZonesAfterDownscale);
            if (numberOfZonesAfterDownscale < availabilityZoneConnector.getMinZonesForFreeIpa()) {
                throw new BadRequestException(String.format("%s will result in number of availability zones less than minimum number of availability zones " +
                                "needed for Multi AZ deployment. Number of zones after %s: %d. Minimum zones needed: %d",
                        OperationType.DOWNSCALE.getLowerCaseName(), OperationType.DOWNSCALE.getLowerCaseName(), numberOfZonesAfterDownscale,
                        availabilityZoneConnector.getMinZonesForFreeIpa()));
            }
        } else {
            LOGGER.info("Implementation for AvailabilityZoneConnector is not present for CloudPlatform {} and PlatformVariant {}." +
                            "Skipping MultiAz validations for {}",
                    stack.getCloudPlatform(), stack.getPlatformvariant(), OperationType.DOWNSCALE.getLowerCaseName());
        }
    }

    private boolean nodeCountAlreadyMatchesTarget(Set<InstanceMetaData> allInstances, ScalingPath scalingPath) {
        return allInstances.size() == scalingPath.getTargetAvailabilityType().getInstanceCount();
    }

    private void throwErrorForNoInstance() {
        LOGGER.warn("Instances are empty for stack.");
        throw new BadRequestException("There are no instances available for scaling!");
    }

    private boolean isAnyInstanceInNotAvailableState(Set<InstanceMetaData> allInstances) {
        return allInstances.stream().anyMatch(not(InstanceMetaData::isAvailable));
    }

    private void throwErrorForNotAvailableInstances(Set<InstanceMetaData> allInstances) {
        Set<String> notAvailableInstances = allInstances.stream()
                .filter(not(InstanceMetaData::isAvailable))
                .map(InstanceMetaData::getInstanceId)
                .collect(Collectors.toSet());
        LOGGER.warn("Instances are not available, refusing to scale. Instances: {}", notAvailableInstances);
        throw new BadRequestException("Some of the instances is not available. Please fix them first! Instances: " + notAvailableInstances);
    }

    private void throwErrorForUnavailableStack(Stack stack, OperationType scaleType) {
        LOGGER.warn("Refusing {} as stack is not available. Current state: [{}]",
                scaleType.name(),
                stack.getStackStatus());
        throw new BadRequestException("Stack is not in available state, refusing to " + scaleType.getLowerCaseName() +
                ". Current state: " + stack.getStackStatus().getStatus());
    }

    private void throwErrorForNoChangeInNodeCountRequested(OperationType scaleType, Set<InstanceMetaData> allInstances, ScalingPath scalingPath) {
        String message = String.format("Refusing %s as the current node count already matches the node count of the requested availability type. Current " +
                        "node count: %d, target availability type: %s and node count: %d.", scaleType.name(), allInstances.size(),
                scalingPath.getTargetAvailabilityType(), scalingPath.getTargetAvailabilityType().getInstanceCount());
        LOGGER.warn(message);
        throw new BadRequestException(message);
    }

    private void throwErrorForUnsupportedScalingPath(ScalingPath scalingPath, OperationType scaleType) {
        String message = String.format("Refusing %s as scaling from %s node to %s is not supported.%s",
                scaleType.getLowerCaseName(),
                scalingPath.getOriginalAvailabilityType().getInstanceCount(),
                scalingPath.getTargetAvailabilityType().getInstanceCount(),
                generateAlternativeTargetString(scaleType.getLowerCaseName(), allowedScalingPaths.getPaths().get(scalingPath.getOriginalAvailabilityType())));
        LOGGER.warn(message);
        throw new BadRequestException(message);
    }

    private void throwErrorForEmptyDownscaleCandidates() {
        String message = String.format("Refusing %s as you specified an empty list of downscale candidates. Please specify at least one instance " +
                        "id to downscale", OperationType.DOWNSCALE.getLowerCaseName());
        LOGGER.warn(message);
        throw new BadRequestException(message);
    }

    private void throwErrorForUnknownDownscaleCandidates(Set<String> unknownInstanceIds) {
        String message = String.format("Refusing %s as some of the selected instance ids are not part of the cluster. Unknown instance ids: %s.",
                OperationType.DOWNSCALE.getLowerCaseName(), unknownInstanceIds);
        LOGGER.warn(message);
        throw new BadRequestException(message);
    }

    private String generateAlternativeTargetString(String scaleType, List<AvailabilityType> targets) {
        return Objects.isNull(targets) ? "" : String.format(" Supported %s targets: %s", scaleType, targets);
    }

    private long calculateNumberOfZonesAfterDownscale(ScalingPath scalingPath, Set<InstanceMetaData> allInstances, Set<String> instanceIdsToDelete) {
        if (CollectionUtils.isNotEmpty(instanceIdsToDelete)) {
            LOGGER.debug("Counting number of nodes after downscale based on instance ids");
            return allInstances.stream().filter(instance -> !instanceIdsToDelete.contains(instance.getInstanceId()))
                    .map(InstanceMetaData::getAvailabilityZone)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
        } else {
            LOGGER.debug("Counting number of nodes after downscale based on target availability type");
            return scalingPath.getTargetAvailabilityType().getInstanceCount();
        }
    }
}

