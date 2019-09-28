package com.sequenceiq.cloudbreak.cloud.gcp.util;

import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Component
public class GcpApiFactory {

    public Compute getComputeApi(CloudCredential cloudCredential) {
        return GcpStackUtil.buildCompute(cloudCredential);
    }
}
