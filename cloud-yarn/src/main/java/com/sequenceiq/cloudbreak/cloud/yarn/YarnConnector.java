package com.sequenceiq.cloudbreak.cloud.yarn;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.yarn.auth.YarnAuthenticator;
import com.sequenceiq.cloudbreak.cloud.yarn.auth.YarnCredentialConnector;

@Service
public class YarnConnector implements CloudConnector<Object> {
    @Inject
    private YarnAuthenticator authenticator;

    @Inject
    private YarnProvisionSetup provisionSetup;

    @Inject
    private YarnCredentialConnector credentialConnector;

    @Inject
    private YarnResourceConnector resourceConnector;

    @Inject
    private YarnInstanceConnector instanceConnector;

    @Inject
    private YarnMetadataCollector metadataCollector;

    @Inject
    private YarnPlatformParameters platformParameters;

    @Inject
    private YarnPlatformResources platformResources;

    @Override
    public Authenticator authentication() {
        return authenticator;
    }

    @Override
    public Setup setup() {
        return provisionSetup;
    }

    @Override
    public List<Validator> validators() {
        return Collections.emptyList();
    }

    @Override
    public CredentialConnector credentials() {
        return credentialConnector;
    }

    @Override
    public ResourceConnector<Object> resources() {
        return resourceConnector;
    }

    @Override
    public InstanceConnector instances() {
        return instanceConnector;
    }

    @Override
    public MetadataCollector metadata() {
        return metadataCollector;
    }

    @Override
    public PlatformParameters parameters() {
        return platformParameters;
    }

    @Override
    public PlatformResources platformResources() {
        return platformResources;
    }

    @Override
    public Platform platform() {
        return YarnConstants.YARN_PLATFORM;
    }

    @Override
    public Variant variant() {
        return YarnConstants.YARN_VARIANT;
    }

    @Override
    public CloudConstant cloudConstant() {
        return null;
    }

    @Override
    public NetworkConnector networkConnector() {
        return null;
    }
}
