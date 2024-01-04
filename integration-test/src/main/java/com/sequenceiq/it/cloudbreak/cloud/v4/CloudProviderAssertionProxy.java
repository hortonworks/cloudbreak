package com.sequenceiq.it.cloudbreak.cloud.v4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Component
public class CloudProviderAssertionProxy implements CloudProviderAssertion {
    private CloudProviderAssertion delegate;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private List<CloudProviderAssertion> cloudProviders;

    private final Map<CloudPlatform, CloudProviderAssertion> cloudProviderMap = new HashMap<>();

    @PostConstruct
    private void init() {
        cloudProviders.forEach(cloudProvider -> {
            cloudProviderMap.put(cloudProvider.getCloudPlatform(), cloudProvider);
        });
        delegate = cloudProviderMap.get(CloudPlatform.valueOf(commonCloudProperties.getCloudProvider()));
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return delegate.getCloudPlatform();
    }

    @Override
    public void assertServiceEndpoint(EnvironmentTestDto environmentTestDto) {
        delegate.assertServiceEndpoint(environmentTestDto);
    }

}
