package com.sequenceiq.cloudbreak.cloud.openstack.nativ;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackAuthenticator;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackCredentialConnector;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackParameters;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackSetup;

@Service
public class OpenStackNativeConnector implements CloudConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackNativeConnector.class);

    @Inject
    private OpenStackCredentialConnector credentialConnector;
    @Inject
    private OpenStackAuthenticator authenticator;
    @Inject
    private OpenStackNativeResourceConnector resourceConnector;
    @Inject
    private OpenStackNativeInstanceConnector instanceConnector;
    @Inject
    private OpenStackSetup openStackSetup;
    @Inject
    private OpenStackParameters openStackParameters;
    @Inject
    private OpenStackNativeMetaDataCollector metadataCollector;


    @Override
    public String platform() {
        return OpenStackConstants.OPENSTACK;
    }

    @Override
    public String variant() {
        return OpenStackConstants.Variant.NATIVE.name();
    }

    @Override
    public Authenticator authentication() {
        return authenticator;
    }

    @Override
    public ResourceConnector resources() {
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
    public Setup setup() {
        return openStackSetup;
    }

    @Override
    public CredentialConnector credentials() {
        return credentialConnector;
    }

    @Override
    public PlatformParameters parameters() {
        return openStackParameters;
    }

}
