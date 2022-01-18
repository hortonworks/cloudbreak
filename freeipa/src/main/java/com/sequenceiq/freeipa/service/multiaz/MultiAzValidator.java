package com.sequenceiq.freeipa.service.multiaz;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_GOV_VARIANT;
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
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;

@Component
public class MultiAzValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzValidator.class);

    @Value("${cb.multiaz.supported.variants:AWS_NATIVE,AWS_GOV}")
    private Set<String> supportedMultiAzVariants;

    @Value("${cb.multiaz.supported.instancemetadata.platforms:AWS,GCP,AZURE,YARN}")
    private Set<String> supportedInstanceMetadataPlatforms;

    @PostConstruct
    public void initSupportedVariants() {
        if (supportedMultiAzVariants.isEmpty()) {
            supportedMultiAzVariants = Set.of(AWS_NATIVE_VARIANT.variant().value(), AWS_GOV_VARIANT.variant().value());
        }
        if (supportedInstanceMetadataPlatforms.isEmpty()) {
            supportedInstanceMetadataPlatforms = Set.of(
                    CloudPlatform.AWS.name(),
                    CloudPlatform.GCP.name(),
                    CloudPlatform.AZURE.name(),
                    CloudPlatform.YARN.name());
        }
    }

    public void validateMultiAzForStack(String variant, Iterable<InstanceGroup> instanceGroups) {
        Set<String> allSubnetIds = collectSubnetIds(instanceGroups);
        if (allSubnetIds.size() > 1 && !Strings.isNullOrEmpty(variant)) {
            if (!supportedMultiAzVariants.contains(variant)) {
                throw new BadRequestException(
                        String.format("Multiple Availability Zone feature is not supported for %s variant", variant));
            }
        }
    }

    public boolean supportedForInstanceMetadataGeneration(InstanceGroup instanceGroup) {
        if (instanceGroup.getInstanceGroupNetwork() != null) {
            boolean platformSupportsMultiAz = supportedInstanceMetadataPlatforms.contains(instanceGroup.getInstanceGroupNetwork().cloudPlatform());
            LOGGER.debug("Multi AZ support on platform result: [{}]", platformSupportsMultiAz);
            return platformSupportsMultiAz;
        } else {
            LOGGER.debug("instanceGroup.getInstanceGroupNetwork() is null");
            return false;
        }

    }

    private Set<String> collectSubnetIds(Iterable<InstanceGroup> instanceGroups) {
        Set<String> allSubnetIds = new HashSet<>();
        for (InstanceGroup instanceGroup : instanceGroups) {
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
