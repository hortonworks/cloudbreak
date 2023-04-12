package com.sequenceiq.freeipa.service.stack;

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.ScalingPath;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.configuration.AllowedScalingPaths;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class FreeIpaScalingValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaScalingValidationService.class);

    @Inject
    private AllowedScalingPaths allowedScalingPaths;

    @Inject
    private VerticalScalingValidatorService verticalScalingValidatorService;

    public void validateStackForUpscale(Set<InstanceMetaData> allInstances, Stack stack, ScalingPath scalingPath) {
        validateScalingIsUpscale(scalingPath);
        executeCommonValidations(allInstances, stack, scalingPath, OperationType.UPSCALE);
    }

    public void validateStackForVerticalUpscale(Stack stack, VerticalScaleRequest request) {
        verticalScalingValidatorService.validateRequest(stack, request);
    }

    public void validateStackForDownscale(Set<InstanceMetaData> allInstances, Stack stack, ScalingPath scalingPath) {
        validateScalingIsDownscale(scalingPath);
        executeCommonValidations(allInstances, stack, scalingPath, OperationType.DOWNSCALE);
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

    private String generateAlternativeTargetString(String scaleType, List<AvailabilityType> targets) {
        return Objects.isNull(targets) ? "" : String.format(" Supported %s targets: %s", scaleType, targets);
    }
}

