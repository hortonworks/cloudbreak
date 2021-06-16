package com.sequenceiq.cloudbreak.controller.validation.network;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_VARIANT;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Component
public class MultiAzValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzValidator.class);

    @Value("${cb.multiaz.supported.variant:AWS_NATIVE}")
    private Set<String> supportedMultiAzVariants;

    @PostConstruct
    public void initSupportedVariants() {
        if (supportedMultiAzVariants.isEmpty()) {
            supportedMultiAzVariants = Set.of(AWS_NATIVE_VARIANT.variant().value());
        }
    }

    public void validateMultiAzForStack(
        String variant,
        Iterable<InstanceGroup> instanceGroups,
        ValidationResult.ValidationResultBuilder validationBuilder) {
        Set<String> allSubnetIds = collectSubnetIds(instanceGroups);
        if (allSubnetIds.size() > 1 && !Strings.isNullOrEmpty(variant)) {
            if (!supportedMultiAzVariants.contains(variant)) {
                validationBuilder.error(String.format("Multiple Availability Zone feature is not supported for %s variant", variant));
            }
        }
    }

    private Set<String> collectSubnetIds(Iterable<InstanceGroup> instanceGroups) {
        Set<String> allSubnetIds = new HashSet<>();
        for (InstanceGroup instanceGroup : instanceGroups) {
            InstanceGroupNetwork instanceGroupNetwork = instanceGroup.getInstanceGroupNetwork();
            Json attributes = instanceGroupNetwork.getAttributes();
            if (attributes != null) {
                List<String> subnetIds = (List<String>) attributes
                        .getMap()
                        .getOrDefault(NetworkConstants.SUBNET_IDS, new ArrayList<>());
                allSubnetIds.addAll(subnetIds);
            }
        }
        return allSubnetIds;
    }
}
