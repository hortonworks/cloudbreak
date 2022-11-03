package com.sequenceiq.cloudbreak.service;

import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.verticalscale.VerticalScaleInstanceProvider;
import com.sequenceiq.common.api.type.CdpResourceType;

@Service
public class VerticalScalingValidatorService {

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private CredentialToExtendedCloudCredentialConverter credentialToExtendedCloudCredentialConverter;

    @Inject
    private CredentialClientService credentialService;

    @Inject
    private VerticalScaleInstanceProvider verticalScaleInstanceProvider;

    @Inject
    private CloudParameterCache cloudParameterCache;

    public void validateProvider(Stack stack) {
        if (!cloudParameterCache.isVerticalScalingSupported(stack.getCloudPlatform())) {
            throw new BadRequestException(String.format("Vertical scaling is not supported on %s cloudplatform", stack.getCloudPlatform()));
        }
    }

    public void validateRequest(Stack stack, StackVerticalScaleV4Request verticalScaleV4Request) {
        if (verticalScaleV4Request.getTemplate() == null) {
            throw new BadRequestException(String.format("Define an exiting instancetype to vertically scale the %s Data Hubs.", stack.getCloudPlatform()));
        }
        if (verticalScaleV4Request.getTemplate().getInstanceType() == null) {
            throw new BadRequestException(String.format("Define an exiting instancetype to vertically scale the %s Data Hubs.", stack.getCloudPlatform()));
        }
        if (anyAttachedVolumePropertyDefinedInVerticalScalingRequest(verticalScaleV4Request)) {
            throw new BadRequestException(String.format("Only instance type modification is supported on %s Data Hubs.", stack.getCloudPlatform()));
        }
    }

    public void validateInstanceType(Stack stack, StackVerticalScaleV4Request verticalScaleV4Request) {
        String group = verticalScaleV4Request.getGroup();
        Optional<InstanceGroup> instanceGroupOptional = stack.getInstanceGroups()
                .stream()
                .filter(e -> e.getGroupName().equals(group))
                .findFirst();
        String requestedInstanceType = verticalScaleV4Request.getTemplate().getInstanceType();
        if (instanceGroupOptional.isPresent()) {
            String availabilityZone = stack.getAvailabilityZone();
            String currentInstanceType = instanceGroupOptional.get().getTemplate().getInstanceType();
            Credential credential = credentialService.getByEnvironmentCrn(stack.getEnvironmentCrn());
            ExtendedCloudCredential cloudCredential = credentialToExtendedCloudCredentialConverter.convert(credential);
            CloudVmTypes allVmTypes = cloudParameterService.getVmTypesV2(
                    cloudCredential,
                    stack.getRegion(),
                    stack.getPlatformVariant(),
                    CdpResourceType.DEFAULT,
                    Maps.newHashMap());
            verticalScaleInstanceProvider.validInstanceTypeForVerticalScaling(
                    getInstance(availabilityZone, currentInstanceType, allVmTypes),
                    getInstance(availabilityZone, requestedInstanceType, allVmTypes)
            );
        } else {
            throw new BadRequestException(String.format("Define a group which exists in Cluster. It can be [%s].",
                    stack.getInstanceGroups()
                            .stream()
                            .map(e -> e.getGroupName())
                            .collect(Collectors.joining(", ")))
            );
        }
    }

    private boolean anyAttachedVolumePropertyDefinedInVerticalScalingRequest(StackVerticalScaleV4Request verticalScaleV4Request) {
        return verticalScaleV4Request.getTemplate().getEphemeralVolume() != null
                || verticalScaleV4Request.getTemplate().getRootVolume() != null
                || (verticalScaleV4Request.getTemplate().getAttachedVolumes() != null && !verticalScaleV4Request.getTemplate().getAttachedVolumes().isEmpty())
                || verticalScaleV4Request.getTemplate().getTemporaryStorage() != null;
    }

    private Optional<VmType> getInstance(String availabilityZone, String currentInstanceType, CloudVmTypes allVmTypes) {
        return allVmTypes.getCloudVmResponses().get(availabilityZone)
                .stream()
                .filter(e -> e.getValue().equals(currentInstanceType))
                .findFirst();
    }
}
