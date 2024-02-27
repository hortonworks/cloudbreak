package com.sequenceiq.cloudbreak.service;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterCache;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.multiaz.MultiAzCalculatorService;
import com.sequenceiq.cloudbreak.service.multiaz.ProviderBasedMultiAzSetupValidator;
import com.sequenceiq.cloudbreak.service.verticalscale.VerticalScaleInstanceProvider;
import com.sequenceiq.common.api.type.CdpResourceType;

@Service
public class VerticalScalingValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerticalScalingValidatorService.class);

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
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private MultiAzCalculatorService multiAzCalculatorService;

    @Inject
    private ProviderBasedMultiAzSetupValidator providerBasedMultiAzSetupValidator;

    @Inject
    private EntitlementService entitlementService;

    public void validateProviderForDelete(Stack stack, String message, boolean checkStackStopped) {
        if (!cloudParameterCache.isDeleteVolumesSupported(stack.getCloudPlatform())) {
            throw new BadRequestException(String.format("%s is not supported on %s cloudplatform", message, stack.getCloudPlatform()));
        }
        if (!stack.isStopped() && checkStackStopped) {
            throw new BadRequestException(String.format("You must stop %s to be able to vertically scale it.", stack.getName()));
        }
    }

    public void validateProviderForAddVolumes(Stack stack, String message) {
        if (!cloudParameterCache.isAddVolumesSupported(stack.getCloudPlatform())) {
            throw new BadRequestException(String.format("%s is not supported on %s cloudplatform", message, stack.getCloudPlatform()));
        }
    }

    public void validateProvider(Stack stack, String message, StackVerticalScaleV4Request verticalScaleV4Request) {
        if (!cloudParameterCache.isVerticalScalingSupported(stack.getCloudPlatform())) {
            throw new BadRequestException(String.format("%s is not supported on %s cloudplatform", message, stack.getCloudPlatform()));
        }
        if (!stack.isStopped()) {
            throw new BadRequestException(String.format("You must stop %s to be able to vertically scale it.", stack.getName()));
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
            boolean validateMultiAz = stack.isMultiAz() && providerBasedMultiAzSetupValidator.getAvailabilityZoneConnector(stack) != null;
            String availabilityZone = stack.getAvailabilityZone();
            String region = stack.getRegion();
            String currentInstanceType = instanceGroupOptional.get().getTemplate().getInstanceType();
            Credential credential = credentialService.getByEnvironmentCrn(stack.getEnvironmentCrn());
            ExtendedCloudCredential cloudCredential = credentialToExtendedCloudCredentialConverter.convert(credential);
            Json attributes = instanceGroupOptional.get().getTemplate().getAttributes();
            CloudVmTypes allVmTypes = cloudParameterService.getVmTypesV2(
                    cloudCredential,
                    stack.getRegion(),
                    stack.getPlatformVariant(),
                    CdpResourceType.DEFAULT,
                    Maps.newHashMap());
            verticalScaleInstanceProvider.validateInstanceTypeForVerticalScaling(
                    getInstance(region, availabilityZone, currentInstanceType, allVmTypes),
                    getInstance(region, availabilityZone, requestedInstanceType, allVmTypes),
                    validateMultiAz ? instanceGroupOptional.get().getAvailabilityZones() : null,
                    attributes == null ? Map.of() : attributes.getMap()
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

    public void validateInstanceTypeForDeletingDisks(Stack stack, StackDeleteVolumesRequest deleteRequest) {
        Optional<InstanceGroup> instanceGroupOptional = stack.getInstanceGroups()
                .stream()
                .filter(e -> e.getGroupName().equals(deleteRequest.getGroup()))
                .findFirst();
        if (instanceGroupOptional.isPresent()) {
            Template template = instanceGroupOptional.get().getTemplate();
            if (null == template.getInstanceStorageCount() || template.getInstanceStorageCount() == 0) {
                throw new BadRequestException("Deleting disks is only supported on instances with instance storage");
            }
        } else {
            throw new BadRequestException(String.format("Define a group which exists in Cluster. It can be [%s].",
                    stack.getInstanceGroups()
                            .stream()
                            .map(InstanceGroup::getGroupName)
                            .collect(Collectors.joining(", ")))
            );
        }
    }

    public void validateEntitlementForDelete(Stack stack) {
        if (CloudPlatform.valueOf(stack.getCloudPlatform()) == CloudPlatform.AZURE
                && !entitlementService.azureDeleteDiskEnabled(Crn.safeFromString(stack.getResourceCrn()).getAccountId())) {
            throw new BadRequestException("Deleting Disk for Azure is not enabled for this account");
        }
    }

    public void validateEntitlementForAddVolumes(Stack stack) {
        if (CloudPlatform.valueOf(stack.getCloudPlatform()) == CloudPlatform.AZURE
                && !entitlementService.azureAddDiskEnabled(Crn.safeFromString(stack.getResourceCrn()).getAccountId())) {
            throw new BadRequestException("Adding Disk for Azure is not enabled for this account");
        }
    }
}
