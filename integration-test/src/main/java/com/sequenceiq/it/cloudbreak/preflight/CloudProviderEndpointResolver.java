package com.sequenceiq.it.cloudbreak.preflight;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsProperties;
import com.sequenceiq.it.cloudbreak.config.CloudProviderEndpointsProperties;

@Component
public class CloudProviderEndpointResolver {

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private CloudProviderEndpointsProperties cloudProviderEndpointsProperties;

    @Inject
    private AwsProperties awsProperties;

    public List<String> resolveActiveProviderHostnames() {
        return switch (activeCloudPlatform()) {
            case AWS -> cloudProviderEndpointsProperties.getAws().stream()
                    .map(hostname -> hostname.replace("%s", awsProperties.getRegion()))
                    .toList();
            case AZURE -> cloudProviderEndpointsProperties.getAzure();
            case GCP -> cloudProviderEndpointsProperties.getGcp();
            default -> List.of();
        };
    }

    public CloudPlatform activeCloudPlatform() {
        return CloudPlatform.valueOf(commonCloudProperties.getCloudProvider());
    }
}
