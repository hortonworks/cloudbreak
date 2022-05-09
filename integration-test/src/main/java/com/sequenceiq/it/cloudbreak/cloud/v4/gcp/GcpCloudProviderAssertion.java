package com.sequenceiq.it.cloudbreak.cloud.v4.gcp;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProviderAssertion;

@Component
public class GcpCloudProviderAssertion extends AbstractCloudProviderAssertion {

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }

}