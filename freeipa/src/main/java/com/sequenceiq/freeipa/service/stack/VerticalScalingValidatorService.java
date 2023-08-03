package com.sequenceiq.freeipa.service.stack;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.verticalscale.VerticalScaleInstanceProvider;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.multiaz.MultiAzCalculatorService;

@Service
public class VerticalScalingValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerticalScalingValidatorService.class);

    @Value("${freeipa.verticalScalingSupported}")
    private Set<String> verticalScalingSupported;

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private CredentialToExtendedCloudCredentialConverter credentialToExtendedCloudCredentialConverter;

    @Inject
    private CredentialService credentialService;

    @Inject
    private VerticalScaleInstanceProvider verticalScaleInstanceProvider;

    @Inject
    private MultiAzCalculatorService multiAzCalculatorService;

    public void validateRequest(Stack stack, VerticalScaleRequest verticalScaleV4Request) {
        if (!verticalScalingSupported.contains(stack.getCloudPlatform())) {
            throw new BadRequestException(String.format("Vertical scaling is not supported on %s cloud platform", stack.getCloudPlatform()));
        }
        if (!stack.isStopped()) {
            throw new BadRequestException("You must stop FreeIPA to be able to vertically scale it.");
        }
        if (verticalScaleV4Request.getTemplate() == null) {
            throw new BadRequestException(String.format("Define an exiting instancetype to vertically scale the %s FreeIpa.", stack.getCloudPlatform()));
        }
        if (verticalScaleV4Request.getTemplate().getInstanceType() == null) {
            throw new BadRequestException(String.format("Define an exiting instancetype to vertically scale the %s FreeIpa.", stack.getCloudPlatform()));
        } else {
            validateInstanceType(stack, verticalScaleV4Request);
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
            String region = stack.getRegion();
            String currentInstanceType = instanceGroupOptional.get().getTemplate().getInstanceType();
            Credential credential = credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn());
            ExtendedCloudCredential cloudCredential = credentialToExtendedCloudCredentialConverter.convert(credential);
            CloudVmTypes allVmTypes = cloudParameterService.getVmTypesV2(
                    cloudCredential,
                    stack.getRegion(),
                    stack.getPlatformvariant(),
                    CdpResourceType.DEFAULT,
                    Maps.newHashMap());
            Optional<VmType> requestInstanceForVerticalScaling = getInstance(region, availabilityZone, requestedInstanceType, allVmTypes);
            verticalScaleInstanceProvider.validInstanceTypeForVerticalScaling(
                    getInstance(region, availabilityZone, currentInstanceType, allVmTypes), requestInstanceForVerticalScaling);
            validateInstanceForMultiAz(stack, instanceGroupOptional.get(), requestInstanceForVerticalScaling.get());
        } else {
            throw new BadRequestException(String.format("Define a group which exists in FreeIpa. It can be [%s].",
                    stack.getInstanceGroups()
                            .stream()
                            .map(e -> e.getGroupName())
                            .collect(Collectors.joining(", ")))
            );
        }
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

    private boolean anyAttachedVolumePropertyDefinedInVerticalScalingRequest(VerticalScaleRequest verticalScaleV4Request) {
        return verticalScaleV4Request.getTemplate().getAttachedVolumes() != null
                && !verticalScaleV4Request.getTemplate().getAttachedVolumes().isEmpty();
    }

    private void validateInstanceForMultiAz(Stack stack, InstanceGroup instanceGroup, VmType vmType) {
        if (stack.isMultiAz()) {
            if (multiAzCalculatorService.getAvailabilityZoneConnector(stack) != null) {
                LOGGER.debug("MultiAz is enabled so validating vertical scaling request for MultiAz");
                validateInstanceSupportsExistingZones(instanceGroup.getAvailabilityZones(), vmType.getMetaData().getAvailabilityZones(), vmType.value());
            } else {
                LOGGER.debug("Implementation for AvailabilityZoneConnector is not present for CloudPlatform {} and PlatformVariant {}",
                        stack.getCloudPlatform(), stack.getPlatformvariant());
            }
        } else {
            LOGGER.debug("MultiAz is not enabled so skipping validations for MultiAz");
        }
    }

    private String convertCollectionToString(Collection<String> c) {
        return CollectionUtils.isEmpty(c) ? "" : c.stream().sorted().collect(Collectors.joining(","));
    }

    private void validateInstanceSupportsExistingZones(Set<String> instanceGroupZones, List<String> availabilityZonesForVm, String instanceType) {
        if (!emptyIfNull(availabilityZonesForVm).containsAll(emptyIfNull(instanceGroupZones))) {
            String errorMsg = String.format("FreeIpa is MultiAz enabled but requested instance type is not supported in existing " +
                            "Availability Zones for Instance Group. Supported Availability Zones for Instance type %s : %s. " +
                            "Existing Availability Zones for " +
                            "Instance Group : %s", instanceType, convertCollectionToString(availabilityZonesForVm),
                    convertCollectionToString(instanceGroupZones));
            LOGGER.error(errorMsg);
            throw new BadRequestException(errorMsg);
        }
    }
}
