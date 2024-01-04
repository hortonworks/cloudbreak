package com.sequenceiq.cloudbreak.controller.validation.network;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_VARIANT;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
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

    @Value("${cb.multiaz.supported.variants:AWS_NATIVE,AWS_NATIVE_GOV}")
    private Set<String> supportedMultiAzVariants;

    @Value("${cb.multiaz.supported.instancemetadata.platforms:AWS,YARN}")
    private Set<String> supportedInstanceMetadataPlatforms;

    @Inject
    private StackService stackService;

    @Inject
    private ProviderBasedMultiAzSetupValidator providerBasedMultiAzSetupValidator;

    @PostConstruct
    public void initSupportedVariants() {
        if (supportedMultiAzVariants.isEmpty()) {
            supportedMultiAzVariants = Set.of(AWS_NATIVE_VARIANT.variant().value(), AWS_NATIVE_GOV_VARIANT.variant().value());
        }
        if (supportedInstanceMetadataPlatforms.isEmpty()) {
            supportedInstanceMetadataPlatforms = Set.of(
                    CloudPlatform.AWS.name(),
                    CloudPlatform.YARN.name());
        }
    }

    public void validateMultiAzForStack(Stack stack, ValidationResult.ValidationResultBuilder validationBuilder) {
        Set<String> allSubnetIds = collectSubnetIds(new ArrayList<>(stack.getInstanceGroups()));
        String platformVariant = stack.getPlatformVariant();
        if (allSubnetIds.size() > 1 && !supportedVariant(platformVariant)) {
            String variantIsNotSupportedMsg = String.format("Multiple Availability Zone feature is not supported for %s, please use one of %s instead",
                    platformVariant, supportedMultiAzVariants);
            LOGGER.info(variantIsNotSupportedMsg);
            validationBuilder.error(variantIsNotSupportedMsg);
        }
        providerBasedMultiAzSetupValidator.validate(validationBuilder, stack);
    }

    public boolean supportedVariant(String variant) {
        return !Strings.isNullOrEmpty(variant) && supportedMultiAzVariants.contains(variant);
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
