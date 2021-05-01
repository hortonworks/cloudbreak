package com.sequenceiq.cloudbreak.cloud.gcp;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.IdentityService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class GcpIdentityService implements IdentityService {

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Override
    public String getAccountId(String region, CloudCredential cloudCredential) {
        return gcpStackUtil.getProjectId(cloudCredential);
    }

    @Override
    public Platform platform() {
        return GcpConstants.GCP_PLATFORM;
    }

    @Override
    public Variant variant() {
        return GcpConstants.GCP_VARIANT;
    }
}
