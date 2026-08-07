package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.constant.AwsPlatformResourcesFilterConstants.ARCHITECTURE;
import static com.sequenceiq.common.model.Architecture.ALL_ARCHITECTURE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.verticalscale.VerticalScaleInstanceProvider;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupAvailabilityZone;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.multiaz.MultiAzCalculatorService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupAvailabilityZoneService;

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

    @Inject
    private InstanceGroupAvailabilityZoneService availabilityZoneService;

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
            InstanceGroup instanceGroup = instanceGroupOptional.get();
            String availabilityZone = stack.getAvailabilityZone();
            String region = stack.getRegion();
            String currentInstanceType = instanceGroup.getTemplate().getInstanceType();
            boolean validateMultiAz = stack.isMultiAz() && multiAzCalculatorService.getAvailabilityZoneConnector(stack) != null;
            Set<String> instanceGroupAvailabilityZones = validateMultiAz
                    ? availabilityZoneService.findAllByInstanceGroupId(instanceGroup.getId()).stream()
                    .map(InstanceGroupAvailabilityZone::getAvailabilityZone).collect(Collectors.toSet())
                    : null;
            Credential credential = credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn());
            ExtendedCloudCredential cloudCredential = credentialToExtendedCloudCredentialConverter.convert(credential);
            CloudVmTypes allVmTypes = cloudParameterService.getVmTypesV2(
                    cloudCredential,
                    stack.getRegion(),
                    stack.getPlatformvariant(),
                    CdpResourceType.FREEIPA,
                    Map.of(ARCHITECTURE, ALL_ARCHITECTURE));
            String zoneForLookup = resolveZone(availabilityZone, instanceGroupAvailabilityZones, allVmTypes.getCloudVmResponses(), region);
            Optional<VmType> requestInstanceForVerticalScaling = getInstance(zoneForLookup, requestedInstanceType, allVmTypes);
            Json attributes = instanceGroup.getTemplate().getAttributes();
            verticalScaleInstanceProvider.validateInstanceTypeForVerticalScaling(
                    stack.getCloudPlatform(),
                    List.of(getInstance(zoneForLookup, currentInstanceType, allVmTypes)),
                    List.of(requestInstanceForVerticalScaling),
                    instanceGroupAvailabilityZones,
                    attributes == null ? Map.of() : attributes.getMap(),
                    CdpResourceType.FREEIPA);
        } else {
            throw new BadRequestException(String.format("Define a group which exists in FreeIpa. It can be [%s].",
                    stack.getInstanceGroups()
                            .stream()
                            .map(e -> e.getGroupName())
                            .collect(Collectors.joining(", ")))
            );
        }
    }

    private Optional<VmType> getInstance(String zone, String instanceType, CloudVmTypes allVmTypes) {
        Set<VmType> vmTypes = allVmTypes.getCloudVmResponses().get(zone);
        if (vmTypes == null) {
            throw new BadRequestException(String.format("No VM types found for zone '%s'.", zone));
        }
        return vmTypes.stream()
                .filter(e -> e.getValue().equals(instanceType))
                .findFirst();
    }

    private String resolveZone(String stackAvailabilityZone, Set<String> instanceGroupAvailabilityZones,
            Map<String, Set<VmType>> cloudVmResponses, String region) {
        if (StringUtils.isNotBlank(stackAvailabilityZone)) {
            LOGGER.debug("Using stack-level availability zone '{}' for VM type lookup.", stackAvailabilityZone);
            return stackAvailabilityZone;
        }
        if (instanceGroupAvailabilityZones != null && !instanceGroupAvailabilityZones.isEmpty()) {
            Set<String> availableZonesFromProvider = cloudVmResponses.keySet();
            String resolvedZone = instanceGroupAvailabilityZones.stream()
                    .filter(availableZonesFromProvider::contains)
                    .findFirst()
                    .orElseGet(() -> instanceGroupAvailabilityZones.iterator().next());
            LOGGER.debug("Stack availability zone is empty, using instance group availability zone '{}' for VM type lookup.", resolvedZone);
            return resolvedZone;
        }
        String resolvedZone = cloudVmResponses.keySet().stream()
                .findFirst()
                .orElse(region);
        LOGGER.debug("Stack and instance group availability zones are empty, falling back to '{}' for VM type lookup.", resolvedZone);
        return resolvedZone;
    }

    private boolean anyAttachedVolumePropertyDefinedInVerticalScalingRequest(VerticalScaleRequest verticalScaleV4Request) {
        return verticalScaleV4Request.getTemplate().getAttachedVolumes() != null
                && !verticalScaleV4Request.getTemplate().getAttachedVolumes().isEmpty();
    }
}
