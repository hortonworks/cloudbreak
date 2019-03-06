package com.sequenceiq.cloudbreak.cloud.openstack.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;

@Service
public class OpenStackCredentialConnector implements CredentialConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackCredentialConnector.class);

    @Override
    public CloudCredentialStatus verify(AuthenticatedContext authenticatedContext) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        return new CloudCredentialStatus(credential, CredentialStatus.VERIFIED);
    }

    @Override
    public CloudCredentialStatus create(AuthenticatedContext auth) {
        LOGGER.debug("Create credential: {}", auth.getCloudCredential());
        return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.CREATED);
    }

    @Override
    public CloudCredentialStatus delete(AuthenticatedContext auth) {
        LOGGER.debug("Delete credential: {}", auth.getCloudCredential());
        return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.DELETED);
    }

}
