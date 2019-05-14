package com.sequenceiq.cloudbreak.cloud.openstack.nativ;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

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
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackAuthenticator;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackCredentialConnector;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.OpenStackVariant;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackParameters;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackPlatformResources;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackSetup;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackTagValidator;

public class OpenStackNativeConnector implements CloudConnector<List<CloudResource>> {

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
    private OpenStackTagValidator openStackTagValidator;

    @Inject
    private OpenStackParameters openStackParameters;

    @Inject
    private OpenStackPlatformResources openStackPlatformResources;

    @Inject
    private OpenStackNativeMetaDataCollector metadataCollector;

    @Inject
    private OpenStackConstants openStackConstants;

    @Override
    public Platform platform() {
        return OpenStackConstants.OPENSTACK_PLATFORM;
    }

    @Override
    public Variant variant() {
        return OpenStackVariant.NATIVE.variant();
    }

    @Override
    public Authenticator authentication() {
        return authenticator;
    }

    @Override
    public ResourceConnector<List<CloudResource>> resources() {
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
    public List<Validator> validators() {
        return Collections.singletonList(openStackTagValidator);
    }

    @Override
    public CredentialConnector credentials() {
        return credentialConnector;
    }

    @Override
    public PlatformParameters parameters() {
        return openStackParameters;
    }

    @Override
    public PlatformResources platformResources() {
        return openStackPlatformResources;
    }

    @Override
    public CloudConstant cloudConstant() {
        return openStackConstants;
    }

    @Override
    public NetworkConnector networkConnector() {
        return null;
    }

}
