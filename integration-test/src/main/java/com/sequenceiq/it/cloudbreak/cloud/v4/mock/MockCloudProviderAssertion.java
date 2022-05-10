package com.sequenceiq.it.cloudbreak.cloud.v4.mock;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProviderAssertion;

@Component
public class MockCloudProviderAssertion extends AbstractCloudProviderAssertion {
    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.MOCK;
    }

}