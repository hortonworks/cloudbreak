package com.sequenceiq.cloudbreak.cloud.openstack;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service("OpenStackConnectorV2")
public class OpenStackConnector implements CloudConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackConnector.class);

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private OpenStackResourceConnector resourceConnector;

    @Inject
    private OpenStackInstanceConnector instanceConnector;

    @Inject
    private OpenStackSetup openStackSetup;

    @Override
    public String platform() {
        return OpenStackConstants.OPENSTACK;
    }

    @Override
    public AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential) {
        LOGGER.info("Authenticating to openstack ...");
        return openStackClient.createAuthenticatedContext(cloudContext, cloudCredential);
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
    public Setup setup() {
        return openStackSetup;
    }
}
