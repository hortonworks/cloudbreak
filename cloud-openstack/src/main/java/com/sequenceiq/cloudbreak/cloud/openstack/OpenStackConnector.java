package com.sequenceiq.cloudbreak.cloud.openstack;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.ValidatorType;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackParameters;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackPlatformResources;

@Service
public class OpenStackConnector implements CloudConnector {

    @Inject
    private OpenStackConstants openstackConstants;

    @Inject
    private OpenStackAuthenticator openstackAuthenticator;

    @Inject
    private OpenStackCredentialConnector credentialConnector;

    @Inject
    private OpenStackSetup openstackSetup;

    @Inject
    private OpenStackParameters openStackParameters;

    @Inject
    private OpenStackPlatformResources openstackPlatformResources;

    @Inject
    private OpenStackResourceConnector openstackResourceConnector;

    @Inject
    private OpenStackInstanceConnector openstackInstanceConnector;

    @Inject
    private OpenStackMetaDataCollector openStackMetaDataCollector;

    @Inject
    private OpenStackNetworkConnector openstackNetworkConnector;

    @Override
    public Authenticator authentication() {
        return openstackAuthenticator;
    }

    @Override
    public Setup setup() {
        return openstackSetup;
    }

    @Override
    public List<Validator> validators(ValidatorType validatorType) {
        return List.of();
    }

    @Override
    public CredentialConnector credentials() {
        return credentialConnector;
    }

    @Override
    public ResourceConnector resources() {
        return openstackResourceConnector;
    }

    @Override
    public InstanceConnector instances() {
        return openstackInstanceConnector;
    }

    @Override
    public MetadataCollector metadata() {
        return openStackMetaDataCollector;
    }

    @Override
    public PlatformParameters parameters() {
        return openStackParameters;
    }

    @Override
    public PlatformResources platformResources() {
        return openstackPlatformResources;
    }

    @Override
    public CloudConstant cloudConstant() {
        return openstackConstants;
    }

    @Override
    public NetworkConnector networkConnector() {
        return openstackNetworkConnector;
    }

    @Override
    public EncryptionResources encryptionResources() {
        return null;
    }

    @Override
    public ResourceVolumeConnector volumeConnector() {
        throw new UnsupportedOperationException("Volume connector is not supported for OpenStack");
    }

    @Override
    public Platform platform() {
        return openstackConstants.platform();
    }

    @Override
    public Variant variant() {
        return openstackConstants.variant();
    }
}
