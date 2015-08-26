package com.sequenceiq.cloudbreak.service.stack.connector.adapter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CreateCredentialRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CreateCredentialResult;
import com.sequenceiq.cloudbreak.cloud.event.credential.DeleteCredentialRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.DeleteCredentialResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.credential.CredentialHandler;
import com.sequenceiq.cloudbreak.service.credential.OpenSshPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ServiceProviderCredentialAdapter implements CredentialHandler<Credential> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderCredentialAdapter.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private OpenSshPublicKeyValidator rsaPublicKeyValidator;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.ADAPTER;
    }

    @Override
    public Credential init(Credential credential) {
        rsaPublicKeyValidator.validate(credential);
        CloudPlatform cloudPlatform = credential.cloudPlatform();
        CloudContext cloudContext = new CloudContext(credential.getId(), credential.getName(), cloudPlatform.name(), credential.getOwner());
        CloudCredential cloudCredential = credentialConverter.convert(credential);

        CreateCredentialRequest createCredentialRequest = new CreateCredentialRequest(cloudContext, cloudCredential);
        LOGGER.info("Triggering event: {}", createCredentialRequest);
        eventBus.notify(createCredentialRequest.selector(), Event.wrap(createCredentialRequest));
        try {
            CreateCredentialResult res = createCredentialRequest.await();
            LOGGER.info("Result: {}", res);
            if (res.getStatus() != EventStatus.OK) {
                throw new OperationException("Failed to setup provisioning", cloudContext, res.getErrorDetails());
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing provisioning setup", e);
            throw new OperationException("Unexpected exception occurred during provisioning setup", cloudContext, e);
        }

        return credential;
    }

    @Override
    public boolean delete(Credential credential) {
        CloudPlatform cloudPlatform = credential.cloudPlatform();
        CloudContext cloudContext = new CloudContext(credential.getId(), credential.getName(), cloudPlatform.name(), credential.getOwner());
        CloudCredential cloudCredential = credentialConverter.convert(credential);

        DeleteCredentialRequest deleteCredentialRequest = new DeleteCredentialRequest(cloudContext, cloudCredential);
        LOGGER.info("Triggering event: {}", deleteCredentialRequest);
        eventBus.notify(deleteCredentialRequest.selector(), Event.wrap(deleteCredentialRequest));
        try {
            DeleteCredentialResult res = deleteCredentialRequest.await();
            LOGGER.info("Result: {}", res);
            if (res.getStatus() != EventStatus.OK) {
                throw new OperationException("Failed to setup provisioning", cloudContext, res.getErrorDetails());
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing provisioning setup", e);
            throw new OperationException("Unexpected exception occurred during provisioning setup", cloudContext, e);
        }

        return true;
    }

    @Override
    public Credential update(Credential credential) throws Exception {
        return credential;
    }
}
