package com.sequenceiq.cloudbreak.service.verticalscale;

import static com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.ENHANCED_NETWORK;
import static com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.HOST_ENCRYPTION_SUPPORTED;
import static com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.RESOURCE_DISK_ATTACHED;
import static com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate.ENCRYPTION_AT_HOST_ENABLED;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.filter.MinimalHardwareFilter;

@Service
public class VerticalScaleInstanceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerticalScaleInstanceProvider.class);

    @Inject
    private MinimalHardwareFilter minimalHardwareFilter;

    public CloudVmTypes listInstanceTypes(String availabilityZone, String currentInstanceType, CloudVmTypes allVmTypes) {
        return listInstanceTypes(availabilityZone, currentInstanceType, allVmTypes, null);
    }

    public CloudVmTypes listInstanceTypes(String availabilityZone, String currentInstanceType, CloudVmTypes allVmTypes,
            Set<String> instanceGroupAvailabilityZones) {
        Map<String, Set<VmType>> cloudVmResponses = allVmTypes.getCloudVmResponses();
        LOGGER.debug("cloudVmResponses: {}", cloudVmResponses);
        if (cloudVmResponses.isEmpty()) {
            LOGGER.warn("Empty cloudVmResponses.");
            return new CloudVmTypes(Map.of(), Map.of());
        }

        String availabilityZoneForSelection = getAvailabilityZone(availabilityZone, cloudVmResponses);
        LOGGER.debug("availabilityZoneForSelection: {}", availabilityZoneForSelection);
        Set<VmType> vmTypes = cloudVmResponses.get(availabilityZoneForSelection);
        if (vmTypes == null) {
            LOGGER.warn("Invalid availabilityZoneForSelection; no corresponding key found in cloudVmResponses.");
            return new CloudVmTypes(Map.of(availabilityZoneForSelection, Set.of()), Map.of());
        }
        Optional<VmType> currentInstance = getInstance(currentInstanceType, vmTypes);
        LOGGER.debug("currentInstance: {}", currentInstance);
        Set<VmType> suitableInstances = vmTypes
                .stream()
                .filter(availableVmType -> {
                    try {
                        validateInstanceType(currentInstance, Optional.of(availableVmType), instanceGroupAvailabilityZones, Map.of());
                        return true;
                    } catch (BadRequestException ex) {
                        return false;
                    }
                })
                .collect(Collectors.toSet());
        LOGGER.debug("suitableInstances: {}", suitableInstances);
        if (suitableInstances.isEmpty()) {
            LOGGER.warn("Empty suitableInstances.");
            return new CloudVmTypes(Map.of(availabilityZoneForSelection, Set.of()), Map.of());
        }

        return new CloudVmTypes(
                Map.of(availabilityZoneForSelection, suitableInstances),
                Map.of(availabilityZoneForSelection, suitableInstances.stream().findFirst().get())
        );
    }

    public void validateInstanceTypeForVerticalScaling(Optional<VmType> currentInstanceTypeOptional, Optional<VmType> requestedInstanceTypeOptional,
        Set<String> instanceGroupAvailabilityZones, Map<String, Object> additionalProperties) {
        try {
            validateInstanceType(
                    currentInstanceTypeOptional,
                    requestedInstanceTypeOptional,
                    instanceGroupAvailabilityZones,
                    additionalProperties);
        } catch (Exception e) {
            LOGGER.warn("Vertical scale validation error: {}", e.getMessage());
            throw e;
        }
    }

    private void validateInstanceType(Optional<VmType> currentInstanceTypeOptional, Optional<VmType> requestedInstanceTypeOptional,
        Set<String> instanceGroupAvailabilityZones, Map<String, Object> additionalProperties) {
        if (currentInstanceTypeOptional.isEmpty()) {
            throw new BadRequestException("The current instancetype does not exist on provider side.");
        }

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
            validateResourceDisk(currentInstanceTypeMetaData, requestedInstanceTypeMetaData);
            validateHostEncryption(currentInstanceType, requestedInstanceType, additionalProperties);
            validateEnhancedNetwork(currentInstanceType, requestedInstanceType);
            if (instanceGroupAvailabilityZones != null) {
                validateInstanceSupportsExistingZones(instanceGroupAvailabilityZones, requestedInstanceTypeMetaData.getAvailabilityZones(),
                        requestedInstanceTypeName);
            }
        } else {
            throw new BadRequestException("The requested instancetype does not exist on provider side.");
        }
    }

    private void validateHostEncryption(VmType currentVmType, VmType requestedVmType, Map<String, Object> additionalProperties) {
        Boolean currentHostEncryptionActivated = (Boolean) additionalProperties.get(ENCRYPTION_AT_HOST_ENABLED);
        Boolean requestedHostEncryptionSupported = (Boolean) requestedVmType.getMetaData().getProperties().get(HOST_ENCRYPTION_SUPPORTED);

        if (currentHostEncryptionActivated != null && requestedHostEncryptionSupported != null && currentHostEncryptionActivated) {
            if (!requestedHostEncryptionSupported) {
                throw new BadRequestException("Unable to resize since changing from host encrypted "
                        + currentVmType.getValue() + " instance type to " + requestedVmType.getValue()
                        + " instance type which does not support host encryption.");
            }
        }
    }

    private void validateEnhancedNetwork(VmType currentVmType, VmType requestedVmType) {
        Boolean currentEnhancedNetworkActivated = (Boolean) currentVmType.getMetaData().getProperties().get(ENHANCED_NETWORK);
        Boolean requestedEnhancedNetworkSupported = (Boolean) requestedVmType.getMetaData().getProperties().get(ENHANCED_NETWORK);

        if (currentEnhancedNetworkActivated != null && requestedEnhancedNetworkSupported != null && currentEnhancedNetworkActivated) {
            if (!requestedEnhancedNetworkSupported) {
                throw new BadRequestException("Unable to resize since changing from enhanced network supported "
                        + currentVmType.getValue() + " instance type to " + requestedVmType.getValue()
                        + " instance type which does not support enhanced network.");
            }
        }
    }

    private void validateResourceDisk(VmTypeMeta currentInstanceTypeMetaData, VmTypeMeta requestedInstanceTypeMetaData) {
        Object currentInstanceTypeResourceDisk = currentInstanceTypeMetaData.getProperties().get(RESOURCE_DISK_ATTACHED);
        Object requestedInstanceTypeResourceDisk = requestedInstanceTypeMetaData.getProperties().get(RESOURCE_DISK_ATTACHED);

        if (currentInstanceTypeResourceDisk != null && requestedInstanceTypeResourceDisk != null) {
            Boolean currentInstanceTypeResourceDiskBoolean = Boolean.valueOf(currentInstanceTypeResourceDisk.toString());
            Boolean requestedInstanceTypeResourceDiskBoolean = Boolean.valueOf(requestedInstanceTypeResourceDisk.toString());
            if (currentInstanceTypeResourceDiskBoolean != requestedInstanceTypeResourceDiskBoolean) {
                throw new BadRequestException("Unable to resize since changing from resource disk to non-resource " +
                        "disk VM size and vice-versa is not allowed. " +
                        "Please refer to https://aka.ms/AAah4sj for more details.");
            }
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
                        "The current instancetype %s has more %s Disk than the requested %s.",
                        currentInstanceTypeName, type, requestedInstanceTypeName));
            }
            if (currentVolumeParameterConfig.maximumNumber()
                    > requestedVolumeParameterConfig.maximumNumber()) {
                throw new BadRequestException(String.format(
                        "The current instancetype %s has more %s Disk than the requested %s.",
                        currentInstanceTypeName, type, requestedInstanceTypeName));
            }
            if (currentVolumeParameterConfig.minimumNumber()
                    > requestedVolumeParameterConfig.minimumNumber()) {
                throw new BadRequestException(String.format(
                        "The current instancetype %s has more %s Disk than the requested %s.",
                        currentInstanceTypeName, type, requestedInstanceTypeName));
            }
        }
    }

    private void validateMemory(String requestedInstanceTypeName, VmTypeMeta requestedInstanceTypeMetaData) {
        if (!minimalHardwareFilter.suitableAsMinimumHardwareForMemory(requestedInstanceTypeMetaData.getMemoryInGb())) {
            throw new BadRequestException(String.format(
                    "The requested instancetype %s has less Memory than the minimum %s GB.",
                    requestedInstanceTypeName, minimalHardwareFilter.minMemory()));
        }
    }

    private void validateCPU(String requestedInstanceTypeName, VmTypeMeta requestedInstanceTypeMetaData) {
        if (!minimalHardwareFilter.suitableAsMinimumHardwareForCpu(requestedInstanceTypeMetaData.getCPU())) {
            throw new BadRequestException(String.format(
                    "The requested instancetype %s has less Cpu than the minimum %s core.",
                    requestedInstanceTypeName, minimalHardwareFilter.minCpu()));
        }
    }

    private Optional<VmType> getInstance(String currentInstanceType, Set<VmType> vmTypes) {
        return vmTypes
                .stream()
                .filter(e -> e.getValue().equals(currentInstanceType))
                .findFirst();
    }

    public String getAvailabilityZone(String availabilityZone, Map<String, Set<VmType>> cloudVmResponses) {
        availabilityZone = availabilityZone == null || availabilityZone.isEmpty() || availabilityZone.isBlank() ?
                cloudVmResponses.keySet().stream().findFirst().get() : availabilityZone;
        return availabilityZone;
    }

    private void validateInstanceSupportsExistingZones(Set<String> instanceGroupZones, List<String> requestedAvailabilityZonesForVm, String instanceType) {
        if (!emptyIfNull(requestedAvailabilityZonesForVm).containsAll(emptyIfNull(instanceGroupZones))) {
            String errorMsg = String.format("Stack is MultiAz enabled but requested instance type is not supported in existing " +
                            "Availability Zones for Instance Group. Supported Availability Zones for Instance type %s : %s. " +
                            "Existing Availability Zones for Instance Group : %s",
                    instanceType, convertCollectionToString(requestedAvailabilityZonesForVm), convertCollectionToString(instanceGroupZones));
            throw new BadRequestException(errorMsg);
        }
    }

    private String convertCollectionToString(Collection<String> collection) {
        return CollectionUtils.isEmpty(collection) ? "" : collection.stream().sorted().collect(Collectors.joining(","));
    }

}
