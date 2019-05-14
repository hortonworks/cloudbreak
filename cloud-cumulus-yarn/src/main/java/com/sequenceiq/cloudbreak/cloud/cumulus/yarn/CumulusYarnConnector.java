package com.sequenceiq.cloudbreak.cloud.cumulus.yarn;

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
import com.sequenceiq.cloudbreak.cloud.cumulus.yarn.auth.CumulusYarnAuthenticator;
import com.sequenceiq.cloudbreak.cloud.cumulus.yarn.auth.CumulusYarnCredentialConnector;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class CumulusYarnConnector implements CloudConnector<Object> {
    @Inject
    private CumulusYarnAuthenticator authenticator;

    @Inject
    private CumulusYarnProvisionSetup provisionSetup;

    @Inject
    private CumulusYarnCredentialConnector credentialConnector;

    @Inject
    private CumulusYarnResourceConnector resourceConnector;

    @Inject
    private CumulusYarnInstanceConnector instanceConnector;

    @Inject
    private CumulusYarnMetadataCollector metadataCollector;

    @Inject
    private CumulusYarnPlatformParameters platformParameters;

    @Inject
    private CumulusYarnPlatformResources platformResources;

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
        return CumulusYarnConstants.CUMULUS_YARN_PLATFORM;
    }

    @Override
    public Variant variant() {
        return CumulusYarnConstants.CUMULUS_YARN_VARIANT;
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
