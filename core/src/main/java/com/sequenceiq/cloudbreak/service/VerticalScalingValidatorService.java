package com.sequenceiq.cloudbreak.service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.base.Enums;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterCache;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.BlackListedLoadBasedVerticalScaleRole;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.verticalscale.VerticalScaleInstanceProvider;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
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

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    public void validateProvider(Stack stack, StackVerticalScaleV4Request verticalScaleV4Request) {
        if (!cloudParameterCache.isVerticalScalingSupported(stack.getCloudPlatform())) {
            throw new BadRequestException(String.format("Vertical scaling is not supported on %s cloudplatform", stack.getCloudPlatform()));
        }
        if (!stack.isStopped() && !validateHostGroup(stack, verticalScaleV4Request)) {
            throw new BadRequestException(String.format("You must stop %s to be able to vertically scale it.", stack.getName()));
        }
    }

    public boolean validateHostGroup(Stack stack, StackVerticalScaleV4Request stackVerticalScaleV4Request) {
        String blueprintText = stack.getBlueprint().getBlueprintText();
        BlueprintTextProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
        Set<String> hostTemplateComponents = processor.getComponentsInHostGroup(stackVerticalScaleV4Request.getGroup());
        for (String service : hostTemplateComponents) {
            com.google.common.base.Optional<BlackListedLoadBasedVerticalScaleRole> enumValue =
                    Enums.getIfPresent(BlackListedLoadBasedVerticalScaleRole.class, service);
            if (enumValue.isPresent()) {
                return false;
            }
        }
        return true;
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
            String region = stack.getRegion();
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
                    getInstance(region, availabilityZone, currentInstanceType, allVmTypes),
                    getInstance(region, availabilityZone, requestedInstanceType, allVmTypes)
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

    private Optional<VmType> getInstance(String region, String availabilityZone, String currentInstanceType, CloudVmTypes allVmTypes) {
        return allVmTypes.getCloudVmResponses().get(getZone(region, availabilityZone))
                .stream()
                .filter(e -> e.getValue().equals(currentInstanceType))
                .findFirst();
    }

    private String getZone(String region, String availabilityZone) {
        return Strings.isNullOrEmpty(availabilityZone) ? region : availabilityZone;
    }
}
