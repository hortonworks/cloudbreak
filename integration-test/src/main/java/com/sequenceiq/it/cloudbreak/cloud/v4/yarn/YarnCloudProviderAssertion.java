package com.sequenceiq.it.cloudbreak.cloud.v4.yarn;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProviderAssertion;

@Component
public class YarnCloudProviderAssertion extends AbstractCloudProviderAssertion {

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.YARN;
    }

}