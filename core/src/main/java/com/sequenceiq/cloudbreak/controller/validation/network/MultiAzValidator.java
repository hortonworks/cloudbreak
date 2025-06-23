package com.sequenceiq.cloudbreak.controller.validation.network;

import static com.sequenceiq.cloudbreak.service.multiaz.InstanceMetadataAvailabilityZoneCalculator.ZONAL_SUBNET_CLOUD_PLATFORMS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.AvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.service.multiaz.ProviderBasedMultiAzSetupValidator;
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

    public boolean supportedForInstanceMetadataGeneration(InstanceGroupNetwork instanceGroupNetwork) {
        if (instanceGroupNetwork != null) {
            return supportedInstanceMetadataPlatforms.contains(instanceGroupNetwork.cloudPlatform());
        }
        return false;
    }

    public Set<String> collectSubnetIds(Iterable<InstanceGroupView> instanceGroups) {
        Set<String> allSubnetIds = new HashSet<>();
        for (InstanceGroupView instanceGroup : instanceGroups) {
            InstanceGroupNetwork instanceGroupNetwork = instanceGroup.getInstanceGroupNetwork();
            if (instanceGroupNetwork != null) {
                Json attributes = instanceGroupNetwork.getAttributes();
                if (attributes != null) {
                    List<String> subnetIds = (List<String>) attributes
                            .getMap()
                            .getOrDefault(NetworkConstants.SUBNET_IDS, new ArrayList<>());
                    allSubnetIds.addAll(subnetIds);
                }
            }
        }
        return allSubnetIds;
    }
}