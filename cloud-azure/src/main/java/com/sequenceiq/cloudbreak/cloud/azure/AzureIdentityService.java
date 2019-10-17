package com.sequenceiq.cloudbreak.cloud.azure;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.IdentityService;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class AzureIdentityService implements IdentityService {

    @Override
    public String getAccountId(String region, CloudCredential cloudCredential) {
        return "";
    }

    @Override
    public Platform platform() {
        return AzureConstants.PLATFORM;
    }

    @Override
    public Variant variant() {
        return AzureConstants.VARIANT;
    }

}
