package com.sequenceiq.cloudbreak.service.verticalscale;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.filter.MinimalHardwareFilter;

@Service
public class VerticalScaleInstanceProvider {

    @Inject
    private MinimalHardwareFilter minimalHardwareFilter;

    public CloudVmTypes listInstanceTypes(String availabilityZone, String currentInstanceType, CloudVmTypes allVmTypes) {
        String availabilityZoneForSelection = getAvailabilityZone(availabilityZone, allVmTypes);
        Optional<VmType> currentInstance = getInstance(availabilityZoneForSelection, currentInstanceType, allVmTypes);
        Set<VmType> collect = allVmTypes.getCloudVmResponses().get(availabilityZoneForSelection)
                .stream()
                .filter(e -> {
                    try {
                        validInstanceTypeForVerticalScaling(currentInstance, Optional.of(e));
                        return true;
                    } catch (BadRequestException ex) {
                        return false;
                    }
                })
                .collect(Collectors.toSet());
        CloudVmTypes cloudVmTypes  = new CloudVmTypes(
                Map.of(availabilityZoneForSelection, collect),
                Map.of(availabilityZoneForSelection, collect.stream().findFirst().get())
        );
        return cloudVmTypes;
    }

    public void validInstanceTypeForVerticalScaling(Optional<VmType> currentInstanceTypeOptional, Optional<VmType> requestedInstanceTypeOptional) {
        if (requestedInstanceTypeOptional.isPresent()) {
            VmType currentInstanceType = currentInstanceTypeOptional.get();
            VmType requestedInstanceType = requestedInstanceTypeOptional.get();
            String currentInstanceTypeName = currentInstanceType.getValue();
            String requestedInstanceTypeName = requestedInstanceType.getValue();
            VmTypeMeta currentInstanceTypeMetaData = currentInstanceType.getMetaData();
            VmTypeMeta requestedInstanceTypeMetaData = requestedInstanceType.getMetaData();

            validateCPU(requestedInstanceTypeName, requestedInstanceTypeMetaData);
            validateMemory(requestedInstanceTypeName, requestedInstanceTypeMetaData);
            validateEphemeral(currentInstanceTypeName, requestedInstanceTypeName,
                    currentInstanceTypeMetaData, requestedInstanceTypeMetaData);
            validateAutoAttached(currentInstanceTypeName, requestedInstanceTypeName,
                    currentInstanceTypeMetaData, requestedInstanceTypeMetaData);
        } else {
            throw new BadRequestException(String.format(
                    "The requested instancetype does not exist on provider side."));
        }
    }

    private void validateAutoAttached(String currentInstanceTypeName, String requestedInstanceTypeName,
        VmTypeMeta currentInstanceTypeMetaData, VmTypeMeta requestedInstanceTypeMetaData) {
        validateVolumeParameterConfig(
                currentInstanceTypeName,
                requestedInstanceTypeName,
                currentInstanceTypeMetaData.getAutoAttachedConfig(),
                requestedInstanceTypeMetaData.getAutoAttachedConfig(),
                "Auto Attached");
    }

    private void validateEphemeral(String currentInstanceTypeName, String requestedInstanceTypeName,
        VmTypeMeta currentInstanceTypeMetaData, VmTypeMeta requestedInstanceTypeMetaData) {
        validateVolumeParameterConfig(
                currentInstanceTypeName,
                requestedInstanceTypeName,
                currentInstanceTypeMetaData.getEphemeralConfig(),
                requestedInstanceTypeMetaData.getEphemeralConfig(),
                "Ephemeral");
    }

    private void validateVolumeParameterConfig(String currentInstanceTypeName, String requestedInstanceTypeName,
        VolumeParameterConfig currentVolumeParameterConfig, VolumeParameterConfig requestedVolumeParameterConfig, String type) {
        if (currentVolumeParameterConfig != null) {
            if (requestedVolumeParameterConfig == null) {
                throw new BadRequestException(String.format(
                        "The current instancetype %s has more %s Disk then the requested %s.",
                        currentInstanceTypeName, type, requestedInstanceTypeName));
            }
            if (currentVolumeParameterConfig.maximumNumber()
                    > requestedVolumeParameterConfig.maximumNumber()) {
                throw new BadRequestException(String.format(
                        "The current instancetype %s has more %s Disk then the requested %s.",
                        currentInstanceTypeName, type, requestedInstanceTypeName));
            }
            if (currentVolumeParameterConfig.minimumNumber()
                    > requestedVolumeParameterConfig.minimumNumber()) {
                throw new BadRequestException(String.format(
                        "The current instancetype %s has more %s Disk then the requested %s.",
                        currentInstanceTypeName, type, requestedInstanceTypeName));
            }
        }
    }

    private void validateMemory(String requestedInstanceTypeName, VmTypeMeta requestedInstanceTypeMetaData) {
        if (!minimalHardwareFilter.suitableAsMinimumHardwareForMemory(requestedInstanceTypeMetaData.getMemoryInGb())) {
            throw new BadRequestException(String.format(
                    "The requested instancetype %s has less Memory then the minimum %s GB.",
                    requestedInstanceTypeName, minimalHardwareFilter.minMemory()));
        }
    }

    private void validateCPU(String requestedInstanceTypeName, VmTypeMeta requestedInstanceTypeMetaData) {
        if (!minimalHardwareFilter.suitableAsMinimumHardwareForCpu(requestedInstanceTypeMetaData.getCPU())) {
            throw new BadRequestException(String.format(
                    "The requested instancetype %s has less Cpu then the minimum %s core.",
                    requestedInstanceTypeName, minimalHardwareFilter.minCpu()));
        }
    }

    private Optional<VmType> getInstance(String availabilityZone, String currentInstanceType, CloudVmTypes allVmTypes) {
        return allVmTypes.getCloudVmResponses().get(availabilityZone)
                .stream()
                .filter(e -> e.getValue().equals(currentInstanceType))
                .findFirst();
    }

    private String getAvailabilityZone(String availabilityZone, CloudVmTypes allVmTypes) {
        availabilityZone = availabilityZone == null || availabilityZone.isEmpty() || availabilityZone.isBlank() ?
                allVmTypes.getCloudVmResponses().keySet().stream().findFirst().get() : availabilityZone;
        return availabilityZone;
    }
}
