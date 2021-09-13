package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialNotifier;
import com.sequenceiq.cloudbreak.cloud.event.credential.LightHouseRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.LightHouseResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;

@Component
public class LightHouseHandler implements CloudPlatformEventHandler<LightHouseRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LightHouseHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CredentialNotifier credentialNotifier;


    @Override
    public Class<LightHouseRequest> type() {
        return LightHouseRequest.class;
    }

    @Override
    public void accept(Event<LightHouseRequest> lightHouseRequestEvent) {
        LOGGER.debug("Received event: {}", lightHouseRequestEvent);
        LightHouseRequest request = lightHouseRequestEvent.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<Object> connector = cloudPlatformConnectors.getDefault(cloudContext.getPlatform());
            Map<String, String> parameters = connector.credentials().lightHouse(cloudContext, request.getExtendedCloudCredential(), credentialNotifier);
            LightHouseResult lightHouseResult = new LightHouseResult(request.getResourceId(), parameters);
            request.getResult().onNext(lightHouseResult);
            LOGGER.debug("Light House login request successfully processed");
        } catch (RuntimeException e) {
            request.getResult().onNext(new LightHouseResult(e.getMessage(), e, request.getResourceId()));
        }
    }

}
