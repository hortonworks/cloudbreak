package com.sequenceiq.cloudbreak.cloud.byos;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class BYOSConnector implements CloudConnector {

    @Inject
    private BYOSPlatformParameters platformParameters;

    @Override
    public Authenticator authentication() {
        throw new UnsupportedOperationException("Authentication operation is not supported on BYOS stacks.");
    }

    @Override
    public Setup setup() {
        throw new UnsupportedOperationException("Setup operation is not supported on BYOS stacks.");
    }

    @Override
    public CredentialConnector credentials() {
        throw new UnsupportedOperationException("Credentials operation is not supported on BYOS stacks.");
    }

    @Override
    public ResourceConnector resources() {
        throw new UnsupportedOperationException("Resources operation is not supported on BYOS stacks.");
    }

    @Override
    public InstanceConnector instances() {
        throw new UnsupportedOperationException("Instances operation is not supported on BYOS stacks.");
    }

    @Override
    public MetadataCollector metadata() {
        throw new UnsupportedOperationException("Metadata operation is not supported on BYOS stacks.");
    }

    @Override
    public PlatformParameters parameters() {
        return platformParameters;
    }

    @Override
    public Platform platform() {
        return BYOSConstants.BYOS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return BYOSConstants.BYOS_VARIANT;
    }
}
