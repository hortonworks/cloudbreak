package com.sequenceiq.cloudbreak.service.stack.connector.adapter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.setup.PreProvisionCheckRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.PreProvisionCheckResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionEvent;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ServiceProviderSetupAdapter implements ProvisionSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderSetupAdapter.class);

    @Inject
    private EventBus eventBus;
    @Inject
    private StackToCloudStackConverter cloudStackConverter;
    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.ADAPTER;
    }

    @Override
    public String preProvisionCheck(Stack stack) {
        CloudContext cloudContext = new CloudContext(stack);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        PreProvisionCheckRequest<PreProvisionCheckResult> preProvisionCheckRequest = new PreProvisionCheckRequest<>(cloudContext, cloudCredential, cloudStack);
        LOGGER.info("Triggering event: {}", preProvisionCheckRequest);
        eventBus.notify(preProvisionCheckRequest.selector(), Event.wrap(preProvisionCheckRequest));
        try {
            PreProvisionCheckResult res = preProvisionCheckRequest.await();
            LOGGER.info("Result: {}", res);
            if (res.getErrorDetails() != null) {
                return res.getErrorDetails().getMessage();
            }
            return res.getStatusReason();
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing pre-provision check", e);
            return e.getMessage();
        }
    }

    @Override
    public ProvisionEvent setupProvisioning(Stack stack) throws Exception {
        CloudPlatform cloudPlatform = stack.cloudPlatform();
        CloudContext cloudContext = new CloudContext(stack);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        SetupRequest<SetupResult> setupRequest = new SetupRequest<>(cloudContext, cloudCredential, cloudStack);
        LOGGER.info("Triggering event: {}", setupRequest);
        eventBus.notify(setupRequest.selector(), Event.wrap(setupRequest));
        try {
            SetupResult res = setupRequest.await();
            LOGGER.info("Result: {}", res);
            if (res.getErrorDetails() != null) {
                throw new OperationException("Failed to setup provisioning", cloudContext, res.getErrorDetails());
            }
            return new ProvisionSetupComplete(cloudPlatform, stack.getId()).withSetupProperties(res.getSetupProperties());
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing provisioning setup", e);
            throw new OperationException("Unexpected exception occurred during provisioning setup", cloudContext, e);
        }
    }


}
