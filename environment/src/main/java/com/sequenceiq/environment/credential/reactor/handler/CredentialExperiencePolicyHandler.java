package com.sequenceiq.environment.credential.reactor.handler;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialExperiencePolicyRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialExperiencePolicyResult;
import com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;

@Component
public class CredentialExperiencePolicyHandler implements CloudPlatformEventHandler<CredentialExperiencePolicyRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialExperiencePolicyHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<CredentialExperiencePolicyRequest> type() {
        return CredentialExperiencePolicyRequest.class;
    }

    @Override
    public void accept(Event<CredentialExperiencePolicyRequest> credentialExperiencePolicyRequestEvent) {
        LOGGER.debug("Received event: {}", credentialExperiencePolicyRequestEvent);
        CredentialExperiencePolicyRequest request = credentialExperiencePolicyRequestEvent.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            LOGGER.info("Gathering credential experience policies for platform: '{}'", cloudContext.getPlatform());
            CloudConnector<?> connector = cloudPlatformConnectors.getDefault(cloudContext.getPlatform());

            CredentialExperiencePolicyResult credentialExperiencePolicyResult = new CredentialExperiencePolicyResult(request.getResourceId(),
                    Map.of("PUKIX", "s3irasjog:beajitva"));
            request.getResult().onNext(credentialExperiencePolicyResult);
            LOGGER.debug("Credential prerequisites have been collected successfully for platform: '{}'!", cloudContext.getPlatform().value());
        } catch (RuntimeException e) {
            request.getResult().onNext(new CredentialExperiencePolicyResult(e.getMessage(), e, request.getResourceId()));
        }
    }
}
