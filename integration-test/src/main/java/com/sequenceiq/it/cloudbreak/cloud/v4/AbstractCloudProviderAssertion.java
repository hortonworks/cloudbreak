package com.sequenceiq.it.cloudbreak.cloud.v4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

public abstract class AbstractCloudProviderAssertion implements CloudProviderAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCloudProviderAssertion.class);

    @Override
    public void assertServiceEndpoint(EnvironmentTestDto environmentTestDto) {
        LOGGER.debug("Service endpoint not applicable for cloud provider " + getCloudPlatform().name());
    }

}