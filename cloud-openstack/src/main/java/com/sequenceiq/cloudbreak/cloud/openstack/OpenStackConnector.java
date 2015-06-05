package com.sequenceiq.cloudbreak.cloud.openstack;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service("OpenStackConnectorV2")
public class OpenStackConnector implements CloudConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackConnector.class);

    private static final String OPENSTACK = "OPENSTACK";

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private OpenStackResourceConnector rc;

    @Inject
    private OpenStackInstanceConnector ic;


    @Override
    public String platform() {
        return OPENSTACK;
    }

    @Override
    public AuthenticatedContext authenticate(StackContext stackContext, CloudCredential cloudCredential) {
        LOGGER.info("Authenticating to openstack ...");
        return openStackClient.createAuthenticatedContext(stackContext, cloudCredential);
    }

    @Override
    public ResourceConnector resources() {
        return rc;
    }

    @Override
    public InstanceConnector instances() {
        return ic;
    }
}
