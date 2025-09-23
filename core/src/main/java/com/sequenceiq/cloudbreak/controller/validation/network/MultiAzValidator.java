package com.sequenceiq.cloudbreak.controller.validation.network;

import static com.sequenceiq.cloudbreak.service.multiaz.InstanceMetadataAvailabilityZoneCalculator.ZONAL_SUBNET_CLOUD_PLATFORMS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkScaleV4Request;
import com.sequenceiq.cloudbreak.cloud.AvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.multiaz.ProviderBasedMultiAzSetupValidator;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;

@Component
public class MultiAzValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzValidator.class);

    @Value("${cb.multiaz.supported.instancemetadata.platforms:YARN}")
    private Set<String> supportedInstanceMetadataPlatforms;

    @Inject
    private ProviderBasedMultiAzSetupValidator providerBasedMultiAzSetupValidator;

    @Inject
    private StackService stackService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @PostConstruct
    public void initSupportedVariants() {
        if (supportedInstanceMetadataPlatforms.isEmpty()) {
            supportedInstanceMetadataPlatforms = Set.of(
                    CloudPlatform.YARN.name());
        }
    }

    public void validateMultiAzForStack(Stack stack, ValidationResult.ValidationResultBuilder validationBuilder) {
        Set<String> allSubnetIds = collectSubnetIds(new ArrayList<>(stack.getInstanceGroups()));
        if (allSubnetIds.size() > 1 && !supportedVariant(stack)) {
            String variantIsNotSupportedMsg = String.format("Multiple subnets are not supported for this %s platform and %s variant", stack.getCloudPlatform(),
                    stack.getPlatformVariant());
            LOGGER.info(variantIsNotSupportedMsg);
            validationBuilder.error(variantIsNotSupportedMsg);
        }
        if (allSubnetIds.size() == 1 && stack.isMultiAz() && ZONAL_SUBNET_CLOUD_PLATFORMS.contains(stack.getCloudPlatform())) {
            String multiAzEnabledWithSingleSubnetError = String.format("Cannot enable multiAz for %s as only one subnetId: %s is defined for it",
                    stack.getDisplayName(), allSubnetIds);
            LOGGER.info(multiAzEnabledWithSingleSubnetError);
            validationBuilder.error(multiAzEnabledWithSingleSubnetError);
        }
        providerBasedMultiAzSetupValidator.validate(validationBuilder, stack);
    }

    public boolean supportedVariant(Stack stack) {
        Optional<AvailabilityZoneConnector> optAvailabilityZoneConnector = Optional.ofNullable(providerBasedMultiAzSetupValidator
                .getAvailabilityZoneConnector(stack));
        return !Strings.isNullOrEmpty(stack.getPlatformVariant()) && optAvailabilityZoneConnector.isPresent()
                && ZONAL_SUBNET_CLOUD_PLATFORMS.contains(stack.getCloudPlatform());
    }

    public boolean supportedForInstanceMetadataGeneration(InstanceGroupView instanceGroup) {
        if (instanceGroup.getInstanceGroupNetwork() != null) {
            return supportedInstanceMetadataPlatforms.contains(instanceGroup.getInstanceGroupNetwork().cloudPlatform());
        }
        return false;
    }

    public ValidationResult validateNetworkScaleRequest(StackDto stack, NetworkScaleV4Request networkScaleV4Request,
            String groupName) {
        ValidationResult.ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        validatePreferredAvailabilityZones(validationResultBuilder, stack, networkScaleV4Request, groupName);
        validatePreferredSubnetIds(validationResultBuilder, stack, networkScaleV4Request, groupName);
        return validationResultBuilder.build();
    }

    private void validatePreferredAvailabilityZones(ValidationResult.ValidationResultBuilder validationBuilder, StackDto stack,
            NetworkScaleV4Request stackNetworkScaleV4Request, String groupName) {
        if (stackNetworkScaleV4Request != null && CollectionUtils.isNotEmpty(stackNetworkScaleV4Request.getPreferredAvailabilityZones())) {
            getScaledInstanceGroupView(stack, groupName)
                    .ifPresent(igv -> {
                        Set<String> instanceGroupZones = instanceGroupService.findAvailabilityZonesByStackIdAndGroupId(igv.getId());
                        if (!instanceGroupZones.containsAll(stackNetworkScaleV4Request.getPreferredAvailabilityZones())) {
                            String message = String.format("The list of preferred availability zones is invalid! Preferred availability zones must be " +
                                    "the subset of '%s'", String.join(", ", instanceGroupZones));
                            LOGGER.info(message);
                            validationBuilder.error(message);
                        }
                    });
        }
    }

    private void validatePreferredSubnetIds(ValidationResult.ValidationResultBuilder validationBuilder, StackDto stack,
            NetworkScaleV4Request stackNetworkScaleV4Request, String groupName) {
        if (stackNetworkScaleV4Request != null && CollectionUtils.isNotEmpty(stackNetworkScaleV4Request.getPreferredSubnetIds())) {
            Set<InstanceGroupView> instanceGroupViews = new HashSet<>(stack.getInstanceGroupViews());
            Set<String> subnetIds = collectSubnetIds(instanceGroupViews);
            if (subnetIds.size() < 2) {
                String message = "It does not make sense to prefer subnets on a cluster that has been provisioned in a single subnet";
                LOGGER.info(message);
                validationBuilder.error(message);
            }
            getScaledInstanceGroupView(stack, groupName)
                    .ifPresent(igv -> {
                        Set<String> subnetIdsFromInstanceGroupNetwork = getSubnetIdsFromInstanceGroupNetwork(igv);
                        if (!subnetIdsFromInstanceGroupNetwork.containsAll(stackNetworkScaleV4Request.getPreferredSubnetIds())) {
                            String message = String.format("The list of preferred subnets is invalid! Preferred subnets must be the subset of '%s'",
                                    String.join(", ", subnetIds));
                            LOGGER.info(message);
                            validationBuilder.error(message);
                        }
                    });
        }
    }

    private Optional<InstanceGroupView> getScaledInstanceGroupView(StackDto stack, String groupName) {
        return stack.getInstanceGroupViews().stream()
                .filter(igv -> groupName.equalsIgnoreCase(igv.getGroupName()))
                .findFirst();
    }

    private Set<String> collectSubnetIds(Iterable<InstanceGroupView> instanceGroups) {
        Set<String> allSubnetIds = new HashSet<>();
        for (InstanceGroupView instanceGroup : instanceGroups) {
            allSubnetIds.addAll(getSubnetIdsFromInstanceGroupNetwork(instanceGroup));
        }
        return allSubnetIds;
    }

    private Set<String> getSubnetIdsFromInstanceGroupNetwork(InstanceGroupView instanceGroup) {
        Set<String> result = new HashSet<>();
        InstanceGroupNetwork instanceGroupNetwork = instanceGroup.getInstanceGroupNetwork();
        if (instanceGroupNetwork != null) {
            Json attributes = instanceGroupNetwork.getAttributes();
            if (attributes != null) {
                result.addAll((List<String>) attributes
                        .getMap()
                        .getOrDefault(NetworkConstants.SUBNET_IDS, new ArrayList<>()));
            }
        }
        return result;
    }
}