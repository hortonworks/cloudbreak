package com.sequenceiq.freeipa.service.stack;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;

@Service
public class VerticalScalingValidatorService {

    @Value("${freeipa.verticalScalingSupported}")
    private Set<String> verticalScalingSupported;

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private CredentialToExtendedCloudCredentialConverter credentialToExtendedCloudCredentialConverter;

    @Inject
    private CredentialService credentialService;

    public void validateStatus(Stack stack) {
        if (!stack.isStopped()) {
            throw new BadRequestException("Vertical scaling currently only available for FreeIPA when it is stopped");
        }
    }

    public void validateRequest(Stack stack, VerticalScaleRequest verticalScaleV4Request) {
        if (!verticalScalingSupported.contains(stack.getCloudPlatform())) {
            throw new BadRequestException(String.format("Vertical scaling is not supported on %s cloud platform", stack.getCloudPlatform()));
        }
        if (verticalScaleV4Request.getTemplate() == null) {
            throw new BadRequestException(String.format("Define an exiting instancetype to vertically scale the %s FreeIpa.", stack.getCloudPlatform()));
        }
        if (verticalScaleV4Request.getTemplate().getInstanceType() == null) {
            throw new BadRequestException(String.format("Define an exiting instancetype to vertically scale the %s FreeIpa.", stack.getCloudPlatform()));
        } else {
            //validateInstanceType(stack, verticalScaleV4Request);
        }
        if (anyAttachedVolumePropertyDefinedInVerticalScalingRequest(verticalScaleV4Request)) {
            throw new BadRequestException(String.format("Only instance type modification is supported on %s FreeIpa.", stack.getCloudPlatform()));
        }
    }

    private void validateInstanceType(Stack stack, VerticalScaleRequest verticalScaleV4Request) {
        String group = verticalScaleV4Request.getGroup();
        Optional<InstanceGroup> instanceGroupOptional = stack.getInstanceGroups()
                .stream()
                .filter(e -> e.getGroupName().equals(group))
                .findFirst();
        String requestedInstanceType = verticalScaleV4Request.getTemplate().getInstanceType();
        if (instanceGroupOptional.isPresent()) {
            String availabilityZone = stack.getAvailabilityZone();
            String currentInstanceType = instanceGroupOptional.get().getTemplate().getInstanceType();
            Credential credential = credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn());
            ExtendedCloudCredential cloudCredential = credentialToExtendedCloudCredentialConverter.convert(credential);
            CloudVmTypes allVmTypes = cloudParameterService.getVmTypesV2(
                    cloudCredential,
                    stack.getRegion(),
                    stack.getPlatformvariant(),
                    CdpResourceType.DEFAULT,
                    Maps.newHashMap());
            validInstanceTypeForVerticalScaling(
                    getInstance(availabilityZone, currentInstanceType, allVmTypes),
                    getInstance(availabilityZone, requestedInstanceType, allVmTypes)
            );
        } else {
            throw new BadRequestException(String.format("Define a group which exists in FreeIpa. It can be [%s].",
                    stack.getInstanceGroups()
                            .stream()
                            .map(e -> e.getGroupName())
                            .collect(Collectors.joining(", ")))
            );
        }
    }

    private Optional<VmType> getInstance(String availabilityZone, String currentInstanceType, CloudVmTypes allVmTypes) {
        return allVmTypes.getCloudVmResponses().get(availabilityZone)
                .stream()
                .filter(e -> e.getValue().equals(currentInstanceType))
                .findFirst();
    }

    private void validInstanceTypeForVerticalScaling(Optional<VmType> currentInstanceTypeOptional, Optional<VmType> requestedInstanceTypeOptional) {
        if (requestedInstanceTypeOptional.isPresent()) {
            VmType currentInstanceType = currentInstanceTypeOptional.get();
            VmType requestedInstanceType = requestedInstanceTypeOptional.get();
            String currentInstanceTypeName = currentInstanceType.getValue();
            String requestedInstanceTypeName = requestedInstanceType.getValue();
            VmTypeMeta currentInstanceTypeMetaData = currentInstanceType.getMetaData();
            VmTypeMeta requestedInstanceTypeMetaData = requestedInstanceType.getMetaData();

            if (currentInstanceTypeMetaData.getCPU() > requestedInstanceTypeMetaData.getCPU()) {
                throw new BadRequestException(String.format(
                        "The current instancetype %s has more CPU then the requested %s.",
                        currentInstanceTypeName, requestedInstanceTypeName));
            }
            if (currentInstanceTypeMetaData.getMemoryInGb() > requestedInstanceTypeMetaData.getMemoryInGb()) {
                throw new BadRequestException(String.format(
                        "The current instancetype %s has more Memory then the requested %s.",
                        currentInstanceTypeName, requestedInstanceTypeName));
            }
            if (currentInstanceTypeMetaData.getEphemeralConfig().maximumNumber()
                    > requestedInstanceTypeMetaData.getEphemeralConfig().maximumNumber()) {
                throw new BadRequestException(String.format(
                        "The current instancetype %s has more Ephemeral Disk then the requested %s.",
                        currentInstanceTypeName, requestedInstanceTypeName));
            }
            if (currentInstanceTypeMetaData.getEphemeralConfig().minimumNumber()
                    > requestedInstanceTypeMetaData.getEphemeralConfig().minimumNumber()) {
                throw new BadRequestException(String.format(
                        "The current instancetype %s has more Ephemeral Disk then the requested %s.",
                        currentInstanceTypeName, requestedInstanceTypeName));
            }
            if (currentInstanceTypeMetaData.getAutoAttachedConfig().maximumNumber()
                    > requestedInstanceTypeMetaData.getAutoAttachedConfig().maximumNumber()) {
                throw new BadRequestException(String.format(
                        "The current instancetype %s has more Auto Attached Disk then the requested %s.",
                        currentInstanceTypeName, requestedInstanceTypeName));
            }
            if (currentInstanceTypeMetaData.getAutoAttachedConfig().minimumNumber()
                    > requestedInstanceTypeMetaData.getAutoAttachedConfig().minimumNumber()) {
                throw new BadRequestException(String.format(
                        "The current instancetype %s has more Auto Attached Disk then the requested %s.",
                        currentInstanceTypeName, requestedInstanceTypeName));
            }
        } else {
            throw new BadRequestException(String.format(
                    "The requested instancetype does not exist on provider side."));
        }
    }

    private boolean anyAttachedVolumePropertyDefinedInVerticalScalingRequest(VerticalScaleRequest verticalScaleV4Request) {
        return verticalScaleV4Request.getTemplate().getAttachedVolumes() != null
                && !verticalScaleV4Request.getTemplate().getAttachedVolumes().isEmpty();
    }
}
